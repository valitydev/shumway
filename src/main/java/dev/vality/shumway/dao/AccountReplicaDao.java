package dev.vality.shumway.dao;

import dev.vality.shumway.domain.AccountBalance;
import org.springframework.lang.Nullable;

import java.time.LocalDateTime;
import java.util.Optional;

public interface AccountReplicaDao {

    Optional<AccountBalance> getAccountBalance(long id,
                                               @Nullable LocalDateTime fromTime,
                                               LocalDateTime toTime) throws DaoException;

    Optional<AccountBalance> getAccountBalance(long id,
                                               LocalDateTime toTime) throws DaoException;

}
