package dev.vality.shumway.dao;

import dev.vality.shumway.domain.AccountBalance;

import java.time.LocalDateTime;
import java.util.Optional;

public interface AccountReplicaDao {

    Optional<AccountBalance> getAccountBalance(long id, LocalDateTime fromTime, LocalDateTime toTime) throws DaoException;

}
