package dev.vality.shumway.dao;

import java.time.LocalDateTime;
import java.util.Optional;

public interface AccountReplicaDao {

    Long getAccountBalanceDiff(long id, LocalDateTime fromTime, LocalDateTime toTime) throws DaoException;

    Long getAccountBalance(long id, LocalDateTime dateTime) throws DaoException;

}
