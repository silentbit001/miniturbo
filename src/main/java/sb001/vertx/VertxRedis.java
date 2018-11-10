package sb001.vertx;

import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.redis.RedisClient;
import io.vertx.redis.RedisOptions;

public class VertxRedis {

    private static RedisClient redisClient;

    public static RedisClient redisClient(Vertx vertx) {

        JsonObject config = vertx.getOrCreateContext().config();

        if (redisClient == null && redisIsEnabled(config)) {

            String host = config.getString("redis.host", "localhost");
            int port = config.getInteger("redis.port", 6379);

            redisClient = RedisClient.create(vertx, new RedisOptions().setHost(host).setPort(port));
        }

        return redisClient;

    }

    public static boolean redisIsEnabled(JsonObject config) {
        return config.getBoolean("redis.enabled", Boolean.FALSE);
    }

}
