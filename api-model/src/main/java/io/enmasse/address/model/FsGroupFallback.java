/*
 * Copyright 2019, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package io.enmasse.address.model;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Strings;

import java.util.HashMap;
import java.util.Map;

public class FsGroupFallback {

    private static TypeReference<Map<String, Long>> MAP_STRING_LONG = new TypeReference<>() {
    };

    private static final Map<String, Long> FS_GROUP_FALLBACK;

    static {
        final String json = System.getenv().get("FS_GROUP_FALLBACK_MAP");
        if (Strings.isNullOrEmpty(json)) {
            FS_GROUP_FALLBACK = new HashMap<>();
        } else {
            ObjectMapper objectMapper = new ObjectMapper();
            try {
                FS_GROUP_FALLBACK = objectMapper.readValue(json, MAP_STRING_LONG);
            } catch (JsonProcessingException e) {
                throw new ExceptionInInitializerError(e);
            }
        }
    }

    public static Long getFsGroupOverride(String component) {
        return FS_GROUP_FALLBACK.get(component);
    }
}
