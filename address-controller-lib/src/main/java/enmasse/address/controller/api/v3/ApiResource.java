package enmasse.address.controller.api.v3;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.io.IOException;

@JsonDeserialize(using = ApiResource.Deserializer.class)
public class ApiResource {
    private final String kind;
    private final String apiVersion;

    public ApiResource(String kind, String apiVersion) {
        this.kind = kind;
        this.apiVersion = apiVersion;
    }

    public String getKind() {
        return kind;
    }

    public String getApiVersion() {
        return apiVersion;
    }

    public static class Deserializer extends JsonDeserializer<ApiResource> {
        @Override
        public ApiResource deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
            ObjectNode node = p.getCodec().readTree(p);
            return new ApiResource(node.get(ResourceKeys.KIND).asText(), node.get(ResourceKeys.APIVERSION).asText());
        }
    }
}
