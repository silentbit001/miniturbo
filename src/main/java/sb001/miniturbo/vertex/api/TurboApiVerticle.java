package sb001.miniturbo.vertex.api;

import java.util.Objects;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.http.HttpClient;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.rx.java.ObservableFuture;
import io.vertx.rx.java.RxHelper;
import io.vertx.servicediscovery.Record;
import io.vertx.servicediscovery.ServiceDiscovery;
import io.vertx.servicediscovery.types.HttpEndpoint;
import lombok.extern.slf4j.Slf4j;

@Slf4j
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

        // start server
        int port = config().getInteger("api.http.port", SERVER_PORT);
        vertx.createHttpServer().requestHandler(router::accept).listen(port, lH -> {
            if (lH.succeeded()) {
                log.info("TurboApi server ready.");
                startFuture.complete();
                if (config().getBoolean("resource.service.publish", Boolean.TRUE)) {
                    discovery.publish(HttpEndpoint.createRecord("miniturbo-api", "localhost", SERVER_PORT, "/"),
                            sdHandler -> {
                                if (sdHandler.succeeded()) {
                                    log.info("Service {} published", sdHandler.result().getName());
                                }
                            });
                }

            } else {
                startFuture.fail(lH.cause());
            }
        });

    }

    private RoutingContext findAllResources(RoutingContext requestHandler) {

        ObservableFuture<Record> discoveryObservable = RxHelper.observableFuture();
        discoveryObservable.filter(Objects::nonNull).map(record -> discovery.getReference(record))
                .map(reference -> reference.getAs(HttpClient.class)).subscribe(httpClient -> {

                    httpClient.getNow("/", res -> {

                        if (res.statusCode() == 200) {

                            res.bodyHandler(bodyHandler -> {
                                requestHandler.response().end(bodyHandler);
                            });

                        } else {
                            requestHandler.response().setStatusCode(404).end();
                        }

                    });

                });

        discovery.getRecord(r -> r.getName().equals("miniturbo-resource"), discoveryObservable.toHandler());

        return requestHandler;
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

    private RoutingContext statusResource(RoutingContext rH) {

        ObservableFuture<Record> discoveryObservable = RxHelper.observableFuture();
        discoveryObservable.filter(Objects::nonNull).map(record -> discovery.getReference(record))
                .map(reference -> reference.getAs(HttpClient.class)).subscribe(hC -> {

                    hC.getNow(String.format("/%s/status", rH.pathParam("id")), kH -> {

                        if (kH.statusCode() == 200) {
                            kH.bodyHandler(bH -> rH.response().end(bH));
                        } else {
                            rH.response().setStatusCode(404).end();
                        }

                    });

                });

        discovery.getRecord(r -> r.getName().equals("miniturbo-k8s"), discoveryObservable.toHandler());

        return rH;
    }

}
