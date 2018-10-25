package sb001.miniturbo.vertex.resource;

import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import io.vertx.core.Vertx;
import io.vertx.core.json.JsonArray;
import io.vertx.junit5.Timeout;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;

@ExtendWith(VertxExtension.class)
public class TestTurboResourceVerticle {

    @BeforeEach
    void deploy_verticle(Vertx vertx, VertxTestContext testContext) {
        vertx.deployVerticle(new TurboResourceVerticle(), testContext.succeeding(id -> testContext.completeNow()));
    }

    @Test
    @DisplayName("Should start a Web Server on port 8081")
    @Timeout(value = 10, timeUnit = TimeUnit.SECONDS)
    public void testGetAllResources(Vertx vertx, VertxTestContext testContext) throws Throwable {
        vertx.createHttpClient().getNow(TurboResourceVerticle.SERVER_PORT, "localhost", "/",
                response -> testContext.verify(() -> {
                    Assertions.assertTrue(response.statusCode() == 200);
                    response.handler(body -> {

                        JsonArray resources = body.toJsonArray();
                        Assertions.assertTrue(resources.contains("redis"));
                        Assertions.assertTrue(resources.contains("cassandra"));

                        testContext.completeNow();
                    });
                }));
    }

}
