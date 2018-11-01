package sb001.miniturbo.vertex.api;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.ext.healthchecks.HealthCheckHandler;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.servicediscovery.ServiceDiscovery;
import sb001.vertex.VertexDiscovery;
import sb001.vertex.VertexServer;

public class TurboApiVerticle extends AbstractVerticle {

    public static final int SERVER_PORT = 8082;

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
        int port = config().getInteger("api.http.port", SERVER_PORT);
        Boolean publishService = config().getBoolean("resource.service.publish", Boolean.TRUE);
        VertexServer.startServer(vertx, "miniturbo-api", port, router, publishService ? discovery : null, startFuture);

    }

    private RoutingContext findAllResources(RoutingContext request) {
        VertexDiscovery.discoveryHttpClient(discovery, "miniturbo-resource", httpClient -> {
            httpClient.getNow("/", resourcesResponse -> {
                if (resourcesResponse.statusCode() == 200) {
                    resourcesResponse.bodyHandler(bodyHandler -> {
                        request.response().end(bodyHandler);
                    });
                } else {
                    request.response().setStatusCode(resourcesResponse.statusCode()).end();
                }
            });
        });
        return request;
    }

    private RoutingContext startResource(RoutingContext requestHandler) {
        vertx.eventBus().send("start_resource", requestHandler.pathParam("id"));
        requestHandler.response().end();
        return requestHandler;
    }

    private RoutingContext stopResource(RoutingContext requestHandler) {
        vertx.eventBus().send("stop_resource", requestHandler.pathParam("id"));
        requestHandler.response().end();
        return requestHandler;
    }

    private RoutingContext statusResource(RoutingContext request) {
        VertexDiscovery.discoveryHttpClient(discovery, "miniturbo-k8s", httpClient -> {
            httpClient.getNow(String.format("/%s/status", request.pathParam("id")), k8sResponse -> {
                if (k8sResponse.statusCode() == 200) {
                    k8sResponse.bodyHandler(bH -> request.response().end(bH));
                } else {
                    request.response().setStatusCode(k8sResponse.statusCode()).end();
                }
            });
        });
        return request;
    }

}
