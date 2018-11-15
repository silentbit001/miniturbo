package sb001.miniturbo.vertx.k8s.client;

import io.vertx.core.http.HttpServerRequest;
import io.vertx.servicediscovery.ServiceDiscovery;
import rx.functions.Action1;
import sb001.vertx.VertxHttpClient;
import sb001.vertx.VertxProxy;

public class TurboK8sClient {

    private static final String MINITURBO_K8S = "miniturbo-k8s";

    /**
     * Get resource status proxy
     * 
     * @param discovery
     * @param id
     * @param request
     */
    public static void getStatusProxy(ServiceDiscovery discovery, String id, HttpServerRequest request) {
        VertxProxy.serviceProxyTo(discovery, MINITURBO_K8S, request, String.format("/%s/status", id));
    }

    public static void getStatus(ServiceDiscovery discovery, String id, Action1<String> success) {
        VertxHttpClient.get(discovery, MINITURBO_K8S, String.format("/%s/status", id),
                buffer -> success.call(buffer.toString()));
    }

    public static void start(ServiceDiscovery discovery, String id) {
        VertxHttpClient.post(discovery, MINITURBO_K8S, String.format("/%s/start", id));
    }

    public static void stop(ServiceDiscovery discovery, String id) {
        VertxHttpClient.post(discovery, MINITURBO_K8S, String.format("/%s/stop", id));
    }

}
