package dev.vality.shumway.config;

import dev.vality.shumway.dao.AccountDao;
import dev.vality.shumway.dao.PayoutDao;
import dev.vality.shumway.dao.PostingPlanDao;
import dev.vality.shumway.dao.impl.AccountDaoImplNew;
import dev.vality.shumway.dao.impl.PayoutDaoImpl;
import dev.vality.shumway.dao.impl.PostingPlanDaoImpl;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import javax.sql.DataSource;

/**
 * Created by vpankrashkin on 30.06.16.
 */
@Configuration
public class DaoConfiguration {

    @Bean
    @Primary
    @ConfigurationProperties(prefix = "spring.datasource")
    public DataSource dataSource() {
        return DataSourceBuilder.create().build();
    }

    @Bean(name = "payoutDatasource")
    @ConfigurationProperties(prefix = "spring.payout-datasource")
    public DataSource payoutDataSource() {
        return DataSourceBuilder.create().build();
    }

    @Bean(name = "accountDao")
    public AccountDao accountDao(DataSource dataSource) {
        return new AccountDaoImplNew(dataSource);
    }

    @Bean(name = "payoutDao")
    public PayoutDao payoutDao(@Qualifier("payoutDatasource") DataSource dataSource) {
        return new PayoutDaoImpl(dataSource);
    }

    @Bean(name = "postingPlanDao")
    public PostingPlanDao postingPlanDao(DataSource dataSource) {
        return new PostingPlanDaoImpl(dataSource);
    }

}
