package sb001.vertx;

import java.util.Optional;

import co.paralleluniverse.strands.Strand;
import io.vertx.core.Future;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpClient;
import io.vertx.core.http.HttpClientResponse;
import io.vertx.servicediscovery.ServiceDiscovery;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import rx.functions.Action1;

@Slf4j
public class VertxHttpClient {

    private static final int DEFAULT_SYNC_TIMEOUT_IN_MILLIS = 5000;
    private static final int DEFAULT_HTTP_SUCCESS_STATUS = 200;

    /**
     * Http get fire and forget
     * 
     * @param discovery   Service discovery
     * @param serviceName service name
     * @param uri
     */
    public static void get(ServiceDiscovery discovery, String serviceName, String uri) {
        get(discovery, serviceName, uri, null, null);
    }

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
        httpClient.get(path, response -> {
            if (response.statusCode() == successStatus && success != null) {
                response.bodyHandler(body -> success.call(body));
            } else if (failed != null) {
                failed.call(response);
            }
        }).end();
    }

    public static Optional<Buffer> getSync(ServiceDiscovery discovery, String serviceName, String path) {
        return getSync(discovery, serviceName, path, DEFAULT_SYNC_TIMEOUT_IN_MILLIS);
    }

    /**
     * Make sync get request to service
     * 
     * @param discovery       Service discovery
     * @param serviceName     service name
     * @param uri
     * @param timeoutInMillis
     * @return
     */
    @SneakyThrows
    public static Optional<Buffer> getSync(ServiceDiscovery discovery, String serviceName, String uri,
            long timeoutInMillis) {

        long start = System.currentTimeMillis();
        Future<Buffer> result = Future.future();

        get(discovery, serviceName, uri, buffer -> {
            result.complete(buffer);
        }, response -> {
            result.fail(new RuntimeException());
        });

        while (!result.isComplete() && System.currentTimeMillis() - start < timeoutInMillis) {
            Strand.sleep(100);
        }

        if (!result.isComplete()) {
            log.warn("Request look isn't complete! Timeout of {}ms has been reach.", timeoutInMillis);
        }

        return result.failed() ? Optional.empty() : Optional.of(result.result());
    }
}
