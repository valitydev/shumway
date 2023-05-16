package dev.vality.shumway.config;

import dev.vality.shumway.dao.AccountDao;
import dev.vality.shumway.dao.AccountReplicaDao;
import dev.vality.shumway.dao.PostingPlanDao;
import dev.vality.shumway.dao.impl.AccountDaoImplNew;
import dev.vality.shumway.dao.impl.AccountReplicaDaoImpl;
import dev.vality.shumway.dao.impl.PostingPlanDaoImpl;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;

@Configuration
public class DaoConfiguration {

    @Bean(name = "accountDao")
    public AccountDao accountDao(DataSource dataSource) {
        return new AccountDaoImplNew(dataSource);
    }

    @Bean(name = "replicaDao")
    public AccountReplicaDao replicaDao(DataSource dataSource) {
        return new AccountReplicaDaoImpl(dataSource);
    }

    @Bean(name = "postingPlanDao")
    public PostingPlanDao postingPlanDao(DataSource dataSource) {
        return new PostingPlanDaoImpl(dataSource);
    }

}
