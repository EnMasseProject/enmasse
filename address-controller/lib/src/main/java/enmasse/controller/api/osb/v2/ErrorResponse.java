package enmasse.controller.api.osb.v2;

import java.io.IOException;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.node.ObjectNode;

@JsonSerialize(using = ErrorResponse.Serializer.class)
public class ErrorResponse {
    private static final ObjectMapper mapper = new ObjectMapper();

    private String description;

    public ErrorResponse(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    protected static class Serializer extends JsonSerializer<ErrorResponse> {
        @Override
        public void serialize(ErrorResponse value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
            ObjectNode node = mapper.createObjectNode();
            if (value.getDescription() != null) {
                node.put("description", value.getDescription());
            }
            mapper.writeValue(gen, node);
        }
    }
}
