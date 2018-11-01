package sb001.vertex;

import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpServer;
import io.vertx.ext.web.Router;
import io.vertx.servicediscovery.ServiceDiscovery;
import io.vertx.servicediscovery.types.HttpEndpoint;
import lombok.extern.slf4j.Slf4j;
import rx.functions.Action1;

@Slf4j
public class VertexServer {

    public static void startServer(Vertx vertx, String serverName, int port, Router router,
            Handler<AsyncResult<HttpServer>> listenHandler) {
        vertx.createHttpServer().requestHandler(router::accept).listen(port, listenHandler);
    }

    public static void startServer(Vertx vertx, String serverName, int port, Router router, Future<Void> startFuture) {
        startServer(vertx, serverName, port, router, listenerHandler(serverName, startFuture, null, null));
    }

    public static void startServer(Vertx vertx, String serverName, int port, Router router, ServiceDiscovery discovery,
            Future<Void> startFuture) {

        startServer(vertx, serverName, port, router, listenerHandler(serverName, startFuture, s -> {

            if (discovery != null) {

                discovery.publish(HttpEndpoint.createRecord(serverName, "localhost", port, "/"), h -> {
                    if (h.succeeded()) {
                        log.info("Service {} published", serverName);
                    } else {
                        startFuture.fail(h.cause());
                        log.error("Failed to publish service {}", serverName, h.cause());
                    }
                });

            } else {
                startFuture.complete();
            }

        }, null));
    }

    private static Handler<AsyncResult<HttpServer>> listenerHandler(String serverName, Future<Void> startFuture,
            Action1<HttpServer> success, Action1<Throwable> fallback) {
        return lH -> {
            if (lH.succeeded()) {
                log.info("Server {} ready.", serverName);
                if (success != null) {
                    success.call(lH.result());
                } else {
                    startFuture.complete();
                }
            } else {
                log.error("Failed to start server {}.", serverName, lH.cause());
                if (fallback != null) {
                    fallback.call(lH.cause());
                } else {
                    startFuture.fail(lH.cause());
                }
            }

        };
    }

}
