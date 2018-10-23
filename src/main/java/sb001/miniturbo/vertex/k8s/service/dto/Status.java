package sb001.miniturbo.vertex.k8s.service.dto;

import java.util.List;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class Status {

    private List<String> serviceExternalIPs;
    private List<Integer> serviceExternalPorts;
    private String image;

    private Boolean ready;

}
