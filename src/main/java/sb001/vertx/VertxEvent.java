package sb001.vertx;

import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import lombok.extern.slf4j.Slf4j;
import rx.functions.Action1;

@Slf4j
public class VertxEvent {

    public static void publish(Vertx vertx, String address, JsonObject value) {

        if (VertxRedis.redisIsEnabled(vertx.getOrCreateContext().config())) {
            VertxRedis.redisClient(vertx).publish(address, value.encodePrettily(), h -> {
                if (h.succeeded()) {
                    log.debug("Message '{}' succesfully published on '{}' redis channel.", value.toString(), address);
                } else {
                    log.error("Failed to publish message '{}' on '{}' redis channel.", value.toString(), address,
                            h.cause());
                }
            });
        } else {
            vertx.eventBus().send(address, value);
        }

    }

    public static void consumer(Vertx vertx, String address, Action1<JsonObject> handler) {

        if (VertxRedis.redisIsEnabled(vertx.getOrCreateContext().config())) {

            String redisChannel = String.format("io.vertx.redis.%s", address);
            vertx.eventBus().<JsonObject>consumer(redisChannel, received -> {
                if (received.body() != null && handler != null) {

                    log.debug("Consumer '{}' message: {}", address, received.body());
                    String jsonBody = String.valueOf(received.body().getJsonObject("value").getMap().get("message"));

                    handler.call(new JsonObject(jsonBody));

                } else {
                    log.warn("It wasn't possible to consume message");
                }
            });

            VertxRedis.redisClient(vertx).subscribe(address, res -> {
                if (res.succeeded()) {
                    log.debug("Redis subscribed message from '{}': {}", address, res.result().encodePrettily());
                } else {
                    log.error("Failed to subscribe message from '{}': {}", address, res.cause());
                }
            });
            log.debug("Consumer registered for {}", address);

        } else {
            vertx.eventBus().<JsonObject>consumer(address, res -> handler.call(res.body()));
        }
    }

}
