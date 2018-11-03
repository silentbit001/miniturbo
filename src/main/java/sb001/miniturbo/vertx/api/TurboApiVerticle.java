package sb001.miniturbo.vertx.api;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.ext.healthchecks.HealthCheckHandler;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.servicediscovery.ServiceDiscovery;
import sb001.vertx.VertxHttpServer;
import sb001.vertx.VertxProxy;

public class TurboApiVerticle extends AbstractVerticle {

    private ServiceDiscovery discovery;

    @Override
    public void start(Future<Void> startFuture) throws Exception {

        discovery = ServiceDiscovery.create(vertx);

        Router router = Router.router(vertx);
        router.get("/resource").handler(this::findAllResources);
        router.post("/resource/:id/start").handler(this::startResource);
        router.post("/resource/:id/stop").handler(this::stopResource);
        router.get("/resource/:id/status").handler(this::statusResource);
        router.get("/health").handler(HealthCheckHandler.create(vertx));
        router.get("/info").handler(h -> {
            vertx.fileSystem().readFile("META-INF/MANIFEST.MF", fH -> {
                h.response().end(fH.result());
            });
        });

        // start server
        VertxHttpServer.startServer(vertx, "miniturbo-api", router, discovery, startFuture);

    }

    private RoutingContext findAllResources(RoutingContext request) {
        VertxProxy.serviceProxyTo(discovery, "miniturbo-resource", request.request(), "/");
        return request;
    }

    private RoutingContext startResource(RoutingContext requestHandler) {
        vertx.eventBus().send("start_resource", requestHandler.pathParam("id"));
        requestHandler.response().setStatusCode(202).end();
        return requestHandler;
    }

    private RoutingContext stopResource(RoutingContext requestHandler) {
        vertx.eventBus().send("stop_resource", requestHandler.pathParam("id"));
        requestHandler.response().setStatusCode(202).end();
        return requestHandler;
    }

    private RoutingContext statusResource(RoutingContext request) {
        String uri = String.format("/%s/status", request.pathParam("id"));
        VertxProxy.serviceProxyTo(discovery, "miniturbo-k8s", request.request(), uri);
        return request;
    }

}
