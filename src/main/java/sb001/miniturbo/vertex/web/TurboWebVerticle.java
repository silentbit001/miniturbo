package sb001.miniturbo.vertex.web;

import java.util.Objects;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.http.HttpClient;
import io.vertx.core.http.HttpClientRequest;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.handler.FaviconHandler;
import io.vertx.ext.web.handler.StaticHandler;
import io.vertx.rx.java.ObservableFuture;
import io.vertx.rx.java.RxHelper;
import io.vertx.servicediscovery.Record;
import io.vertx.servicediscovery.ServiceDiscovery;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class TurboWebVerticle extends AbstractVerticle {

    private static final int SERVER_PORT = 8080;

    @Override
    public void start(Future<Void> startFuture) throws Exception {

        ServiceDiscovery discovery = ServiceDiscovery.create(vertx);

        Router router = Router.router(vertx);

        router.route("/js/*").handler(StaticHandler.create("web/js"));
        router.route("/css/*").handler(StaticHandler.create("web/css"));
        router.get("/").handler(req -> req.response().sendFile("web/index.html"));
        router.get("/favicon.ico").handler(FaviconHandler.create("web/favicon.ico"));

        router.route("/api/*").handler(req -> {

            ObservableFuture<Record> discoveryObservable = RxHelper.observableFuture();
            discoveryObservable.filter(Objects::nonNull).map(record -> discovery.getReference(record))
                    .map(reference -> reference.getAs(HttpClient.class))
                    .subscribe(httpClient -> proxy(httpClient, req.request(), req.request().uri().split("/api")[1]));

            discovery.getRecord(r -> r.getName().equals("miniturbo-api"), discoveryObservable.toHandler());

        });

        // start server
        int port = config().getInteger("web.http.port", SERVER_PORT);
        vertx.createHttpServer().requestHandler(router::accept).listen(port, lHand -> {
            if (lHand.succeeded()) {
                log.info("Miniturbo web server ready.");
            }
        });

    }

    private void proxy(HttpClient redirectClient, HttpServerRequest originalRequest, String uri) {

        final HttpClientRequest redireactRequest = redirectClient.request(originalRequest.method(), uri, response -> {
            log.debug("Proxying response: {}", response.statusCode());
            originalRequest.response().setStatusCode(response.statusCode());
            originalRequest.response().headers().addAll(response.headers());
            originalRequest.response().setChunked(true);
            response.handler(h -> originalRequest.response().write(h));
            response.endHandler(h -> originalRequest.response().end());
        });

        redireactRequest.headers().addAll(originalRequest.headers());
        redireactRequest.setChunked(true);
        originalRequest.handler(data -> redireactRequest.write(data));
        originalRequest.endHandler(h -> redireactRequest.end());

    }
}
