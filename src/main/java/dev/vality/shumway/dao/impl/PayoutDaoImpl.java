package dev.vality.shumway.dao.impl;

import dev.vality.shumway.dao.DaoException;
import dev.vality.shumway.dao.PayoutDao;
import org.springframework.core.NestedRuntimeException;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcDaoSupport;

import javax.sql.DataSource;
import java.sql.Types;
import java.time.LocalDateTime;

public class PayoutDaoImpl extends NamedParameterJdbcDaoSupport implements PayoutDao {

    public PayoutDaoImpl(DataSource ds) {
        setDataSource(ds);
    }

    @Override
    public long getStatefulAccountAvailableAmount(long id, LocalDateTime time) throws DaoException {
        final String sql =
                "select own_accumulated from shm.account_log where account_id = :id and creation_time <= :time " +
                        "order by creation_time desc limit 1;";

        MapSqlParameterSource params =
                new MapSqlParameterSource().addValue("id", id).addValue("time", time, Types.OTHER);
        try {
            return getNamedParameterJdbcTemplate().queryForObject(sql, params, Long.class);
        } catch (EmptyResultDataAccessException e) {
            return 0L;
        } catch (NestedRuntimeException e) {
            throw new DaoException(e);
        }
    }

}
