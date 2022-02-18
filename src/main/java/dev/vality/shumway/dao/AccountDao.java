package dev.vality.shumway.dao;

import dev.vality.shumway.domain.Account;
import dev.vality.shumway.domain.AccountLog;
import dev.vality.shumway.domain.AccountState;
import dev.vality.shumway.domain.StatefulAccount;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Map;

public interface AccountDao {
    long add(Account prototype) throws DaoException;

    void addLogs(List<AccountLog> accountLogs) throws DaoException;

    Account get(long id) throws DaoException;

    List<Account> get(Collection<Long> ids) throws DaoException;

    Map<Long, StatefulAccount> getStatefulUpTo(Collection<Long> ids, String planId, long batchId) throws DaoException;

    Map<Long, StatefulAccount> getStateful(Collection<Long> ids) throws DaoException;

    Map<Long, StatefulAccount> getStatefulExclusive(Collection<Long> ids) throws DaoException;

    Map<Long, AccountState> getAccountStates(Collection<Long> accountIds) throws DaoException;
}
