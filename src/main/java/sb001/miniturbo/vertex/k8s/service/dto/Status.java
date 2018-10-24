package sb001.miniturbo.vertex.k8s.service.dto;

import java.util.Map;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class Status {

    private Map<Integer, Integer> ports;
    private String image;

    private Boolean ready;

}
