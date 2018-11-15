package sb001.vertx;

import io.vertx.core.Future;
import io.vertx.core.http.HttpClient;
import io.vertx.core.json.JsonObject;
import io.vertx.reactivex.servicediscovery.types.RedisDataSource;
import io.vertx.servicediscovery.Record;
import io.vertx.servicediscovery.ServiceDiscovery;
import io.vertx.servicediscovery.types.HttpEndpoint;
import lombok.extern.slf4j.Slf4j;
import rx.functions.Action1;

@Slf4j
public class VertxDiscovery {

    public static void discoveryHttpClient(ServiceDiscovery discovery, String serviceName,
            Action1<HttpClient> callback) {
        discoveryHttpClient(discovery, serviceName, callback, null);
    }

    public static void discoveryHttpClient(ServiceDiscovery discovery, String serviceName, Action1<HttpClient> callback,
            Action1<Throwable> fail) {

        // TODO: we need to handle timeout and resource not found
        discovery.getRecord(record -> record.getName().equals(serviceName), h -> {

            if (h.succeeded() && h.result() != null) {

                try {

                    HttpClient httpClient = discovery.getReference(h.result()).getAs(HttpClient.class);
                    callback.call(httpClient);

                } catch (Exception e) {
                    if (fail != null) {
                        fail.call(e);
                    }
                }

            } else if (fail != null) {
                fail.call(new RuntimeException("Service not found"));
            }

        });

    }

    public static void publishHttpService(ServiceDiscovery discovery, String serviceName, String host, int port,
            String path, Future<Void> startFuture) {
        publishService(discovery, HttpEndpoint.createRecord(serviceName, host, port, path), startFuture);
    }

    public static void publishRedisService(ServiceDiscovery discovery, String serviceName, String host, int port,
            Future<Void> startFuture) {
        publishService(discovery, RedisDataSource.createRecord(serviceName,
                new JsonObject().put("url", host).put("port", port), new JsonObject()), startFuture);
    }

    public static void publishService(ServiceDiscovery discovery, Record record, Future<Void> startFuture) {
        discovery.publish(record, h -> {
            if (h.succeeded()) {
                if (startFuture != null) {
                    startFuture.complete();
                }
                log.info("Service {} published", record.getName());
            } else {
                if (startFuture != null) {
                    startFuture.fail(h.cause());
                }
                log.error("Failed to publish service {}", record.getName(), h.cause());
            }
        });
    }

}
