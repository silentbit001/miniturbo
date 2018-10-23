package sb001.miniturbo.vertex.k8s;

import java.util.Objects;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.http.HttpClient;
import io.vertx.core.json.Json;
import io.vertx.ext.web.Router;
import io.vertx.rx.java.ObservableFuture;
import io.vertx.rx.java.RxHelper;
import io.vertx.servicediscovery.Record;
import io.vertx.servicediscovery.ServiceDiscovery;
import io.vertx.servicediscovery.types.HttpEndpoint;
import lombok.extern.slf4j.Slf4j;
import sb001.miniturbo.vertex.k8s.service.K8sService;
import sb001.miniturbo.vertex.k8s.service.dto.Status;

@Slf4j
public class TurboK8sVerticle extends AbstractVerticle {

    private K8sService k8sService;
    public static final int SERVER_PORT = 8083;

    @Override
    public void start(Future<Void> startFuture) throws Exception {

        Router router = Router.router(vertx);

        // Obtain service discovery
        ServiceDiscovery discovery = ServiceDiscovery.create(vertx, dH -> {

            // start server
            int port = config().getInteger("resource.http.port", SERVER_PORT);
            vertx.createHttpServer().requestHandler(router::accept).listen(port, lHand -> {
                if (lHand.succeeded()) {
                    log.info("K8s server is ready.");
                    k8sService = new K8sService();
                    if (config().getBoolean("resource.service.publish", Boolean.TRUE)) {
                        dH.publish(HttpEndpoint.createRecord("miniturbo-k8s", "localhost", SERVER_PORT, "/"),
                                sdHandler -> {
                                    if (sdHandler.succeeded()) {
                                        log.info("Service {} published", sdHandler.result().getName());
                                    }
                                });
                    }
                }

            });

        });

        router.get("/:id/status").handler(h -> {

            ObservableFuture<Record> discoveryObservable = RxHelper.observableFuture();
            discoveryObservable.filter(Objects::nonNull).map(record -> discovery.getReference(record))
                    .map(reference -> reference.getAs(HttpClient.class)).subscribe(rHC -> {

                        String resourceId = h.pathParam("id");
                        log.info("Status k8s resource '{}'....", resourceId);
                        rHC.getNow(String.format("/%s", resourceId), rH -> {

                            if (rH.statusCode() == 200) {

                                rH.bodyHandler(bodyHandler -> {

                                    Status status = k8sService.status(bodyHandler.toString());
                                    h.response().end(Json.encodePrettily(status));

                                });

                            } else {
                                log.error("Deployment failed: {}", rH.resume());
                                h.response().setStatusCode(500).end();
                            }

                        });

                    });

            discovery.getRecord(r -> r.getName().equals("miniturbo-resource"), discoveryObservable.toHandler());

        });

        vertx.eventBus().consumer("start_resource", handler -> {

            String id = String.valueOf(handler.body());

            ObservableFuture<Record> resourceServiceObsv = RxHelper.observableFuture();
            resourceServiceObsv.filter(Objects::nonNull)
                    .map(record -> (HttpClient) discovery.getReference(record).getAs(HttpClient.class))
                    .subscribe(httpClient -> deploy(httpClient, id));

            discovery.getRecord(r -> r.getName().equals("miniturbo-resource"), resourceServiceObsv.toHandler());

        });

        vertx.eventBus().consumer("stop_resource", handler -> {

            String id = String.valueOf(handler.body());

            ObservableFuture<Record> resourceServiceObsv = RxHelper.observableFuture();
            resourceServiceObsv.filter(Objects::nonNull)
                    .map(record -> (HttpClient) discovery.getReference(record).getAs(HttpClient.class))
                    .subscribe(httpClient -> unDeploy(httpClient, id));

            discovery.getRecord(r -> r.getName().equals("miniturbo-resource"), resourceServiceObsv.toHandler());

        });

    }

    private void deploy(HttpClient httpClient, String resourceId) {

        log.info("Starting k8s resource '{}'....", resourceId);
        httpClient.getNow(String.format("/%s", resourceId), rHandler -> {

            if (rHandler.statusCode() == 200) {

                rHandler.bodyHandler(bodyHandler -> {
                    k8sService.deployYamlDocuments(bodyHandler.toString());
                });

            } else {
                log.error("Deployment failed: {}", rHandler.resume());
            }
        });

    }

    private void unDeploy(HttpClient httpClient, String resourceId) {

        log.info("Stoping k8s resource '{}'....", resourceId);
        httpClient.getNow(String.format("/%s", resourceId), rHandler -> {

            if (rHandler.statusCode() == 200) {

                rHandler.bodyHandler(bodyHandler -> {
                    k8sService.unDeployYamlDocuments(bodyHandler.toString());
                });

            } else {
                log.error("Deployment failed: {}", rHandler.resume());
            }
        });

    }

}
