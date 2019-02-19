/*
 * Copyright 2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package io.enmasse.address.model;

import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;

/**
 * A helper to serialize/deserialize maps of ports.
 */
public final class PortMap {

    private PortMap() {}

    private static class Mapping {
        private String name;
        private Integer port;

        @JsonCreator
        public Mapping(
                @JsonProperty("name") String name,
                @JsonProperty("port") Integer port) {
            this.name = name;
            this.port = port;
        }

        public String getName() {
            return name;
        }

        public Integer getPort() {
            return port;
        }
    }

    public static class Deserializer extends JsonDeserializer<Map<String, Integer>> {

        @Override
        public Map<String, Integer> deserialize(JsonParser p, DeserializationContext ctxt) throws IOException, JsonProcessingException {

            if (p.currentToken() == JsonToken.VALUE_NULL) {
                return null;
            }

            if (p.currentToken() != JsonToken.START_ARRAY) {
                throw new JsonParseException(p, "Expected start of array");
            }

            p.nextToken();

            if (p.currentToken() == JsonToken.END_ARRAY) {
                return new HashMap<> ();
            }

            final Map<String, Integer> result = new HashMap<>();
            final Iterator<Mapping> i = p.readValuesAs(Mapping.class);
            i.forEachRemaining(m -> result.put(m.getName(), m.getPort()));

            return result;
        }

    }

    public static class Serializer extends JsonSerializer<Map<String, Integer>> {

        @Override
        public void serialize(Map<String, Integer> value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
            if (value == null) {
                gen.writeNull();
                return;
            }

            gen.writeStartArray();
            for (Map.Entry<String, Integer> entry : value.entrySet()) {
                gen.writeStartObject();
                gen.writeObjectField("name", entry.getKey());
                gen.writeObjectField("port", entry.getValue());
                gen.writeEndObject();
            }
            gen.writeEndArray();
        }

    }

}
