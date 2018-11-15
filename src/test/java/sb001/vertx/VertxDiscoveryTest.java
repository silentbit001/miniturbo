package sb001.vertx;

import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.ext.web.Router;
import io.vertx.junit5.Timeout;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import io.vertx.servicediscovery.ServiceDiscovery;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@DisplayName("VertxDiscovery")
@ExtendWith(VertxExtension.class)
public class VertxDiscoveryTest {

    static ServiceDiscovery discovery;

    static final int TIMEOUT_IN_SECONDS = 3;

    @BeforeAll
    @SneakyThrows
    @DisplayName("Given I have server attached to service A")
    static void beforeAll(Vertx vertx, VertxTestContext testContext) {

        discovery = ServiceDiscovery.create(vertx);

        Future<Void> startFuture = Future.<Void>future();
        Router router = Router.router(vertx);
        router.get("/hi").handler(h -> h.response().end("hi I'm elfo"));
        VertxHttpServer.startServer(vertx, "A", router, discovery, startFuture);

        // wait until server be ready
        Futures.getResult(startFuture, 5000);
        testContext.completed();
    }

    @Test
    @SneakyThrows
    @Timeout(value = TIMEOUT_IN_SECONDS, timeUnit = TimeUnit.SECONDS)
    @DisplayName("Should fail when I search by unregistered service B")
    public void shouldFailUnknowService(Vertx vertx, VertxTestContext testContext) {

        VertxDiscovery.discoveryHttpClient(discovery, "B", success -> {

            testContext.failed();
            testContext.completeNow();

        }, fail -> testContext.completeNow());

    }

}
