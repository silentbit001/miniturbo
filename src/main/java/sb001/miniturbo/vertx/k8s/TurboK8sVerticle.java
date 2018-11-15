package sb001.miniturbo.vertx.k8s;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.json.Json;
import io.vertx.ext.web.Router;
import io.vertx.servicediscovery.ServiceDiscovery;
import lombok.extern.slf4j.Slf4j;
import sb001.miniturbo.vertx.k8s.service.K8sService;
import sb001.miniturbo.vertx.k8s.service.dto.DeploymentStatus;
import sb001.miniturbo.vertx.resource.client.TurboResourceClient;
import sb001.vertx.VertxHttpServer;

@Slf4j
public class TurboK8sVerticle extends AbstractVerticle {

    private K8sService k8sService;

    @Override
    public void start(Future<Void> startFuture) throws Exception {

        // Obtain service discovery
        ServiceDiscovery discovery = ServiceDiscovery.create(vertx);

        Router router = Router.router(vertx);

        // status by id
        router.get("/:id/status").handler(request -> {

            String resourceId = request.pathParam("id");
            log.info("Status k8s resource '{}'....", resourceId);

            TurboResourceClient.getResourceById(discovery, resourceId, resource -> {
                DeploymentStatus status = k8sService.status(resource);
                request.response().end(Json.encodePrettily(status));
            }, failed -> request.response().setStatusCode(500).end());

        });

        // start resource by id
        router.post("/:id/start").handler(h -> {
            TurboResourceClient.getResourceById(discovery, h.request().getParam("id"),
                    resource -> k8sService.deployYamlDocuments(resource));
            h.response().setStatusCode(202).end();
        });

        // stop resource by id
        router.post("/:id/stop").handler(h -> {
            TurboResourceClient.getResourceById(discovery, h.request().getParam("id"),
                    resource -> k8sService.unDeployYamlDocuments(resource));
            h.response().setStatusCode(202).end();
        });

        // start server
        VertxHttpServer.startServer(vertx, "miniturbo-k8s", router, discovery, startFuture);

    }

}
