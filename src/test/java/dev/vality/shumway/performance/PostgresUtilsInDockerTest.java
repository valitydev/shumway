package dev.vality.shumway.performance;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

import java.io.IOException;

@Disabled
public class PostgresUtilsInDockerTest {

    @Test
    public void testAllInOne() throws IOException, InterruptedException {
        Thread.sleep(5000); //sometimes ".waitingForService("postgres", HealthChecks.toHaveAllPortsOpen())" doesn't work

        // configure utils for postgres in docker
        final PostgresUtils utils = PostgresUtils.builder()
                .host("localhost")
                .port(5432)
                .superUser("postgres")
                .password("postgres")
                .database("shumway")
                .bashScriptPath(new ClassPathResource("db/docker-wrapper.sh").getFile().getAbsolutePath())
               // .containerId(getRawContainerName(docker, "postgres"))
                .bashScriptInContainerPath("/src/test/resources/db/utils.sh")
                .build();

        final String dumpPath = "/tmp/shumway.dump";

        utils.dropDb();
        utils.createDb();
        utils.createDump(dumpPath);
        utils.createSnapshot();
        utils.dropDb();
        utils.restoreDump(dumpPath);
        utils.restoreSnapshot();
        utils.dropSnapshot();
    }
}
