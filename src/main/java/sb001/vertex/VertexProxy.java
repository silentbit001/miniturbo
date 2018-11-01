package sb001.vertex;

import io.vertx.core.http.HttpClient;
import io.vertx.core.http.HttpClientRequest;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.servicediscovery.ServiceDiscovery;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class VertexProxy {

    public static void proxy(HttpClient httpClient, HttpServerRequest request, String afterUri) {

        final String uri = request.uri().split(afterUri)[1];

        final HttpClientRequest forwardRequest = httpClient.request(request.method(), uri, response -> {
            log.debug("Response status: {}", response.statusCode());
            request.response().setStatusCode(response.statusCode());
            request.response().headers().addAll(response.headers());
            request.response().setChunked(true);
            response.handler(h -> request.response().write(h));
            response.endHandler(h -> request.response().end());
        });

        forwardRequest.headers().addAll(request.headers());
        forwardRequest.setChunked(true);
        request.handler(data -> forwardRequest.write(data));
        request.endHandler(h -> forwardRequest.end());

    }

    public static void serviceProxy(ServiceDiscovery discovery, String serviceName, HttpServerRequest request,
            String afterUri) {
        VertexDiscovery.discoveryHttpClient(discovery, serviceName, httpClient -> proxy(httpClient, request, afterUri));
    }

}
