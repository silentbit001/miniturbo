package sb001.miniturbo.vertx.api;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.ext.healthchecks.HealthCheckHandler;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.servicediscovery.ServiceDiscovery;
import sb001.miniturbo.vertx.api.dto.Status;
import sb001.miniturbo.vertx.k8s.client.TurboK8sClient;
import sb001.miniturbo.vertx.resource.client.TurboResourceClient;
import sb001.vertx.VertxHttpServer;
import sb001.vertx.VertxRedisEvent;
import sb001.vertx.VertxSharedData;

public class TurboApiVerticle extends AbstractVerticle {

    private static final String MINITURBO_API = "miniturbo-api";

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
        router.get("/resource/status").handler(this::status);
        router.get("/health").handler(HealthCheckHandler.create(vertx));

        // start server
        VertxHttpServer.startServer(vertx, MINITURBO_API, router, discovery, startFuture);

    }

    private RoutingContext status(RoutingContext request) {
        
        
        // get from cache
        
        

        TurboResourceClient.getAllResources(discovery, resources -> {

            VertxSharedData.getFromSharedMap(vertx.sharedData(), "miniturbo", "resource_list", list -> {
                if (list == null || !resources.equals(list)) {

                    // notify changed resources
                    VertxRedisEvent.publish(vertx, "update_status",
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
        TurboResourceClient.proxy(discovery, request.request());
        return request;
    }

    private RoutingContext startResource(RoutingContext requestHandler) {
        TurboK8sClient.start(discovery, requestHandler.pathParam("id"));
        requestHandler.response().setStatusCode(202).end();
        return requestHandler;
    }

    private RoutingContext stopResource(RoutingContext requestHandler) {
        TurboK8sClient.stop(discovery, requestHandler.pathParam("id"));
        requestHandler.response().setStatusCode(202).end();
        return requestHandler;
    }

    private RoutingContext statusResource(RoutingContext request) {
        TurboK8sClient.getStatusProxy(discovery, request.pathParam("id"), request.request());
        return request;
    }

}
