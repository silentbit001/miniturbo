package sb001.miniturbo.vertex.resource;

import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import io.vertx.core.DeploymentOptions;
import io.vertx.core.Vertx;
import io.vertx.junit5.Timeout;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import io.vertx.servicediscovery.ServiceDiscovery;
import sb001.miniturbo.vertx.resource.TurboResourceVerticle;
import sb001.miniturbo.vertx.resource.client.TurboResourceClient;

@DisplayName("TurboResourceVerticle")
@ExtendWith(VertxExtension.class)
public class TurboResourceClientTest {

    static ServiceDiscovery discovery;

    static final int TIMETOU_IN_SECONDS = 3;

    @BeforeAll
    @DisplayName("Should start a Web Server")
    static void deploy_verticle(Vertx vertx, VertxTestContext testContext) {

        vertx.deployVerticle(new TurboResourceVerticle(), new DeploymentOptions(),
                testContext.succeeding(id -> testContext.completeNow()));

        discovery = ServiceDiscovery.create(vertx);
    }

    @Test
    @DisplayName("Should return some resources by service discovery")
    @Timeout(value = TIMETOU_IN_SECONDS, timeUnit = TimeUnit.SECONDS)
    void testGetAllResourcesByServiceDiscovery(Vertx vertx, VertxTestContext testContext) throws Throwable {

        TurboResourceClient.getAllResources(discovery, resources -> testContext.verify(() -> {
            Assertions.assertTrue(resources.contains("redis"));
            Assertions.assertTrue(resources.contains("cassandra"));
            testContext.completeNow();
        }));

    }

    @ParameterizedTest
    @DisplayName("Should return resource by id ")
    @Timeout(value = TIMETOU_IN_SECONDS, timeUnit = TimeUnit.SECONDS)
    @MethodSource("getResources")
    void testGetResourcesByIdParameterized(String id, Vertx vertx, VertxTestContext testContext) throws Throwable {

        TurboResourceClient.getResourceById(discovery, id, resource -> testContext.verify(() -> {

            Assertions.assertTrue(StringUtils.isNotBlank(resource));

            String expectedResource = vertx.fileSystem().readFileBlocking(String.format("deployments/%s.yaml", id))
                    .toString();
            Assertions.assertEquals(expectedResource, resource);

            testContext.completeNow();

        }));

    }

    static Stream<String> getResources() {
        return TurboResourceClient.getAllResources(discovery).stream();
    }

}
