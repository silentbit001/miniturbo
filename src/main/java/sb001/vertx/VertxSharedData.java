package sb001.vertx;

import io.vertx.core.shareddata.AsyncMap;
import io.vertx.core.shareddata.SharedData;
import rx.functions.Action1;

public final class VertxSharedData {

    public static void putToSharedMap(SharedData sharedData, String mapName, String key, Object value) {
        putToSharedMap(sharedData, mapName, key, value, null);
    }

    public static void putToSharedMap(SharedData sharedData, String mapName, String key, Object value,
            Action1<Throwable> failed) {
        getSharedMap(sharedData, mapName, map -> map.put(key, value, rs -> {
            if (rs.failed() && failed != null) {
                failed.call(rs.cause());
            }
        }), failed);
    }

    public static void getFromSharedMap(SharedData sharedData, String mapName, String key, Action1<Object> success) {
        getFromSharedMap(sharedData, mapName, key, success, null);
    }

    public static void getFromSharedMap(SharedData sharedData, String mapName, String key, Action1<Object> success,
            Action1<Throwable> failed) {
        getSharedMap(sharedData, mapName, map -> map.get(key, r -> success.call(r)), failed);
    }

    public static void getSharedMap(SharedData sharedData, String mapName, Action1<AsyncMap<Object, Object>> success,
            Action1<Throwable> failed) {
        sharedData.getClusterWideMap(mapName, map -> {
            if (map.succeeded()) {
                success.call(map.result());
            } else if (failed != null) {
                failed.call(map.cause());
            }
        });
    }

}
