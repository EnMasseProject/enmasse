/*
 * Copyright 2017-2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package io.enmasse.controller;

import java.io.IOException;

import com.fasterxml.jackson.databind.ObjectMapper;

import io.enmasse.address.model.AddressSpace;
import io.enmasse.address.model.Schema;
import io.enmasse.admin.model.v1.InfraConfig;
import io.enmasse.config.AnnotationKeys;

public final class InfraConfigs {
    private InfraConfigs() {
    }

    public static InfraConfig parseCurrentInfraConfig(final Schema schema, final AddressSpace addressSpace) throws IOException {
        final String config = addressSpace.getAnnotation(AnnotationKeys.APPLIED_INFRA_CONFIG);
        if ( config == null) {
            return null;
        }

        return new ObjectMapper().readValue(config, InfraConfig.class);
    }


}
