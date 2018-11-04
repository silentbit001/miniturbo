package sb001.miniturbo.vertx.resource;

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
import sb001.vertx.VertxHttpServer;

public class TurboResourceVerticle extends AbstractVerticle {

    private static List<String> embeddedResources;
    private Pattern deploymentPattern = Pattern.compile("(?<=/deployments/)(.*)(?=.yaml)");

    @Override
    public void start(Future<Void> startFuture) throws Exception {

        Router router = Router.router(vertx);
        router.get("/").handler(this::findAll);
        router.get("/:id").handler(this::getById);

        // start server
        VertxHttpServer.startServer(vertx, "miniturbo-resource", router, ServiceDiscovery.create(vertx), startFuture);

    }

    private RoutingContext getById(RoutingContext h) {

        String fileName = String.format("deployments/%s.yaml", h.pathParam("id"));
        vertx.fileSystem().readFile(fileName, f -> {
            if (f.succeeded()) {
                h.response().setChunked(true).end(f.result());
            } else {
                h.response().setStatusCode(404).end();
            }
        });

        return h;
    }

    private RoutingContext findAll(RoutingContext h) {

        if (embeddedResources != null) {
            h.response().end(new JsonArray(embeddedResources).encodePrettily());
        } else {

            vertx.fileSystem().readDir("deployments/", d -> {

                if (d.succeeded()) {

                    embeddedResources = d.result().stream().map(this::extractResourceNameFromFile)
                            .filter(StringUtils::isNotBlank).collect(Collectors.toList());

                    h.response().end(new JsonArray(embeddedResources).encodePrettily());

                } else {
                    h.response().setStatusCode(404).end();
                }

            });
        }

        return h;
    }

    private String extractResourceNameFromFile(String url) {
        Matcher matcher = deploymentPattern.matcher(url);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return null;
    }

}
