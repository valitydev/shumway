package dev.vality.shumway.handler;

import dev.vality.damsel.accounter.Account;
import dev.vality.damsel.accounter.AccountNotFound;
import dev.vality.damsel.accounter.AccountPrototype;
import dev.vality.damsel.accounter.AccounterSrv;
import dev.vality.damsel.accounter.PlanNotFound;
import dev.vality.damsel.accounter.PostingBatch;
import dev.vality.damsel.accounter.PostingPlan;
import dev.vality.damsel.accounter.PostingPlanChange;
import dev.vality.damsel.accounter.PostingPlanLog;
import dev.vality.shumway.dao.DaoException;
import dev.vality.shumway.domain.AccountState;
import dev.vality.shumway.domain.PostingLog;
import dev.vality.shumway.domain.PostingOperation;
import dev.vality.shumway.domain.StatefulAccount;
import dev.vality.shumway.service.AccountService;
import dev.vality.shumway.service.PostingPlanService;
import dev.vality.woody.api.flow.error.WUnavailableResultException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.thrift.TException;
import org.springframework.transaction.TransactionException;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Created by vpankrashkin on 16.09.16.
 */
@Slf4j
@RequiredArgsConstructor
public class AccounterHandler implements AccounterSrv.Iface {

    private final TransactionTemplate transactionTemplate;
    private final AccountService accountService;
    private final PostingPlanService planService;

    public static boolean isFinalOperation(PostingOperation operation) {
        return operation != PostingOperation.HOLD;
    }

    @Override
    public PostingPlanLog hold(PostingPlanChange planChange) throws TException {
        return doSafeOperation(new PostingPlan(planChange.getId(), Arrays.asList(planChange.getBatch())),
                PostingOperation.HOLD);
    }

    @Override
    public PostingPlanLog commitPlan(PostingPlan postingPlan) throws TException {
        return doSafeOperation(postingPlan, PostingOperation.COMMIT);
    }

    @Override
    public PostingPlanLog rollbackPlan(PostingPlan postingPlan) throws TException {
        return doSafeOperation(postingPlan, PostingOperation.ROLLBACK);
    }

    protected PostingPlanLog doSafeOperation(PostingPlan postingPlan, PostingOperation operation) throws TException {
        Map<Long, StatefulAccount> affectedDomainStatefulAccounts;
        try {
            affectedDomainStatefulAccounts =
                    transactionTemplate.execute(transactionStatus -> safePostingOperation(postingPlan, operation));
            Map<Long, Account> affectedProtocolAccounts = affectedDomainStatefulAccounts.values()
                    .stream()
                    .collect(Collectors.toMap(
                            domainStAccount -> domainStAccount.getId(),
                            domainStAccount -> ProtocolConverter.convertFromDomainAccount(domainStAccount)
                    ));
            PostingPlanLog protocolPostingPlanLog = new PostingPlanLog(affectedProtocolAccounts);
            log.info("PostingPlanLog of affected accounts: {}", protocolPostingPlanLog);
            return protocolPostingPlanLog;
        } catch (Exception e) {
            log.error("PostingOperation processing error: ", e);
            if (e instanceof TransactionException) {
                throw new WUnavailableResultException(e);
            } else if (e.getCause() instanceof TException) {
                throw (TException) e.getCause();
            } else {
                throw e;
            }
        }
    }

    private Map<Long, StatefulAccount> safePostingOperation(
            PostingPlan postingPlan,
            PostingOperation operation
    ) {
        boolean finalOp = isFinalOperation(operation);
        try {
            log.info("New {} request, plan: {}", operation, postingPlan);
            AccounterValidator.validateStaticPlanBatches(postingPlan, finalOp);
            AccounterValidator.validateStaticPostings(postingPlan);
            dev.vality.shumway.domain.PostingPlanLog receivedDomainPlanLog =
                    ProtocolConverter.convertToDomainPlan(postingPlan, operation);

            Map.Entry<dev.vality.shumway.domain.PostingPlanLog, dev.vality.shumway.domain.PostingPlanLog>
                    postingPlanLogPair = finalOp
                    ? planService.updatePostingPlan(receivedDomainPlanLog, operation)
                    : planService.createOrUpdatePostingPlan(receivedDomainPlanLog);
            dev.vality.shumway.domain.PostingPlanLog oldDomainPlanLog = postingPlanLogPair.getKey();
            dev.vality.shumway.domain.PostingPlanLog currDomainPlanLog = postingPlanLogPair.getValue();
            log.info("Old plan log is {}, curr plan log is {}", oldDomainPlanLog, currDomainPlanLog);

            PostingOperation prevOperation =
                    oldDomainPlanLog == null ? PostingOperation.HOLD : oldDomainPlanLog.getLastOperation();
            if (currDomainPlanLog == null) {
                throw AccounterValidator.validatePlanNotFixedResult(receivedDomainPlanLog, oldDomainPlanLog, !finalOp);
            } else {
                Map<Long, List<PostingLog>> savedDomainPostingLogs =
                        planService.getPostingLogs(currDomainPlanLog.getPlanId(), prevOperation);

                AccounterValidator.validatePlanBatches(postingPlan, savedDomainPostingLogs, finalOp);

                //generally - valid result is single received batch for new hold and empty for any commit or rollback
                List<PostingBatch> newProtocolBatches = postingPlan.getBatchList()
                        .stream()
                        .filter(batch -> !savedDomainPostingLogs.containsKey(batch.getId()))
                        .collect(Collectors.toList());
                Map<Long, AccountState> resultAccStates;
                Map<Long, StatefulAccount> savedDomainStatefulAcc;
                if (prevOperation == operation && newProtocolBatches.isEmpty()) {
                    log.info("This is duplicate request: {}", operation);
                    savedDomainStatefulAcc = accountService.getStatefulAccounts(
                            postingPlan.getBatchList(),
                            postingPlan.getId(),
                            isFinalOperation(operation)
                    );
                    resultAccStates = savedDomainStatefulAcc.values().stream()
                            .collect(Collectors.toMap(acc -> acc.getId(), acc -> acc.getAccountState()));
                } else {
                    savedDomainStatefulAcc = accountService.getStatefulExclusiveAccounts(postingPlan.getBatchList());
                    AccounterValidator.validateAccounts(newProtocolBatches, savedDomainStatefulAcc);
                    log.debug("Saving posting batches: {}", newProtocolBatches);
                    List<PostingLog> newDomainPostingLogs = postingPlan.getBatchList()
                            .stream()
                            .flatMap(batch -> batch.getPostings().stream().map(posting -> ProtocolConverter
                                    .convertToDomainPosting(posting, batch, currDomainPlanLog)))
                            .collect(Collectors.toList());
                    log.info("New posting logs are {}", newDomainPostingLogs);
                    planService.addPostingLogs(newDomainPostingLogs);
                    if (PostingOperation.HOLD.equals(operation)) {
                        List<PostingLog> savedDomainPostingLogList = savedDomainPostingLogs.values().stream()
                                .flatMap(Collection::stream)
                                .collect(Collectors.toList());
                        resultAccStates = accountService.holdAccounts(
                                postingPlan.getId(),
                                postingPlan.getBatchList().get(0),
                                newDomainPostingLogs,
                                savedDomainPostingLogList,
                                savedDomainStatefulAcc
                        );
                    } else {
                        resultAccStates = accountService.commitOrRollback(
                                operation,
                                postingPlan.getId(),
                                newDomainPostingLogs,
                                savedDomainStatefulAcc
                        );
                    }
                }
                log.info("Result account state is {}", resultAccStates);
                return accountService.getStatefulAccounts(savedDomainStatefulAcc, () -> resultAccStates);
            }
        } catch (TException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public PostingPlan getPlan(String planId) throws TException {
        log.info("New GetPlan request, id: {}", planId);

        dev.vality.shumway.domain.PostingPlanLog domainPostingPlan;
        try {
            domainPostingPlan = planService.getSharedPostingPlan(planId);
        } catch (Exception e) {
            log.error("Failed to get posting plan log", e);
            if (e instanceof DaoException) {
                throw new WUnavailableResultException(e);
            }
            throw new TException(e);
        }
        if (domainPostingPlan == null) {
            log.warn("Not found plan with id: {}", planId);
            throw new PlanNotFound(planId);
        }
        Map<Long, List<PostingLog>> domainBatchLogs;
        try {
            domainBatchLogs = planService.getPostingLogs(planId, PostingOperation.HOLD);
        } catch (Exception e) {
            log.error("Failed to get posting logs", e);
            if (e instanceof DaoException) {
                throw new WUnavailableResultException(e);
            }
            throw new TException(e);
        }
        List<PostingBatch> protocolBatchList = domainBatchLogs.entrySet().stream()
                .map(entry -> ProtocolConverter.convertFromDomainToBatch(entry.getKey(), entry.getValue()))
                .collect(Collectors.toList());
        PostingPlan protocolPlan = new PostingPlan(planId, protocolBatchList);
        log.info("Response: {}", protocolPlan);
        return protocolPlan;
    }

    @Override
    public long createAccount(AccountPrototype accountPrototype) throws TException {
        log.info("New CreateAccount request, proto: {}", accountPrototype);
        dev.vality.shumway.domain.Account domainPrototype = ProtocolConverter.convertToDomainAccount(accountPrototype);
        long response;
        try {
            response = accountService.createAccount(domainPrototype);
        } catch (Exception e) {
            log.error("Failed to create account", e);
            if (e instanceof DaoException) {
                throw new WUnavailableResultException(e);
            }
            throw new TException(e);
        }
        log.info("Response: {}", response);
        return response;
    }

    @Override
    public Account getAccountByID(long id) throws TException {
        log.info("New GetAccountById request, id: {}", id);
        StatefulAccount domainAccount;
        try {
            domainAccount = accountService.getStatefulAccount(id);
        } catch (Exception e) {
            log.error("Failed to get account", e);
            if (e instanceof DaoException) {
                throw new WUnavailableResultException(e);
            }
            throw new TException(e);
        }
        if (domainAccount == null) {
            log.warn("Not found account with id: {}", id);
            throw new AccountNotFound(id);
        }
        Account response = ProtocolConverter.convertFromDomainAccount(domainAccount);
        log.info("Response: {}", response);
        return response;
    }

}
