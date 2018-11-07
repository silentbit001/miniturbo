package sb001.miniturbo.vertx.web;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.http.HttpServer;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.healthchecks.HealthCheckHandler;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.handler.FaviconHandler;
import io.vertx.ext.web.handler.StaticHandler;
import io.vertx.servicediscovery.ServiceDiscovery;
import lombok.extern.slf4j.Slf4j;
import sb001.vertx.VertxHttpServer;
import sb001.vertx.VertxProxy;

@Slf4j
public class TurboWebVerticle extends AbstractVerticle {

    public static final int SERVER_PORT = 8080;

    @Override
    public void start(Future<Void> startFuture) throws Exception {

        ServiceDiscovery discovery = ServiceDiscovery.create(vertx);
        HttpServer httpServer = vertx.createHttpServer();

        Router router = Router.router(vertx);

        // serve static files
        router.route("/js/*").handler(StaticHandler.create("web/js"));
        router.route("/css/*").handler(StaticHandler.create("web/css"));
        router.get("/").handler(ctxt -> ctxt.response().sendFile("web/index.html"));
        router.get("/favicon.ico").handler(FaviconHandler.create("web/favicon.ico"));
        router.get("/health").handler(HealthCheckHandler.create(vertx));

        // proxy to miniturbo-api
        router.route("/api/*")
                .handler(ctxt -> VertxProxy.serviceProxyUri(discovery, "miniturbo-api", ctxt.request(), "/api"));

        // create ws for status
        httpServer.websocketHandler(ws -> {
            if (ws.path().equals("/status")) {
                log.debug("Ws connected! {}", ws.localAddress());
                vertx.eventBus().consumer("miniturbo:update_status", h -> {
                    if (h.body() instanceof JsonObject) {
                        JsonObject json = (JsonObject) h.body();
                        ws.writeTextMessage(json.encodePrettily());
                    }
                });
            } else {
                log.debug("Ws connection rejected! {}", ws.localAddress());
                ws.reject();
            }
        });

        // start server
        VertxHttpServer.startServer(httpServer, "miniturbo-web", SERVER_PORT, router, startFuture);

    }

}
