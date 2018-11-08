package sb001.vertx;

import io.vertx.core.http.HttpClient;
import io.vertx.core.http.HttpClientRequest;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.servicediscovery.ServiceDiscovery;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class VertxProxy {

    public static void proxyUri(HttpClient httpClient, HttpServerRequest request, String afterUri) {
        final String uri = request.uri().split(afterUri)[1];
        proxyTo(httpClient, request, uri);
    }

    public static void serviceProxyUri(ServiceDiscovery discovery, String serviceName, HttpServerRequest request,
            String afterUri) {
        VertxDiscovery.discoveryHttpClient(discovery, serviceName,
                httpClient -> proxyUri(httpClient, request, afterUri));
    }

    public static void serviceProxyTo(ServiceDiscovery discovery, String serviceName, HttpServerRequest request) {
        serviceProxyTo(discovery, serviceName, request, "/");
    }

    public static void serviceProxyTo(ServiceDiscovery discovery, String serviceName, HttpServerRequest request,
            String uri) {
        VertxDiscovery.discoveryHttpClient(discovery, serviceName, httpClient -> proxyTo(httpClient, request, uri));
    }

    public static void proxyTo(HttpClient httpClient, HttpServerRequest request, final String uri) {

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

}
