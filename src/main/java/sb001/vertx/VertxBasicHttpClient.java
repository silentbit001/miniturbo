package sb001.vertx;

import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpClient;
import io.vertx.core.http.HttpClientRequest;
import io.vertx.core.http.HttpClientResponse;
import rx.functions.Action1;

public class VertxBasicHttpClient {

    public static void get(HttpClient httpClient, String uri, int successStatus, Action1<Buffer> success,
            Action1<HttpClientResponse> failed) {
        httpClient.get(uri, response -> {
            if (response.statusCode() == successStatus && success != null) {
                response.bodyHandler(body -> success.call(body));
            } else if (failed != null) {
                failed.call(response);
            }
        }).end();
    }

    public static void post(HttpClient httpClient, String uri, Buffer data, int successStatus, Action1<Buffer> success,
            Action1<HttpClientResponse> failed) {

        HttpClientRequest request = httpClient.post(uri, response -> {
            if (response.statusCode() == successStatus && success != null) {
                response.bodyHandler(body -> success.call(body));
            } else if (failed != null) {
                failed.call(response);
            }
        });

        if (data != null) {
            request.write(data);
        }

        request.end();

    }

    public static void put(HttpClient httpClient, String uri, Buffer data, int successStatus, Action1<Buffer> success,
            Action1<HttpClientResponse> failed) {

        HttpClientRequest request = httpClient.put(uri, response -> {
            if (response.statusCode() == successStatus && success != null) {
                response.bodyHandler(body -> success.call(body));
            } else if (failed != null) {
                failed.call(response);
            }
        });

        if (data != null) {
            request.write(data);
        }

        request.end();

    }

    public static void delete(HttpClient httpClient, String uri, Buffer data, int successStatus,
            Action1<Buffer> success, Action1<HttpClientResponse> failed) {

        HttpClientRequest request = httpClient.delete(uri, response -> {
            if (response.statusCode() == successStatus && success != null) {
                response.bodyHandler(body -> success.call(body));
            } else if (failed != null) {
                failed.call(response);
            }
        });

        if (data != null) {
            request.write(data);
        }

        request.end();

    }

}
