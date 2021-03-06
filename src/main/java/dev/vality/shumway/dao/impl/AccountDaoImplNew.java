package dev.vality.shumway.dao.impl;

import dev.vality.shumway.dao.AccountDao;
import dev.vality.shumway.dao.DaoException;
import dev.vality.shumway.domain.Account;
import dev.vality.shumway.domain.AccountLog;
import dev.vality.shumway.domain.AccountState;
import dev.vality.shumway.domain.StatefulAccount;
import org.springframework.core.NestedRuntimeException;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcDaoSupport;
import org.springframework.jdbc.support.GeneratedKeyHolder;

import javax.sql.DataSource;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.AbstractMap;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class AccountDaoImplNew extends NamedParameterJdbcDaoSupport implements AccountDao {
    private static final int BATCH_SIZE = 1000;

    private final AccountMapper accountMapper = new AccountMapper();
    private final StatefulAccountMapper statefulAccountMapper = new StatefulAccountMapper();
    private final AmountStatePairMapper amountStatePairMapper = new AmountStatePairMapper();

    public AccountDaoImplNew(DataSource ds) {
        setDataSource(ds);
    }

    @Override
    public long add(Account prototype) throws DaoException {
        final String sql = "INSERT INTO shm.account(curr_sym_code, creation_time, description) " +
                "VALUES (:curr_sym_code, :creation_time, :description) RETURNING id;";
        MapSqlParameterSource params = new MapSqlParameterSource();
        params.addValue("curr_sym_code", prototype.getCurrSymCode());
        params.addValue("creation_time", toLocalDateTime(prototype.getCreationTime()), Types.OTHER);
        params.addValue("description", prototype.getDescription());
        try {
            GeneratedKeyHolder keyHolder = new GeneratedKeyHolder();
            int updateCount = getNamedParameterJdbcTemplate().update(sql, params, keyHolder);
            if (updateCount != 1) {
                throw new DaoException("Account creation returned unexpected update count: " + updateCount);
            }
            return keyHolder.getKey().longValue();
        } catch (NestedRuntimeException e) {
            throw new DaoException(e);
        }
    }

    @Override
    public void addLogs(List<AccountLog> logs) throws DaoException {
        final String sql = "INSERT INTO shm.account_log(plan_id, batch_id, account_id, operation, own_accumulated, " +
                "max_accumulated, min_accumulated, own_diff, min_diff, max_diff, creation_time, credit, merged) " +
                "VALUES (?, ?, ?, ?::shm.POSTING_OPERATION_TYPE,?, ?, ?, ?, ?, ?, ?, ?, ?)";
        int[][] updateCounts = getJdbcTemplate().batchUpdate(sql, logs, BATCH_SIZE,
                (ps, argument) -> {
                    ps.setString(1, argument.getPlanId());
                    ps.setLong(2, argument.getBatchId());
                    ps.setLong(3, argument.getAccountId());
                    ps.setString(4, argument.getOperation().getKey());
                    ps.setLong(5, argument.getOwnAccumulated());
                    ps.setLong(6, argument.getMaxAccumulated());
                    ps.setLong(7, argument.getMinAccumulated());
                    ps.setLong(8, argument.getOwnDiff());
                    ps.setLong(9, argument.getMinDiff());
                    ps.setLong(10, argument.getMaxDiff());
                    ps.setObject(11, toLocalDateTime(argument.getCreationTime()), Types.OTHER);
                    ps.setBoolean(12, argument.isCredit());
                    ps.setBoolean(13, argument.isMerged());
                });
        boolean checked = false;
        for (int[] updateCount : updateCounts) {
            for (int i : updateCount) {
                checked = true;
                if (i != 1) {
                    throw new DaoException(
                            "Account log creation returned unexpected update count: " + i);
                }
            }
        }
        if (!checked) {
            throw new DaoException("Account log creation returned unexpected update count [0]");
        }
    }

    @Override
    public Account get(long id) {
        final String sql = "SELECT id, curr_sym_code, creation_time, description FROM shm.account WHERE id = :id";
        try {
            return getNamedParameterJdbcTemplate()
                    .queryForObject(sql, new MapSqlParameterSource("id", id), accountMapper);
        } catch (EmptyResultDataAccessException e) {
            return null;
        } catch (NestedRuntimeException e) {
            throw new DaoException(e);
        }
    }

    @Override
    public List<Account> get(Collection<Long> ids) throws DaoException {
        if (ids.isEmpty()) {
            return Collections.emptyList();
        } else {
            final String sql =
                    "SELECT id, curr_sym_code, creation_time, description FROM shm.account WHERE id in (:ids)";
            try {
                return getNamedParameterJdbcTemplate().query(sql, new MapSqlParameterSource("ids", ids), accountMapper);
            } catch (NestedRuntimeException e) {
                throw new DaoException(e);
            }
        }
    }

    @Override
    public Map<Long, StatefulAccount> getStateful(Collection<Long> ids) throws DaoException {
        if (ids.isEmpty()) {
            return Collections.emptyMap();
        } else {
            final String sql = "select * from shm.get_acc_stat(Array [ :ids ])";
            try {
                return getNamedParameterJdbcTemplate()
                        .query(sql, new MapSqlParameterSource("ids", ids), statefulAccountMapper)
                        .stream()
                        .collect(Collectors.toMap(Account::getId, acc -> acc));
            } catch (NestedRuntimeException e) {
                throw new DaoException(e);
            }
        }
    }

    @Override
    public Map<Long, StatefulAccount> getStatefulUpTo(Collection<Long> ids, String planId, long batchId)
            throws DaoException {
        if (ids.isEmpty()) {
            return Collections.emptyMap();
        } else {
            final String sql = "select * from shm.get_acc_stat_upto(Array[ :ids ], :plan_id, :batch_id)";
            MapSqlParameterSource params = new MapSqlParameterSource()
                    .addValue("ids", ids)
                    .addValue("plan_id", planId)
                    .addValue("batch_id", batchId);
            try {
                return getNamedParameterJdbcTemplate().query(sql, params, statefulAccountMapper)
                        .stream()
                        .collect(Collectors.toMap(Account::getId, acc -> acc));
            } catch (NestedRuntimeException e) {
                throw new DaoException(e);
            }
        }
    }

    @Override
    public Map<Long, StatefulAccount> getStatefulExclusive(Collection<Long> ids) throws DaoException {
        if (ids.isEmpty()) {
            return Collections.emptyMap();
        } else {
            final String sql = "select * from shm.get_exclusive_acc_stat(Array[ :ids ])";
            try {
                return getNamedParameterJdbcTemplate()
                        .query(sql, new MapSqlParameterSource("ids", ids), statefulAccountMapper)
                        .stream()
                        .collect(Collectors.toMap(Account::getId, acc -> acc));
            } catch (NestedRuntimeException e) {
                throw new DaoException(e);
            }
        }
    }

    @Override
    public Map<Long, AccountState> getAccountStates(Collection<Long> accountIds) throws DaoException {
        if (accountIds.isEmpty()) {
            return Collections.emptyMap();
        } else {
            final String sql = "select " +
                    "account_id,  " +
                    "own_accumulated, " +
                    "max_accumulated, " +
                    "min_accumulated  " +
                    "from shm.account_log al " +
                    "join (values (:ids)) as t (acId) " +
                    "on al.id = (select max(id) from shm.account_log  where account_id = t.acId);";
            try {
                return fillAbsentValues(accountIds, getNamedParameterJdbcTemplate()
                        .query(sql, new MapSqlParameterSource("ids", accountIds), amountStatePairMapper)
                        .stream()
                        .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue)));
            } catch (NestedRuntimeException e) {
                throw new DaoException(e);
            }
        }
    }

    private Map<Long, AccountState> fillAbsentValues(Collection<Long> accountIds, Map<Long, AccountState> stateMap) {
        accountIds.forEach(id -> stateMap.putIfAbsent(id, new AccountState()));
        return stateMap;
    }

    private LocalDateTime toLocalDateTime(Instant instant) {
        return LocalDateTime.ofInstant(instant, ZoneOffset.UTC);
    }

    private static class AmountStatePairMapper implements RowMapper<Map.Entry<Long, AccountState>> {
        @Override
        public Map.Entry<Long, AccountState> mapRow(ResultSet rs, int rowNum) throws SQLException {
            long accountId = rs.getLong("account_id");
            long ownAccumulatedAmount = rs.getLong("own_accumulated");
            long minAccumulatedDiff = rs.getLong("min_accumulated");
            long maxAccumulatedDiff = rs.getLong("max_accumulated");
            AccountState accountState = new AccountState(ownAccumulatedAmount, minAccumulatedDiff, maxAccumulatedDiff);
            return new AbstractMap.SimpleEntry<>(accountId, accountState);
        }
    }

    private static class AccountMapper implements RowMapper<Account> {
        @Override
        public Account mapRow(ResultSet rs, int i) throws SQLException {
            long id = rs.getLong("id");
            String currSymCode = rs.getString("curr_sym_code");
            Instant creationTime = rs.getObject("creation_time", LocalDateTime.class).toInstant(ZoneOffset.UTC);
            String description = rs.getString("description");
            return new Account(id, creationTime, currSymCode, description);
        }
    }

    private static class StatefulAccountMapper implements RowMapper<StatefulAccount> {
        @Override
        public StatefulAccount mapRow(ResultSet rs, int i) throws SQLException {
            AccountState accountState;
            Long ownAccumulatedAmount = rs.getObject("own_accumulated", Long.class);
            if (rs.wasNull()) {
                accountState = new AccountState();
            } else {
                long minAccumulatedDiff = rs.getLong("min_accumulated");
                long maxAccumulatedDiff = rs.getLong("max_accumulated");
                accountState = new AccountState(ownAccumulatedAmount, minAccumulatedDiff, maxAccumulatedDiff);
            }

            long id = rs.getLong("account_id");
            String currSymCode = rs.getString("curr_sym_code");
            Instant creationTime = rs.getObject("creation_time", LocalDateTime.class).toInstant(ZoneOffset.UTC);
            String description = rs.getString("description");

            return new StatefulAccount(id, creationTime, currSymCode, description, accountState);
        }
    }
}

