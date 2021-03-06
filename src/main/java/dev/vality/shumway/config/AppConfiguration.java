package dev.vality.shumway.config;

import dev.vality.shumway.dao.AccountDao;
import dev.vality.shumway.dao.AccountReplicaDao;
import dev.vality.shumway.dao.PostingPlanDao;
import dev.vality.shumway.handler.AccounterHandler;
import dev.vality.shumway.service.AccountService;
import dev.vality.shumway.service.PostingPlanService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

@Configuration
public class AppConfiguration {

    @Value("${db.jdbc.tr_timeout}")
    private int transactionTimeoutSec;

    @Bean
    public TransactionTemplate transactionTemplate(PlatformTransactionManager transactionManager) {
        TransactionTemplate transactionTemplate = new TransactionTemplate(transactionManager);
        transactionTemplate.setIsolationLevel(TransactionDefinition.ISOLATION_READ_COMMITTED);
        transactionTemplate.setTimeout(transactionTimeoutSec);
        return transactionTemplate;
    }

    @Bean
    public AccountService accountService(AccountDao accountDao, AccountReplicaDao replicaDao) {
        return new AccountService(accountDao, replicaDao);
    }

    @Bean
    PostingPlanService postingPlanService(PostingPlanDao postingPlanDao) {
        return new PostingPlanService(postingPlanDao);
    }

    @Bean
    AccounterHandler accounterHandler(
            AccountService accountService,
            PostingPlanService postingPlanService,
            TransactionTemplate transactionTemplate
    ) {
        return new AccounterHandler(accountService, postingPlanService, transactionTemplate);
    }
}
