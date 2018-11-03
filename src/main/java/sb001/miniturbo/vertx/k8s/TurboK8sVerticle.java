package sb001.miniturbo.vertx.k8s;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.http.HttpClient;
import io.vertx.core.json.Json;
import io.vertx.ext.web.Router;
import io.vertx.servicediscovery.ServiceDiscovery;
import lombok.extern.slf4j.Slf4j;
import sb001.miniturbo.vertx.k8s.service.K8sService;
import sb001.miniturbo.vertx.k8s.service.dto.Status;
import sb001.vertx.VertxDiscovery;
import sb001.vertx.VertxHttpClient;
import sb001.vertx.VertxHttpServer;

@Slf4j
public class TurboK8sVerticle extends AbstractVerticle {

    private K8sService k8sService;

    @Override
    public void start(Future<Void> startFuture) throws Exception {

        // Obtain service discovery
        ServiceDiscovery discovery = ServiceDiscovery.create(vertx);

        Router router = Router.router(vertx);

        router.get("/:id/status").handler(request -> {

            String resourceId = request.pathParam("id");
            log.info("Status k8s resource '{}'....", resourceId);
            String path = String.format("/%s", resourceId);

            VertxHttpClient.get(discovery, "miniturbo-resource", path, buffer -> {

                Status status = k8sService.status(buffer.toString());
                request.response().end(Json.encodePrettily(status));

            }, response -> {
                log.error("Deployment failed: {}", response.resume());
                request.response().setStatusCode(500).end();
            });

        });

        vertx.eventBus().consumer("start_resource", handler -> {
            String id = String.valueOf(handler.body());
            VertxDiscovery.discoveryHttpClient(discovery, "miniturbo-resource", httpClient -> deploy(httpClient, id));
        });

        vertx.eventBus().consumer("stop_resource", handler -> {

            String id = String.valueOf(handler.body());
            VertxDiscovery.discoveryHttpClient(discovery, "miniturbo-resource", httpClient -> unDeploy(httpClient, id));

        });

        // start server
        VertxHttpServer.startServer(vertx, "miniturbo-k8s", router, discovery, startFuture);

    }

    private void deploy(HttpClient httpClient, String resourceId) {
        log.info("Starting k8s resource '{}'....", resourceId);
        VertxHttpClient.get(httpClient, String.format("/%s", resourceId),
                buffer -> k8sService.deployYamlDocuments(buffer.toString()),
                response -> log.error("Deployment failed: {}", response.resume()));
    }

    private void unDeploy(HttpClient httpClient, String resourceId) {
        log.info("Stoping k8s resource '{}'....", resourceId);
        VertxHttpClient.get(httpClient, String.format("/%s", resourceId),
                buffer -> k8sService.unDeployYamlDocuments(buffer.toString()),
                response -> log.error("Deployment failed: {}", response.resume()));
    }

}
