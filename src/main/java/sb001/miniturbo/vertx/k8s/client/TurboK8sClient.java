package sb001.miniturbo.vertx.k8s.client;

import io.vertx.core.http.HttpServerRequest;
import io.vertx.servicediscovery.ServiceDiscovery;
import rx.functions.Action1;
import sb001.vertx.VertxHttpClient;
import sb001.vertx.VertxProxy;

public class TurboK8sClient {

    private static final String MINITURBO_K8S = "miniturbo-k8s";

    public static void getStatusProxy(ServiceDiscovery discovery, String id, HttpServerRequest request) {
        String uri = String.format("/%s/status", id);
        VertxProxy.serviceProxyTo(discovery, MINITURBO_K8S, request, uri);
    }

    public static void getStatus(ServiceDiscovery discovery, String id, Action1<String> success) {
        String uri = String.format("/%s/status", id);
        VertxHttpClient.get(discovery, MINITURBO_K8S, uri, buffer -> success.call(buffer.toString()));
    }

}
