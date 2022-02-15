package dev.vality.shumway;

import dev.vality.damsel.accounter.AccounterSrv;
import dev.vality.shumway.dao.SupportAccountDao;
import dev.vality.shumway.utils.AccountUtils;
import dev.vality.woody.thrift.impl.http.THSpawnClientBuilder;
import lombok.extern.slf4j.Slf4j;
import org.apache.thrift.TException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Disabled;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.test.context.TestPropertySource;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;

import static dev.vality.shumway.utils.AccountUtils.startCircleCheck;
import static dev.vality.shumway.utils.AccountUtils.startCircleTransfer;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;

@Disabled
@SpringBootTest(webEnvironment = RANDOM_PORT)
@TestPropertySource(locations = "classpath:test.properties")
@Slf4j
public class HighAvailabilityTest {
    private static final int NUMBER_OF_THREADS = 8;
    private static final int SIZE_OF_QUEUE = NUMBER_OF_THREADS * 8;
    private static final int NUMBER_OF_ACCS = 40000;
    private static final int AMOUNT = 1000;

    @Autowired
    AccounterSrv.Iface client;

    @Autowired
    SupportAccountDao supportAccountDao;

    @LocalServerPort
    int port;

//    @ClassRule
//    public static DockerComposeRule docker = DockerComposeRule.builder()
//            .file("src/test/resources/docker-compose.yml")
//            .logCollector(new FileLogCollector(new File("target/pglog")))
//            .waitingForService("postgres", HealthChecks.toHaveAllPortsOpen())
//            .build();

    @Test
    @Disabled
    public void testRemote() throws URISyntaxException, TException, InterruptedException {
        THSpawnClientBuilder clientBuilder =
                new THSpawnClientBuilder().withAddress(new URI("http://localhost:" + getPort() + "/accounter"));
        client = clientBuilder.build(AccounterSrv.Iface.class);
        testHighAvailability();
    }

    @Test
    public void testLocal() throws URISyntaxException, TException, InterruptedException {
        testHighAvailability();
    }

    // move money 1 -> 2 -> 3 .. -> N -> 1
    // after all transactions amount on all accounts should be zero
    // also check intermediate amounts
    @SuppressWarnings("VariableDeclarationUsageDistance")
    private void testHighAvailability() throws TException, InterruptedException {
        long totalStartTime = System.currentTimeMillis();
        Assertions.assertNotNull(client);

        List<Long> accs = AccountUtils.createAccounts(NUMBER_OF_ACCS, supportAccountDao);

        AccountUtils.startCircleTransfer(client, accs, NUMBER_OF_THREADS, SIZE_OF_QUEUE, AMOUNT);
        AccountUtils.startCircleCheck(client, accs, 0);

        log.warn("Total time: {}ms", (System.currentTimeMillis() - totalStartTime));
    }

    private int getPort() {
        return port;
    }
}
