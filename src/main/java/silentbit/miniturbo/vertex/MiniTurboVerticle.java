package silentbit.miniturbo.vertex;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.Future;
import io.vertx.core.Launcher;
import silentbit.miniturbo.vertex.api.TurboApiVerticle;
import silentbit.miniturbo.vertex.k8s.TurboK8sProxyVerticle;
import silentbit.miniturbo.vertex.resource.TurboResourceVerticle;
import silentbit.miniturbo.vertex.web.TurboWebVerticle;

public class MiniTurboVerticle extends AbstractVerticle {

    @Override
    public void start(Future<Void> startFuture) throws Exception {
        vertx.deployVerticle(TurboResourceVerticle.class, new DeploymentOptions());
        vertx.deployVerticle(TurboApiVerticle.class, new DeploymentOptions());
        vertx.deployVerticle(TurboK8sProxyVerticle.class, new DeploymentOptions());
        vertx.deployVerticle(TurboWebVerticle.class, new DeploymentOptions());
    }

    public static void main(String[] args) {
        Launcher.main(new String[] { "run", MiniTurboVerticle.class.getCanonicalName() });
    }

}
