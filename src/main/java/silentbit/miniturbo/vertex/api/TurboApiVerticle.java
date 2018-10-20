package silentbit.miniturbo.vertex.api;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.servicediscovery.ServiceDiscovery;
import io.vertx.servicediscovery.types.HttpEndpoint;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class TurboApiVerticle extends AbstractVerticle {

    private static final int SERVER_PORT = 8080;

    @Override
    public void start(Future<Void> startFuture) throws Exception {

        Router router = Router.router(vertx);
        router.post("/resource/:id/start").handler(this::startResource);
        router.post("/resource/:id/stop").handler(this::stopResource);

        // start server
        int port = config().getInteger("api.http.port", SERVER_PORT);
        vertx.createHttpServer().requestHandler(router::accept).listen(port, lHand -> {
            if (lHand.succeeded()) {
                log.info("TurboApi server ready.");

                if (config().getBoolean("resource.service.publish", Boolean.TRUE)) {
                    ServiceDiscovery.create(vertx).publish(
                            HttpEndpoint.createRecord("miniturbo-api", "localhost", SERVER_PORT, "/"), sdHandler -> {
                                if (sdHandler.succeeded()) {
                                    log.info("Service {} published", sdHandler.result().getName());
                                }
                            });
                }

            }
        });

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

}
