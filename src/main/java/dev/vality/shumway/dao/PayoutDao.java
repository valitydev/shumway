package dev.vality.shumway.dao;

import java.time.LocalDateTime;

public interface PayoutDao {

    long getStatefulAccountAvailableAmount(long id, LocalDateTime time) throws DaoException;

}
