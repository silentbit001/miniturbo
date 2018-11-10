package sb001.vertx;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import co.paralleluniverse.strands.Strand;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import io.vertx.junit5.Timeout;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import io.vertx.servicediscovery.ServiceDiscovery;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import rx.functions.Action1;

@Slf4j
@DisplayName("VertxEvents")
@ExtendWith(VertxExtension.class)
public class VertxEventTest {

    static ServiceDiscovery discovery;

    static final int TIMEOUT_IN_SECONDS = 5;

    static Process redisDocker;

    @BeforeAll
    @SneakyThrows
    static void beforeAll(Vertx vertx, VertxTestContext testContext) {

        // start redis
        redisDocker = Runtime.getRuntime().exec("docker run -p 6379:6379 redis:4.0.11-alpine");
        BufferedReader in = new BufferedReader(new InputStreamReader(redisDocker.getInputStream()));
        String line;
        while ((line = in.readLine()) != null) {
            System.out.println(line);
            if (line.contains("Ready to accept connections")) {
                break;
            }
        }

        discovery = ServiceDiscovery.create(vertx);

        DeploymentOptions options = new DeploymentOptions().setConfig(new JsonObject().put("redis.enabled", true));

        vertx.deployVerticle(new MyVerticle("my-event-1"), options,
                testContext.succeeding(id -> testContext.completeNow()));
        vertx.deployVerticle(new MyVerticle("my-event-2"), options,
                testContext.succeeding(id -> testContext.completeNow()));
    }

    @AfterAll
    @SneakyThrows
    static void afterAll(Vertx vertx, VertxTestContext testContext) {
        redisDocker.destroy();
        testContext.completeNow();
    }

    @Test
    @SneakyThrows
    @DisplayName("Should publish message for all consumers")
    @Timeout(value = TIMEOUT_IN_SECONDS, timeUnit = TimeUnit.SECONDS)
    void publishMessageTest(Vertx vertx, VertxTestContext testContext) {

        AtomicInteger counter = new AtomicInteger(0);

        Action1<Buffer> consumerCallback = h -> testContext.verify(() -> {
            Assertions.assertEquals("1", h.toString());
            counter.addAndGet(Integer.valueOf(h.toString()));
        });

        VertxHttpClient.get(discovery, "my-event-1", "/consume", consumerCallback);
        VertxHttpClient.get(discovery, "my-event-2", "/consume", consumerCallback);

        // TODO: please make it more reliable
        Strand.sleep(2000); // wait consumer be ready
        VertxHttpClient.get(discovery, "my-event-1", "/publish"); // publish message

        // wait
        // messages to
        // be consumed
        // TODO: remove sync by sleep
        long startAt = System.currentTimeMillis();
        while (counter.get() != 2 && System.currentTimeMillis() - startAt < 3000) {
            Strand.sleep(100);
        }

        Assertions.assertEquals(2, counter.get());
        testContext.completeNow();

    }

    static class MyVerticle extends AbstractVerticle {

        private String name;

        public MyVerticle(String serviceName) {
            this.name = serviceName;
        }

        @Override
        public void start(Future<Void> startFuture) throws Exception {

            Router router = Router.router(vertx);
            router.get("/publish").handler(h -> {
                VertxEvent.publish(vertx, "event1", new JsonObject().put("name", "event1").put("value", 1));
                h.response().end();
            });

            router.get("/consume").handler(h -> {
                VertxEvent.consumer(vertx, "event1", ch -> {
                    String value = ch.getInteger("value").toString();
                    h.response().end(value);
                });
            });

            VertxHttpServer.startServer(vertx, this.name, router, discovery, startFuture);

        }

    }

}
