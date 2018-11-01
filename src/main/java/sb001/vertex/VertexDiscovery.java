package sb001.vertex;

import java.util.Objects;

import io.vertx.core.http.HttpClient;
import io.vertx.rx.java.ObservableFuture;
import io.vertx.rx.java.RxHelper;
import io.vertx.servicediscovery.Record;
import io.vertx.servicediscovery.ServiceDiscovery;
import rx.functions.Action1;

public class VertexDiscovery {

    public static void discoveryHttpClient(ServiceDiscovery discovery, String serviceName,
            Action1<HttpClient> callback) {

        // TODO: we need to handle timeout and resource not found

        ObservableFuture<Record> discoveryObservable = RxHelper.observableFuture();

        discoveryObservable.filter(Objects::nonNull).map(record -> discovery.getReference(record))
                .map(reference -> reference.getAs(HttpClient.class)).subscribe(callback);

        discovery.getRecord(r -> r.getName().equals(serviceName), discoveryObservable.toHandler());

    }

}
