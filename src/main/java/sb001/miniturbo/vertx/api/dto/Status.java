package sb001.miniturbo.vertx.api.dto;

import java.util.List;

import io.vertx.core.json.JsonObject;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class Status {

    private List<String> resources;

    public JsonObject toJson() {
        return JsonObject.mapFrom(this);
    }

}
