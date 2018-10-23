package sb001.miniturbo.vertex.resource;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.json.JsonArray;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.servicediscovery.ServiceDiscovery;
import io.vertx.servicediscovery.types.HttpEndpoint;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class TurboResourceVerticle extends AbstractVerticle {

    public static final int SERVER_PORT = 8081;
    private Pattern deploymentPattern = Pattern.compile("(?<=/deployments/)(.*)(?=.yaml)");

    @Override
    public void start(Future<Void> startFuture) throws Exception {

        Router router = Router.router(vertx);
        router.get("/").handler(this::findAll);
        router.get("/:id").handler(this::getById);

        // start server
        int port = config().getInteger("resource.http.port", SERVER_PORT);
        vertx.createHttpServer().requestHandler(router::accept).listen(port, lH -> {
            if (lH.succeeded()) {
                log.info("Resource server is ready.");
                if (config().getBoolean("resource.service.publish", Boolean.TRUE)) {
                    ServiceDiscovery.create(vertx).publish(
                            HttpEndpoint.createRecord("miniturbo-resource", "localhost", SERVER_PORT, "/"), h -> {
                                if (h.succeeded()) {
                                    log.info("Service {} published", h.result().getName());
                                }
                            });
                }
            }

        });

    }

    private RoutingContext getById(RoutingContext requestHandler) {

        String fileName = String.format("deployments/%s.yaml", requestHandler.pathParam("id"));
        vertx.fileSystem().readFile(fileName, fH -> {
            if (fH.succeeded()) {
                requestHandler.response().end(String.valueOf(fH.result()));
            } else {
                requestHandler.response().setStatusCode(404).end();
            }
        });

        return requestHandler;
    }

    private RoutingContext findAll(RoutingContext requestHandler) {

        vertx.fileSystem().readDir("deployments/", dirHandler -> {

            if (dirHandler.succeeded()) {

                List<String> embeddedResources = dirHandler.result().stream().map(this::extractResourceNameFromFile)
                        .filter(StringUtils::isNotBlank).collect(Collectors.toList());

                requestHandler.response().end(new JsonArray(embeddedResources).encodePrettily());

            } else {
                requestHandler.response().setStatusCode(404).end();
            }

        });

        return requestHandler;
    }

    private String extractResourceNameFromFile(String url) {

        Matcher matcher = deploymentPattern.matcher(url);

        if (matcher.find()) {
            return matcher.group(1);
        }

        return null;
    }

}
