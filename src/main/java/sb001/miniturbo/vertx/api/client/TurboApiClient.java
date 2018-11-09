package sb001.miniturbo.vertx.api.client;

import io.vertx.core.http.HttpServerRequest;
import io.vertx.servicediscovery.ServiceDiscovery;
import sb001.vertx.VertxProxy;

public class TurboApiClient {

    private static final String MINITURBO_API = "miniturbo-api";

    public static void proxy(ServiceDiscovery discovery, HttpServerRequest request) {
        VertxProxy.serviceProxyUri(discovery, MINITURBO_API, request, "/api");
    }

}
