package sb001.miniturbo.vertex.resource;

import java.io.IOException;
import java.net.ServerSocket;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import io.vertx.core.DeploymentOptions;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;

@RunWith(VertxUnitRunner.class)
public class TestTurboResourceVerticleLegacy {

    private Integer port;
    private Vertx vertx;

    @Before
    public void setUp(TestContext context) throws IOException {
        vertx = Vertx.vertx();
        ServerSocket socket = new ServerSocket(0);
        port = socket.getLocalPort();
        socket.close();
        DeploymentOptions options = new DeploymentOptions().setConfig(new JsonObject().put("resource.http.port", port));
        vertx.deployVerticle(TurboResourceVerticle.class.getName(), options, context.asyncAssertSuccess());
    }

    @After
    public void after(TestContext context) {
        vertx.close(context.asyncAssertSuccess());
    }

    @Test
    public void testGetAllResources(TestContext context) {

        final Async async = context.async();
        vertx.createHttpClient().getNow(port, "localhost", "/", response -> {
            response.handler(body -> {

                JsonArray resources = body.toJsonArray();
                context.assertTrue(resources.contains("redis"));
                context.assertTrue(resources.contains("cassandra"));

                async.complete();
            });
        });

    }

}
