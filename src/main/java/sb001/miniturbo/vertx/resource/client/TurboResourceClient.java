package sb001.miniturbo.vertx.resource.client;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpClientResponse;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.servicediscovery.ServiceDiscovery;
import rx.functions.Action1;
import sb001.vertx.VertxHttpClient;
import sb001.vertx.VertxProxy;

public final class TurboResourceClient {

    private static final String MINITURBO_RESOURCE = "miniturbo-resource";

    public static void proxy(ServiceDiscovery discovery, HttpServerRequest request) {
        VertxProxy.serviceProxyTo(discovery, MINITURBO_RESOURCE, request);
    }

    public static void existsResourceId(ServiceDiscovery discovery, String id, Action1<Boolean> success) {
        getAllResources(discovery, resources -> success.call(resources.contains(id)));
    }

    public static void getAllResources(ServiceDiscovery discovery, Action1<List<String>> success) {
        VertxHttpClient.get(discovery, MINITURBO_RESOURCE, buffer -> success
                .call(buffer.toJsonArray().stream().map(String::valueOf).collect(Collectors.toList())));
    }

    public static List<String> getAllResources(ServiceDiscovery discovery) {
        Optional<Buffer> buff = VertxHttpClient.getSync(discovery, MINITURBO_RESOURCE, "/");
        return buff.isPresent() ? buff.get().toJsonArray().stream().map(String::valueOf).collect(Collectors.toList())
                : Arrays.asList();
    }

    public static void getResourceById(ServiceDiscovery discovery, String id, Action1<String> success) {
        getResourceById(discovery, id, success, null);
    }

    public static void getResourceById(ServiceDiscovery discovery, String id, Action1<String> success,
            Action1<HttpClientResponse> failed) {
        VertxHttpClient.get(discovery, MINITURBO_RESOURCE, String.format("/%s", id),
                buffer -> success.call(buffer.toString()), error -> failed.call(error));
    }

}