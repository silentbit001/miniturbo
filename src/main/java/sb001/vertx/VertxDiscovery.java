package sb001.vertx;

import java.util.Objects;

import io.vertx.core.Future;
import io.vertx.core.http.HttpClient;
import io.vertx.rx.java.ObservableFuture;
import io.vertx.rx.java.RxHelper;
import io.vertx.servicediscovery.Record;
import io.vertx.servicediscovery.ServiceDiscovery;
import io.vertx.servicediscovery.types.HttpEndpoint;
import lombok.extern.slf4j.Slf4j;
import rx.functions.Action1;

@Slf4j
public class VertxDiscovery {

    public static void discoveryHttpClient(ServiceDiscovery discovery, String serviceName,
            Action1<HttpClient> callback) {

        // TODO: we need to handle timeout and resource not found

        ObservableFuture<Record> discoveryObservable = RxHelper.observableFuture();

        discoveryObservable.filter(Objects::nonNull).map(record -> discovery.getReference(record))
                .map(reference -> reference.getAs(HttpClient.class)).subscribe(callback);

        discovery.getRecord(r -> r.getName().equals(serviceName), discoveryObservable.toHandler());

    }

    public static void publishHttpService(ServiceDiscovery discovery, String serviceName, String host, int port,
            String path, Future<Void> startFuture) {
        discovery.publish(HttpEndpoint.createRecord(serviceName, host, port, path), h -> {
            if (h.succeeded()) {
                if (startFuture != null) {
                    startFuture.complete();
                }
                log.info("Service {} published", serviceName);
            } else {
                if (startFuture != null) {
                    startFuture.fail(h.cause());
                }
                log.error("Failed to publish service {}", serviceName, h.cause());
            }
        });
    }

}
