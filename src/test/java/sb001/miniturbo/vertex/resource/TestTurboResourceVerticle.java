package sb001.miniturbo.vertex.resource;

import java.util.Optional;
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
import io.vertx.core.buffer.Buffer;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.junit5.Timeout;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import io.vertx.servicediscovery.ServiceDiscovery;
import sb001.miniturbo.vertx.resource.TurboResourceVerticle;
import sb001.vertx.VertxHttpClient;

@DisplayName("TurboResourceVerticle")
@ExtendWith(VertxExtension.class)
public class TestTurboResourceVerticle {

    int port = 8081;
    static ServiceDiscovery discovery;

    static final int TIMETOU_IN_SECONDS = 3;

    @BeforeAll
    @DisplayName("Should start a Web Server on port 8081")
    static void deploy_verticle(Vertx vertx, VertxTestContext testContext) {

        DeploymentOptions options = new DeploymentOptions().setConfig(new JsonObject().put("http.port", 8081));

        vertx.deployVerticle(new TurboResourceVerticle(), options,
                testContext.succeeding(id -> testContext.completeNow()));

        discovery = ServiceDiscovery.create(vertx);
    }

    @Test
    @DisplayName("Should return some resources like redis and cassandra")
    @Timeout(value = TIMETOU_IN_SECONDS, timeUnit = TimeUnit.SECONDS)
    void testGetAllResources(Vertx vertx, VertxTestContext testContext) throws Throwable {
        vertx.createHttpClient().getNow(port, "localhost", "/", response -> testContext.verify(() -> {
            Assertions.assertTrue(response.statusCode() == 200);
            response.handler(body -> {

                JsonArray resources = body.toJsonArray();
                Assertions.assertTrue(resources.contains("redis"));
                Assertions.assertTrue(resources.contains("cassandra"));

                testContext.completeNow();
            });
        }));
    }

    @Test
    @DisplayName("Should return some resources by service discovery")
    @Timeout(value = TIMETOU_IN_SECONDS, timeUnit = TimeUnit.SECONDS)
    void testGetAllResourcesByServiceDiscovery(Vertx vertx, VertxTestContext testContext) throws Throwable {

        VertxHttpClient.get(discovery, "miniturbo-resource", "/", buffer -> testContext.verify(() -> {

            JsonArray resources = buffer.toJsonArray();
            Assertions.assertTrue(resources.contains("redis"));
            Assertions.assertTrue(resources.contains("cassandra"));

            testContext.completeNow();

        }), response -> testContext.failed());

    }

    @ParameterizedTest
    @DisplayName("Should return resource by id ")
    @Timeout(value = TIMETOU_IN_SECONDS, timeUnit = TimeUnit.SECONDS)
    @MethodSource("getResources")
    void testGetResourcesByIdParameterized(String id, Vertx vertx, VertxTestContext testContext) throws Throwable {

        VertxHttpClient.get(discovery, "miniturbo-resource", String.format("/%s", id),
                buffer -> testContext.verify(() -> {

                    String deployment = buffer.toString();
                    Assertions.assertTrue(StringUtils.isNotBlank(deployment));

                    String expectedResource = vertx.fileSystem()
                            .readFileBlocking(String.format("deployments/%s.yaml", id)).toString();
                    Assertions.assertEquals(expectedResource, deployment);

                    testContext.completeNow();

                }), response -> testContext.failed());

    }

    static Stream<String> getResources() {

        Optional<Buffer> resources = VertxHttpClient.getSync(discovery, "miniturbo-resource", "/");

        if (!resources.isPresent()) {
            throw new RuntimeException("Not found resources");
        }

        return resources.get().toJsonArray().stream().map(String::valueOf);
    }

}
