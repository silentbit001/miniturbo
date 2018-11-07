package sb001.vertx;

import java.net.ServerSocket;

import org.apache.commons.lang3.RandomUtils;

import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpServer;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import io.vertx.servicediscovery.ServiceDiscovery;
import lombok.extern.slf4j.Slf4j;
import rx.functions.Action1;

@Slf4j
public class VertxHttpServer {

    private static final String DEFAULT_HOSTNAME = "localhost";
    private static String hostName;

    public static HttpServer startServer(HttpServer server, String serverName, int port, Router router,
            Handler<AsyncResult<HttpServer>> listenHandler) {
        return server.requestHandler(router::accept).listen(port, listenHandler);
    }

    public static HttpServer startServer(HttpServer server, String serverName, int port, Router router,
            Future<Void> startFuture) {
        return startServer(server, serverName, port, router,
                listenerHandler(serverName, DEFAULT_HOSTNAME, port, startFuture, null, null));
    }

    public static HttpServer startServer(Vertx vertx, String serverName, int port, Router router,
            Handler<AsyncResult<HttpServer>> listenHandler) {
        return vertx.createHttpServer().requestHandler(router::accept).listen(port, listenHandler);
    }

    public static HttpServer startServer(Vertx vertx, String serverName, int port, Router router,
            Future<Void> startFuture) {
        return startServer(vertx, serverName, port, router,
                listenerHandler(serverName, DEFAULT_HOSTNAME, port, startFuture, null, null));
    }

    public static HttpServer startServer(Vertx vertx, String serverName, int port, Router router,
            ServiceDiscovery discovery, Future<Void> startFuture) {
        return startServer(vertx, serverName, getHostName(), port, router, discovery, startFuture);
    }

    public static HttpServer startServer(Vertx vertx, String serverName, Router router, Future<Void> startFuture) {
        return startServer(vertx, serverName, router, null, startFuture);
    }

    public static HttpServer startServer(Vertx vertx, String serverName, Router router, ServiceDiscovery discovery,
            Future<Void> startFuture) {
        int port = config(vertx).getInteger("http.port", pickUpRandomPort());
        return startServer(vertx, serverName, getHostName(), port, router, discovery, startFuture);
    }

    public static HttpServer startServer(Vertx vertx, String serverName, String host, int port, Router router,
            ServiceDiscovery discovery, Future<Void> startFuture) {

        boolean serviceAutoPublish = config(vertx).getBoolean("service.autopublish", Boolean.TRUE);
        return startServer(vertx, serverName, port, router, listenerHandler(serverName, host, port, startFuture, s -> {
            if (discovery != null && serviceAutoPublish) {
                VertxDiscovery.publishHttpService(discovery, serverName, host, port, "/", startFuture);
            } else {
                startFuture.complete();
            }

        }, null));

    }

    private static Handler<AsyncResult<HttpServer>> listenerHandler(String serverName, String host, int port,
            Future<Void> startFuture, Action1<HttpServer> success, Action1<Throwable> fallback) {
        return h -> {
            if (h.succeeded()) {
                log.info("Server {}[{}:{}] ready.", serverName, host, port);
                if (success != null) {
                    success.call(h.result());
                } else {
                    startFuture.complete();
                }
            } else {
                log.error("Failed to start server {}.", serverName, h.cause());
                if (fallback != null) {
                    fallback.call(h.cause());
                } else {
                    startFuture.fail(h.cause());
                }
            }

        };
    }

    public static String getHostName() {

        if (hostName == null) {
            try (ServerSocket socket = new ServerSocket(0)) {
                hostName = socket.getInetAddress().getHostName();
            } catch (Exception e) {
                log.error("Failed to get host name", e);
                hostName = DEFAULT_HOSTNAME;
            }
        }

        return hostName;
    }

    public static int pickUpRandomPort() {
        try (ServerSocket socket = new ServerSocket(0)) {
            return socket.getLocalPort();
        } catch (Exception e) {
            log.error("Failed to get random port", e);
            return RandomUtils.nextInt(10100, 13100);
        }
    }

    private static JsonObject config(Vertx vertx) {
        return vertx.getOrCreateContext().config();
    }

}
