package sb001.vertx;

import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpClient;
import io.vertx.core.http.HttpClientResponse;
import io.vertx.servicediscovery.ServiceDiscovery;
import rx.functions.Action1;

public class VertxHttpClient {

    private static final int DEFAULT_HTTP_SUCCESS_STATUS = 200;

    public static void get(ServiceDiscovery discovery, String serviceName, String path, Action1<Buffer> success) {
        get(discovery, serviceName, path, success, null);
    }

    public static void get(ServiceDiscovery discovery, String serviceName, String path, Action1<Buffer> success,
            Action1<HttpClientResponse> failed) {
        get(discovery, serviceName, path, DEFAULT_HTTP_SUCCESS_STATUS, success, failed);
    }

    public static void get(ServiceDiscovery discovery, String serviceName, String path, int successStatus,
            Action1<Buffer> success, Action1<HttpClientResponse> failed) {
        VertxDiscovery.discoveryHttpClient(discovery, serviceName,
                httpClient -> get(httpClient, path, successStatus, success, failed));
    }

    public static void get(HttpClient httpClient, String path, Action1<Buffer> success,
            Action1<HttpClientResponse> failed) {
        get(httpClient, path, DEFAULT_HTTP_SUCCESS_STATUS, success, failed);
    }

    public static void get(HttpClient httpClient, String path, int successStatus, Action1<Buffer> success,
            Action1<HttpClientResponse> failed) {
        httpClient.getNow(path, response -> {
            if (response.statusCode() == successStatus && success != null) {
                response.handler(body -> success.call(body));
            } else if (failed != null) {
                failed.call(response);
            }
        });
    }
}
