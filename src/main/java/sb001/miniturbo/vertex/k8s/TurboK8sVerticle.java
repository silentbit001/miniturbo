package sb001.miniturbo.vertex.k8s;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.http.HttpClient;
import io.vertx.core.json.Json;
import io.vertx.ext.web.Router;
import io.vertx.servicediscovery.ServiceDiscovery;
import lombok.extern.slf4j.Slf4j;
import sb001.miniturbo.vertex.k8s.service.K8sService;
import sb001.miniturbo.vertex.k8s.service.dto.Status;
import sb001.vertex.VertexDiscovery;
import sb001.vertex.VertexServer;

@Slf4j
public class TurboK8sVerticle extends AbstractVerticle {

    private K8sService k8sService;
    public static final int SERVER_PORT = 8083;

    @Override
    public void start(Future<Void> startFuture) throws Exception {

        // Obtain service discovery
        ServiceDiscovery discovery = ServiceDiscovery.create(vertx);

        Router router = Router.router(vertx);

        router.get("/:id/status").handler(request -> {
            VertexDiscovery.discoveryHttpClient(discovery, "miniturbo-resource", rHC -> {
                String resourceId = request.pathParam("id");
                log.info("Status k8s resource '{}'....", resourceId);
                rHC.getNow(String.format("/%s", resourceId), resourceResponse -> {

                    if (resourceResponse.statusCode() == 200) {

                        resourceResponse.bodyHandler(bodyHandler -> {

                            Status status = k8sService.status(bodyHandler.toString());
                            request.response().end(Json.encodePrettily(status));

                        });

                    } else {
                        log.error("Deployment failed: {}", resourceResponse.resume());
                        request.response().setStatusCode(500).end();
                    }

                });
            });

        });

        vertx.eventBus().consumer("start_resource", handler -> {
            String id = String.valueOf(handler.body());
            VertexDiscovery.discoveryHttpClient(discovery, "miniturbo-resource", httpClient -> deploy(httpClient, id));
        });

        vertx.eventBus().consumer("stop_resource", handler -> {

            String id = String.valueOf(handler.body());
            VertexDiscovery.discoveryHttpClient(discovery, "miniturbo-resource", httpClient -> unDeploy(httpClient, id));

        });

        // start server
        int port = config().getInteger("resource.http.port", SERVER_PORT);
        boolean publishService = config().getBoolean("resource.service.publish", Boolean.TRUE);
        VertexServer.startServer(vertx, "miniturbo-k8s", port, router, publishService ? discovery : null, startFuture);

    }

    private void deploy(HttpClient httpClient, String resourceId) {

        log.info("Starting k8s resource '{}'....", resourceId);
        httpClient.getNow(String.format("/%s", resourceId), resourceResponse -> {
            if (resourceResponse.statusCode() == 200) {
                resourceResponse.bodyHandler(bodyHandler -> {
                    k8sService.deployYamlDocuments(bodyHandler.toString());
                });

            } else {
                log.error("Deployment failed: {}", resourceResponse.resume());
            }
        });

    }

    private void unDeploy(HttpClient httpClient, String resourceId) {

        log.info("Stoping k8s resource '{}'....", resourceId);
        httpClient.getNow(String.format("/%s", resourceId), resourceResponse -> {
            if (resourceResponse.statusCode() == 200) {
                resourceResponse.bodyHandler(bodyHandler -> {
                    k8sService.unDeployYamlDocuments(bodyHandler.toString());
                });
            } else {
                log.error("Deployment failed: {}", resourceResponse.resume());
            }
        });

    }

}
