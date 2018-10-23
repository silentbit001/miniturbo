package sb001.miniturbo.vertex;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.Future;
import io.vertx.core.Launcher;
import sb001.miniturbo.vertex.api.TurboApiVerticle;
import sb001.miniturbo.vertex.k8s.TurboK8sVerticle;
import sb001.miniturbo.vertex.resource.TurboResourceVerticle;
import sb001.miniturbo.vertex.web.TurboWebVerticle;

public class MiniTurboVerticle extends AbstractVerticle {

    @Override
    public void start(Future<Void> startFuture) throws Exception {
        vertx.deployVerticle(TurboWebVerticle.class, new DeploymentOptions());
        vertx.deployVerticle(TurboResourceVerticle.class, new DeploymentOptions());
        vertx.deployVerticle(TurboApiVerticle.class, new DeploymentOptions());
        vertx.deployVerticle(TurboK8sVerticle.class, new DeploymentOptions());
    }

    public static void main(String[] args) {
        Launcher.main(new String[] { "run", MiniTurboVerticle.class.getCanonicalName() });
    }

}
