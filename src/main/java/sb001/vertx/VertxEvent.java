package sb001.vertx;

import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.eventbus.Message;

public class VertxEvent {

    public static <T> void send(Vertx vertx, String address, T value) {
        vertx.eventBus().send(address, value);
    }

    public static <T> void consumer(Vertx vertx, String address, Handler<Message<T>> handler) {
        vertx.eventBus().consumer(address, handler);
    }

}
