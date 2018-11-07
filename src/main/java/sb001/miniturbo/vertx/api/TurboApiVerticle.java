package sb001.miniturbo.vertx.api;

import java.util.List;
import java.util.stream.Collectors;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.ext.healthchecks.HealthCheckHandler;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.servicediscovery.ServiceDiscovery;
import sb001.miniturbo.vertx.api.dto.Status;
import sb001.vertx.VertxHttpClient;
import sb001.vertx.VertxHttpServer;
import sb001.vertx.VertxProxy;
import sb001.vertx.VertxSharedData;

public class TurboApiVerticle extends AbstractVerticle {

    private static final String MINITURBO_API = "miniturbo-api";
    private static final String MINITURBO_RESOURCE = "miniturbo-resource";

    private ServiceDiscovery discovery;

    @Override
    public void start(Future<Void> startFuture) throws Exception {

        discovery = ServiceDiscovery.create(vertx);

        // routers
        Router router = Router.router(vertx);
        router.get("/resource").handler(this::findAllResources);
        router.post("/resource/:id/start").handler(this::startResource);
        router.post("/resource/:id/stop").handler(this::stopResource);
        router.get("/resource/:id/status").handler(this::statusResource);
        router.get("/job").handler(this::job);
        router.get("/health").handler(HealthCheckHandler.create(vertx));

        // start server
        VertxHttpServer.startServer(vertx, MINITURBO_API, router, discovery, startFuture);

    }

    private RoutingContext job(RoutingContext request) {
        VertxHttpClient.get(discovery, MINITURBO_RESOURCE, "/", rb -> {

            // parse resources
            List<String> resources = rb.toJsonArray().stream().map(String::valueOf).collect(Collectors.toList());

            VertxSharedData.getFromSharedMap(vertx.sharedData(), "miniturbo", "resource_list", list -> {
                if (list == null || !resources.equals(list)) {

                    // notify changed resources
                    vertx.eventBus().send("miniturbo:update_status",
                            Status.builder().resources(resources).build().toJson());

                    // update shared list
                    VertxSharedData.putToSharedMap(vertx.sharedData(), "miniturbo", "resource_list", resources);
                }
            });

        });

        // got it
        request.response().setStatusCode(202).end();
        return request;
    }

    private RoutingContext findAllResources(RoutingContext request) {
        VertxProxy.serviceProxyTo(discovery, MINITURBO_RESOURCE, request.request(), "/");
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
