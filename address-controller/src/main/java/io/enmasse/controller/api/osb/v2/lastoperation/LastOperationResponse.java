/*
 * Copyright 2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.controller.api.osb.v2.lastoperation;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.io.IOException;

@JsonSerialize(using = LastOperationResponse.Serializer.class)
public class LastOperationResponse {
    private static final ObjectMapper mapper = new ObjectMapper();

    private LastOperationState state;
    private String description;

    public LastOperationResponse() {
    }

    public LastOperationResponse(LastOperationState state, String description) {
        this.state = state;
        this.description = description;
    }

    public LastOperationState getState() {
        return state;
    }

    public String getDescription() {
        return description;
    }

    protected static class Serializer extends JsonSerializer<LastOperationResponse> {
        @Override
        public void serialize(LastOperationResponse value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
            ObjectNode node = mapper.createObjectNode();
            node.put("state", toStateString(value.getState()));
            if (value.getDescription() != null) {
                node.put("description", value.getDescription());
            }
            mapper.writeValue(gen, node);
        }

        private String toStateString(LastOperationState state) {
            switch (state) {
                case IN_PROGRESS:
                    return "in progress";
                case SUCCEEDED:
                    return "succeeded";
                case FAILED:
                    return "failed";
                default:
                    throw new InternalError("Unknown LastOperationState " + state);
            }
        }
    }
}
