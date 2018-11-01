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
import sb001.vertex.VertexServer;

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
        boolean publishService = config().getBoolean("resource.service.publish", Boolean.TRUE);
        ServiceDiscovery discovery = publishService ? ServiceDiscovery.create(vertx) : null;
        VertexServer.startServer(vertx, "miniturbo-resource", port, router, discovery, startFuture);

    }

    private RoutingContext getById(RoutingContext h) {

        String fileName = String.format("deployments/%s.yaml", h.pathParam("id"));
        vertx.fileSystem().readFile(fileName, f -> {
            if (f.succeeded()) {
                h.response().end(String.valueOf(f.result()));
            } else {
                h.response().setStatusCode(404).end();
            }
        });

        return h;
    }

    private RoutingContext findAll(RoutingContext h) {

        vertx.fileSystem().readDir("deployments/", d -> {

            if (d.succeeded()) {

                List<String> embeddedResources = d.result().stream().map(this::extractResourceNameFromFile)
                        .filter(StringUtils::isNotBlank).collect(Collectors.toList());

                h.response().end(new JsonArray(embeddedResources).encodePrettily());

            } else {
                h.response().setStatusCode(404).end();
            }

        });

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
