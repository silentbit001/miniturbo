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
        vertx.deployVerticle(TurboResourceVerticle.class, new DeploymentOptions());
        vertx.deployVerticle(TurboApiVerticle.class, new DeploymentOptions());
        vertx.deployVerticle(TurboK8sVerticle.class, new DeploymentOptions());
        vertx.deployVerticle(TurboWebVerticle.class, new DeploymentOptions(), h -> {
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
