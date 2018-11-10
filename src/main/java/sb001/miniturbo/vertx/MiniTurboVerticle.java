package sb001.miniturbo.vertx;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.Future;
import io.vertx.core.Launcher;
import sb001.miniturbo.vertx.api.TurboApiVerticle;
import sb001.miniturbo.vertx.k8s.TurboK8sVerticle;
import sb001.miniturbo.vertx.resource.TurboResourceVerticle;
import sb001.miniturbo.vertx.web.TurboWebVerticle;

public class MiniTurboVerticle extends AbstractVerticle {

    @Override
    public void start(Future<Void> startFuture) throws Exception {

        DeploymentOptions options = new DeploymentOptions();

        vertx.deployVerticle(TurboResourceVerticle.class, options);
        vertx.deployVerticle(TurboApiVerticle.class, options);
        vertx.deployVerticle(TurboK8sVerticle.class, options);
        vertx.deployVerticle(TurboWebVerticle.class, options, h -> {
            if (h.succeeded()) {
                startFuture.complete();
            } else {
                startFuture.fail(h.cause());
            }
        });
    }

    public static void main(String[] args) {
        Launcher.main(new String[] { "run", MiniTurboVerticle.class.getCanonicalName() });
    }

}
