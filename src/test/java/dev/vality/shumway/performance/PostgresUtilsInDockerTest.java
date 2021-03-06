package dev.vality.shumway.performance;

import com.palantir.docker.compose.DockerComposeRule;
import com.palantir.docker.compose.connection.ContainerName;
import com.palantir.docker.compose.connection.waiting.HealthChecks;
import com.palantir.docker.compose.logging.FileLogCollector;
import org.junit.ClassRule;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Disabled;
import org.springframework.core.io.ClassPathResource;

import java.io.File;
import java.io.IOException;

@Disabled
public class PostgresUtilsInDockerTest {
    @ClassRule
    public static DockerComposeRule docker = DockerComposeRule.builder()
            .file("src/test/resources/docker-compose.yml")
            .logCollector(new FileLogCollector(new File("target/pglog")))
            .waitingForService("postgres", HealthChecks.toHaveAllPortsOpen())
            .build();

    public static String getRawContainerName(DockerComposeRule docker, String serviceName)
            throws IOException, InterruptedException {
        for (ContainerName containerName : docker.dockerCompose().ps()) {
            if (serviceName.equals(containerName.semanticName())) {
                return containerName.rawName();
            }
        }

        return null;
    }

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
                .containerId(getRawContainerName(docker, "postgres"))
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
