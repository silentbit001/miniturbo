package sb001.miniturbo.vertex.web;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.handler.FaviconHandler;
import io.vertx.ext.web.handler.StaticHandler;
import io.vertx.servicediscovery.ServiceDiscovery;
import sb001.vertex.VertexProxy;
import sb001.vertex.VertexServer;

public class TurboWebVerticle extends AbstractVerticle {

    public static final int SERVER_PORT = 8080;

    @Override
    public void start(Future<Void> startFuture) throws Exception {

        ServiceDiscovery discovery = ServiceDiscovery.create(vertx);

        Router router = Router.router(vertx);

        // serve static files
        router.route("/js/*").handler(StaticHandler.create("web/js"));
        router.route("/css/*").handler(StaticHandler.create("web/css"));
        router.get("/").handler(ctxt -> ctxt.response().sendFile("web/index.html"));
        router.get("/favicon.ico").handler(FaviconHandler.create("web/favicon.ico"));

        // proxy to miniturbo-api
        router.route("/api/*")
                .handler(ctxt -> VertexProxy.serviceProxyUri(discovery, "miniturbo-api", ctxt.request(), "/api"));

        // start server
        VertexServer.startServer(vertx, "miniturbo-web", config().getInteger("web.http.port", SERVER_PORT), router,
                startFuture);

    }

}
