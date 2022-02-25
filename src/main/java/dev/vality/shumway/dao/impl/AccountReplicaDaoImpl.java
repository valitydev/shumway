package dev.vality.shumway.dao.impl;

import dev.vality.shumway.dao.DaoException;
import dev.vality.shumway.dao.AccountReplicaDao;
import dev.vality.shumway.domain.AccountBalance;
import org.springframework.core.NestedRuntimeException;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcDaoSupport;
import org.springframework.lang.Nullable;

import javax.sql.DataSource;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.time.LocalDateTime;
import java.util.Optional;

public class AccountReplicaDaoImpl extends NamedParameterJdbcDaoSupport implements AccountReplicaDao {

    private final AccountBalanceMapper balanceMapper = new AccountBalanceMapper();

    public AccountReplicaDaoImpl(DataSource ds) {
        setDataSource(ds);
    }

    @Override
    public Optional<AccountBalance> getAccountBalance(long id,
                                                      LocalDateTime fromTime,
                                                      LocalDateTime toTime) throws DaoException {
        final String sql = """
                select ac.id, al1.own_accumulated as start_balance, 
                              al2.own_accumulated as final_balance from shm.account ac 
                left join shm.account_log al1 on al1.account_id = ac.id and al1.creation_time <= :from_time 
                left join shm.account_log al2 on al2.account_id = ac.id and al2.creation_time < :to_time 
                 where ac.id = :id 
                 order by al1.creation_time, al2.creation_time desc limit 1;""";

        MapSqlParameterSource params =
                new MapSqlParameterSource()
                        .addValue("id", id)
                        .addValue("from_time", fromTime, Types.OTHER)
                        .addValue("to_time", toTime, Types.OTHER);
        try {
            return Optional.of(getNamedParameterJdbcTemplate().queryForObject(sql, params, balanceMapper));
        } catch (EmptyResultDataAccessException e) {
            return Optional.empty();
        } catch (NestedRuntimeException e) {
            throw new DaoException(e);
        }
    }

    @Override
    public Optional<AccountBalance> getAccountBalance(long id, LocalDateTime toTime) throws DaoException {
        final String sql = """
                select ac.id, al.own_accumulated as final_balance from shm.account ac 
                left join shm.account_log al on al.account_id = ac.id and al.creation_time < :to_time 
                where ac.id = :id 
                order by al.creation_time desc limit 1;""";

        MapSqlParameterSource params =
                new MapSqlParameterSource()
                        .addValue("id", id)
                        .addValue("to_time", toTime, Types.OTHER);
        try {
            return Optional.of(getNamedParameterJdbcTemplate().queryForObject(sql, params, balanceMapper));
        } catch (EmptyResultDataAccessException e) {
            return Optional.empty();
        } catch (NestedRuntimeException e) {
            throw new DaoException(e);
        }
    }

    private static class AccountBalanceMapper implements RowMapper<AccountBalance> {
        @Override
        public AccountBalance mapRow(ResultSet rs, int i) throws SQLException {
            Long id = rs.getObject("id", Long.class);
            Long finalAmount = rs.getObject("final_balance", Long.class);
            Long startAmount = null;

            if (rs.getMetaData().getColumnCount() == 3) {
                startAmount = rs.getObject("start_balance", Long.class);
            }
            return new AccountBalance(id, startAmount, finalAmount);
        }
    }

}
