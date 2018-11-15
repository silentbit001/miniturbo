package sb001.vertx;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

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
import io.vertx.core.json.JsonObject;
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

    static final int TIMEOUT_IN_SECONDS = 20;

    static Process redisDocker;

    @BeforeAll
    @SneakyThrows
    @DisplayName("Given I have a redis running")
    static void beforeAll(Vertx vertx, VertxTestContext testContext) {

        // start redis
        redisDocker = Runtime.getRuntime().exec("docker run -p 6379:6379 redis:4.0.11-alpine");
        BufferedReader in = new BufferedReader(new InputStreamReader(redisDocker.getInputStream()));
        String line;
        while ((line = in.readLine()) != null) {
            System.out.println("[redis] " + line);
            if (line.contains("Ready to accept connections")) {
                break;
            }
        }

        testContext.completeNow();

    }

    @Test
    @SneakyThrows
    @Timeout(value = TIMEOUT_IN_SECONDS, timeUnit = TimeUnit.SECONDS)
    @DisplayName("Should publish and consume message from redis")
    public void shouldPublishAndConsumeMessages(Vertx vertx, VertxTestContext testContext) {

        DeploymentOptions options = new DeploymentOptions().setConfig(new JsonObject().put("redis.enabled", true));

        Future<Integer> counterFut = Future.future();
        AtomicInteger aggregate = new AtomicInteger(0);

        Action1<JsonObject> mHandler = message -> {
            log.debug("Got message {}", message);
            int count = aggregate.addAndGet(message.getInteger("value"));
            log.debug("Count {}", count);
            if (count >= 3) {
                counterFut.complete(count);
            }
        };

        // deploy three verticles with one redis consumers each
        Stream.of(1, 2, 3).parallel().forEach(i -> vertx.deployVerticle(new AbstractVerticle() {
            @Override
            public void start(Future<Void> startFuture) throws Exception {
                VertxRedisEvent.consumer(vertx, "event1", mHandler);
                startFuture.complete();
            }
        }, options));

        // deploy verticle with just redis publishers
        Strand.sleep(7000);
        vertx.deployVerticle(new AbstractVerticle() {

            @Override
            public void start(Future<Void> startFuture) throws Exception {
                VertxRedisEvent.publish(vertx, "event1", new JsonObject().put("value", 1));
                VertxRedisEvent.publish(vertx, "event2", new JsonObject().put("value", 2));
                startFuture.complete();
                log.debug("Messages published.");
            }

        }, options);

        Assertions.assertEquals(Integer.valueOf(3), Futures.getResult(counterFut, 7000));
        testContext.completeNow();

    }

    @AfterAll
    @SneakyThrows
    @DisplayName("After all I should stop redis")
    @Timeout(value = TIMEOUT_IN_SECONDS, timeUnit = TimeUnit.SECONDS)
    static void afterAll(Vertx vertx, VertxTestContext testContext) {
        redisDocker.destroy();
        testContext.completeNow();
    }

}
