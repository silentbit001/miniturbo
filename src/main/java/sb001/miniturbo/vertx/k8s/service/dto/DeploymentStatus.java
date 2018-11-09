package sb001.miniturbo.vertx.k8s.service.dto;

import java.util.Map;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class DeploymentStatus {

    private Map<Integer, Integer> ports;
    private String image;

    private Boolean ready;

}
