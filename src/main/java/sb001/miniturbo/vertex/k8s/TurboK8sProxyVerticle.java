package sb001.miniturbo.vertex.k8s;

import java.util.Objects;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.http.HttpClient;
import io.vertx.rx.java.ObservableFuture;
import io.vertx.rx.java.RxHelper;
import io.vertx.servicediscovery.Record;
import io.vertx.servicediscovery.ServiceDiscovery;
import lombok.extern.slf4j.Slf4j;
import sb001.miniturbo.vertex.k8s.service.K8sService;

@Slf4j
public class TurboK8sProxyVerticle extends AbstractVerticle {

    private K8sService k8sService;

    @Override
    public void start(Future<Void> startFuture) throws Exception {

        // Obtain service discovery
        ServiceDiscovery discovery = ServiceDiscovery.create(vertx, h -> {
            k8sService = new K8sService();
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
