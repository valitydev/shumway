package dev.vality.shumway.handler;

import dev.vality.damsel.accounter.AccountPrototype;
import dev.vality.damsel.accounter.Posting;
import dev.vality.damsel.accounter.PostingBatch;
import dev.vality.damsel.accounter.PostingPlan;
import dev.vality.geck.common.util.TypeUtil;
import dev.vality.shumway.domain.Account;
import dev.vality.shumway.domain.AccountState;
import dev.vality.shumway.domain.PostingLog;
import dev.vality.shumway.domain.PostingOperation;
import dev.vality.shumway.domain.PostingPlanLog;
import dev.vality.shumway.domain.StatefulAccount;

import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Created by vpankrashkin on 19.09.16.
 */
public class ProtocolConverter {

    public static Account convertToDomainAccount(AccountPrototype protocolPrototype) {
        Instant creationTime = protocolPrototype.isSetCreationTime()
                ? TypeUtil.stringToInstant(protocolPrototype.getCreationTime()) : Instant.now();
        return new Account(
                0,
                creationTime,
                protocolPrototype.getCurrencySymCode(),
                protocolPrototype.getDescription()
        );
    }

    public static dev.vality.damsel.accounter.Account convertFromDomainAccount(StatefulAccount domainAccount) {
        AccountState accountState = domainAccount.getAccountState();
        dev.vality.damsel.accounter.Account protocolAccount = new dev.vality.damsel.accounter.Account(
                domainAccount.getId(),
                accountState.getOwnAmount(),
                accountState.getMaxAvailableAmount(),
                accountState.getMinAvailableAmount(),
                domainAccount.getCurrSymCode()
        );
        protocolAccount.setCreationTime(TypeUtil.temporalToString(domainAccount.getCreationTime()));
        protocolAccount.setDescription(domainAccount.getDescription());
        return protocolAccount;
    }

    public static Posting convertFromDomainToPosting(PostingLog domainPostingLog) {
        return new Posting(
                domainPostingLog.getFromAccountId(),
                domainPostingLog.getToAccountId(),
                domainPostingLog.getAmount(),
                domainPostingLog.getCurrSymCode(),
                domainPostingLog.getDescription()
        );
    }

    public static PostingLog convertToDomainPosting(
            Posting protocolPosting,
            PostingBatch batch,
            PostingPlanLog currentDomainPlanLog
    ) {
        return new PostingLog(
                0,
                currentDomainPlanLog.getPlanId(),
                batch.getId(),
                protocolPosting.getFromId(),
                protocolPosting.getToId(),
                protocolPosting.getAmount(),
                Instant.now(),
                currentDomainPlanLog.getLastOperation(),
                protocolPosting.getCurrencySymCode(),
                protocolPosting.getDescription()
        );
    }

    public static PostingPlanLog convertToDomainPlan(
            PostingPlan protocolPostingPlan,
            PostingOperation domainPostingOperation
    ) {
        long lastBatchId = protocolPostingPlan.getBatchList().stream()
                .mapToLong(batch -> batch.getId())
                .max()
                .getAsLong();
        PostingPlanLog domainPlanLog =
                new PostingPlanLog(protocolPostingPlan.getId(), Instant.now(), domainPostingOperation, lastBatchId);
        return domainPlanLog;
    }

    public static PostingBatch convertFromDomainToBatch(long batchId, List<PostingLog> domainPostings) {
        return new PostingBatch(batchId, domainPostings.stream()
                .map(ProtocolConverter::convertFromDomainToPosting)
                .collect(Collectors.toList()));
    }


}
