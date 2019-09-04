/*
 * Copyright 2017-2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package io.enmasse.controller;

import java.io.IOException;
import java.util.HashMap;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.enmasse.address.model.AddressSpace;
import io.enmasse.admin.model.v1.InfraConfig;
import io.enmasse.config.AnnotationKeys;
import io.fabric8.kubernetes.api.model.HasMetadata;

public final class InfraConfigs {
    private static final ObjectMapper mapper = new ObjectMapper();
    private InfraConfigs() {
    }

    public static InfraConfig parseCurrentInfraConfig(final AddressSpace addressSpace) throws IOException {
        final String config = addressSpace.getAnnotation(AnnotationKeys.APPLIED_INFRA_CONFIG);
        if (config == null) {
            return null;
        }

        return mapper.readValue(config, InfraConfig.class);
    }

    public static void setCurrentInfraConfig(final HasMetadata metadata, InfraConfig infraConfig) throws JsonProcessingException {
        if (metadata.getMetadata().getAnnotations() == null) {
            metadata.getMetadata().setAnnotations(new HashMap<>());
        }
        metadata.getMetadata().getAnnotations().put(AnnotationKeys.APPLIED_INFRA_CONFIG, mapper.writeValueAsString(infraConfig));
    }


}
