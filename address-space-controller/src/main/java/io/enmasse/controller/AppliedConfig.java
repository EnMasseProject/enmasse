/*
 * Copyright 2017-2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package io.enmasse.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.enmasse.address.model.AddressSpace;
import io.enmasse.address.model.AddressSpaceSpec;
import io.enmasse.admin.model.v1.InfraConfig;
import io.enmasse.config.AnnotationKeys;
import io.fabric8.kubernetes.api.model.HasMetadata;

import java.io.IOException;
import java.util.HashMap;

public final class AppliedConfig {
    private static final ObjectMapper mapper = new ObjectMapper();
    private AppliedConfig() {
    }

    public static AddressSpaceSpec parseCurrentAppliedConfig(String json) throws IOException {
        if (json == null) {
            return null;
        }
        return mapper.readValue(json, AddressSpaceSpec.class);
    }

    public static AddressSpaceSpec parseCurrentAppliedConfig(final HasMetadata resource) throws IOException {
        return parseCurrentAppliedConfig(resource.getMetadata().getAnnotations().get(AnnotationKeys.APPLIED_CONFIGURATION));
    }

    public static void setCurrentAppliedConfig(final HasMetadata metadata, AddressSpaceSpec config) throws JsonProcessingException {
        if (metadata.getMetadata().getAnnotations() == null) {
            metadata.getMetadata().setAnnotations(new HashMap<>());
        }
        // Remove old annotation no longer used
        metadata.getMetadata().getAnnotations().remove(AnnotationKeys.APPLIED_PLAN);

        metadata.getMetadata().getAnnotations().put(AnnotationKeys.APPLIED_CONFIGURATION, mapper.writeValueAsString(config));
    }


}
