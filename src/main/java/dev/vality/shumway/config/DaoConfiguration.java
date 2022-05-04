package dev.vality.shumway.config;

import dev.vality.shumway.dao.AccountDao;
import dev.vality.shumway.dao.AccountReplicaDao;
import dev.vality.shumway.dao.PostingPlanDao;
import dev.vality.shumway.dao.impl.AccountDaoImplNew;
import dev.vality.shumway.dao.impl.AccountReplicaDaoImpl;
import dev.vality.shumway.dao.impl.PostingPlanDaoImpl;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import javax.sql.DataSource;

@Configuration
public class DaoConfiguration {

    @Bean
    @Primary
    @ConfigurationProperties(prefix = "spring.datasource")
    public DataSource dataSource(@Value("${spring.datasource.url}") String url) {
        return DataSourceBuilder
                .create()
                .url(url)
                .build();
    }

    @Bean(name = "replicaDatasource")
    @ConfigurationProperties(prefix = "spring.replica-datasource")
    public DataSource replicaDataSource(@Value("${spring.replica-datasource.url}") String url) {
        return DataSourceBuilder
                .create()
                .url(url)
                .build();
    }

    @Bean(name = "accountDao")
    public AccountDao accountDao(DataSource dataSource) {
        return new AccountDaoImplNew(dataSource);
    }

    @Bean(name = "replicaDao")
    public AccountReplicaDao replicaDao(@Qualifier("replicaDatasource") DataSource dataSource) {
        return new AccountReplicaDaoImpl(dataSource);
    }

    @Bean(name = "postingPlanDao")
    public PostingPlanDao postingPlanDao(DataSource dataSource) {
        return new PostingPlanDaoImpl(dataSource);
    }

}
