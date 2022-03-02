package dev.vality.shumway.dao.impl;

import dev.vality.shumway.dao.DaoException;
import dev.vality.shumway.dao.AccountReplicaDao;
import org.springframework.core.NestedRuntimeException;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcDaoSupport;

import javax.sql.DataSource;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.time.LocalDateTime;

public class AccountReplicaDaoImpl extends NamedParameterJdbcDaoSupport implements AccountReplicaDao {

    private final AccountBalanceMapper balanceMapper = new AccountBalanceMapper();

    public AccountReplicaDaoImpl(DataSource ds) {
        setDataSource(ds);
    }

    @Override
    public Long getAccountBalanceDiff(long id, LocalDateTime fromTime, LocalDateTime toTime) throws DaoException {
        final String sql = """
                with finish_amount as
                (select id, own_accumulated from shm.account_log
                    where id = :id and creation_time < :to_time
                        order by creation_time desc limit 1),
                    start_amount as
                    (select id, own_accumulated from shm.account_log
                        where id = :id and creation_time < :from_time
                            order by creation_time desc limit 1)
                select coalesce(finish_amount.own_accumulated, 0) - coalesce(start_amount.own_accumulated, 0) as balance
                from  shm.account a
                left join finish_amount on a.id = finish_amount.id
                left join start_amount on a.id = start_amount.id
                where a.id = :id""";

        MapSqlParameterSource params =
                new MapSqlParameterSource().addValue("id", id).addValue("from_time", fromTime, Types.OTHER)
                        .addValue("to_time", toTime, Types.OTHER);
        try {
            return getNamedParameterJdbcTemplate().queryForObject(sql, params, balanceMapper);
        } catch (EmptyResultDataAccessException e) {
            return null;
        } catch (NestedRuntimeException e) {
            throw new DaoException(e);
        }
    }

    @Override
    public Long getAccountBalance(long id, LocalDateTime dateTime) throws DaoException {
        final String sql = """
                with current_amount as
                (select id, own_accumulated from shm.account_log
                    where id = :id and creation_time < :to_time
                        order by creation_time desc limit 1)
                select coalesce(current_amount.own_accumulated, 0) as balance
                from  shm.account a
                left join current_amount on a.id = current_amount.id
                where a.id = :id""";

        MapSqlParameterSource params =
                new MapSqlParameterSource().addValue("id", id).addValue("to_time", dateTime, Types.OTHER);
        try {
            return getNamedParameterJdbcTemplate().queryForObject(sql, params, balanceMapper);
        } catch (EmptyResultDataAccessException e) {
            return null;
        } catch (NestedRuntimeException e) {
            throw new DaoException(e);
        }
    }

    private static class AccountBalanceMapper implements RowMapper<Long> {
        @Override
        public Long mapRow(ResultSet rs, int i) throws SQLException {
            return rs.getObject("balance", Long.class);
        }
    }

}
