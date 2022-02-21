package dev.vality.shumway.dao;

import java.time.LocalDateTime;

public interface AccountReplicaDao {

    long getAccountBalance(long id, LocalDateTime time) throws DaoException;

}
