package sb001.miniturbo.vertex;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import io.vertx.core.Vertx;
import io.vertx.junit5.Timeout;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import sb001.miniturbo.vertex.resource.TurboResourceVerticle;

@ExtendWith(VertxExtension.class)
public class TestMiniTurboVerticle {

    @BeforeEach
    void deploy_verticle(Vertx vertx, VertxTestContext testContext) {
        vertx.deployVerticle(new MiniTurboVerticle(), testContext.succeeding(id -> testContext.completeNow()));
    }

    @Test
    @DisplayName("Should start a Web Server on port 8080")
    @Timeout(value = 10, timeUnit = TimeUnit.SECONDS)
    void start_http_server(Vertx vertx, VertxTestContext testContext) throws Throwable {
        vertx.createHttpClient().getNow(TurboResourceVerticle.SERVER_PORT, "localhost", "/",
                response -> testContext.verify(() -> {
                    assertTrue(response.statusCode() == 200);
                    response.handler(body -> {
                        assertTrue(body.toString().contains("Hello from Vert.x!"));
                        testContext.completeNow();
                    });
                }));
    }

}
