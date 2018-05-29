/*
 * Copyright 2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.api.common;

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

    private int statusCode;
    private String reason;
    private String message;

    public ErrorResponse(int statusCode, String reason, String message) {
        this.statusCode = statusCode;
        this.reason = reason;
        this.message = message;
    }

    public String getReason() {
        return reason;
    }

    public String getMessage() {
        return message;
    }

    public int getStatusCode() {
        return statusCode;
    }

    protected static class Serializer extends JsonSerializer<ErrorResponse> {
        @Override
        public void serialize(ErrorResponse value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
            ObjectNode node = mapper.createObjectNode();
            node.put("apiVersion", "v1");
            node.put("kind", "Status");
            node.put("status", "Failure");
            node.put("code", value.getStatusCode());
            if (value.getReason() != null) {
                node.put("reason", value.getReason());
            }

            if (value.getMessage() != null) {
                node.put("message", value.getMessage());
            }
            mapper.writeValue(gen, node);
        }
    }
}
