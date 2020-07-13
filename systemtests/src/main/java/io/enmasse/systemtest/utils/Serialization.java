/*
 * Copyright 2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package io.enmasse.systemtest.utils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;

public final class Serialization {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final ObjectMapper OBJECT_MAPPER_PRETTY = new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);
    private static final ObjectMapper OBJECT_MAPPER_YAML = new ObjectMapper(new YAMLFactory());

    private Serialization() {
    }

    /**
     * Serialize value as YAML.
     */
    public static String toYaml(final Object value) {
        return toString(OBJECT_MAPPER_YAML, value);
    }

    /**
     * Serialize value as pretty-JSON.
     */
    public static String toJson(final Object value) {
        return toJson(true, value);
    }

    /**
     * Serialize value as JSON.
     * @param pretty Toggle pretty or compact JSON.
     * @param value The value to serialize
     * @return The serialized value.
     */
    public static String toJson(final boolean pretty, final Object value) {
        return toString(pretty ? OBJECT_MAPPER_PRETTY : OBJECT_MAPPER, value);
    }

    private static String toString(final ObjectMapper mapper, final Object value) {
        try {
            return mapper.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to serialize to JSON", e);
        }
    }

}
