/*
 * Copyright 2017-2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package io.enmasse.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.enmasse.address.model.AddressSpaceSpec;
import io.enmasse.address.model.AuthenticationServiceSettings;
import io.enmasse.config.AnnotationKeys;
import io.fabric8.kubernetes.api.model.HasMetadata;

import java.io.IOException;
import java.util.HashMap;

public final class AppliedConfig {
    private static final ObjectMapper mapper = new ObjectMapper();

    private AuthenticationServiceSettings authenticationServiceSettings;
    private AddressSpaceSpec addressSpaceSpec;

    public static AppliedConfig parseCurrentAppliedConfig(String json) throws IOException {
        if (json == null) {
            return null;
        }
        return mapper.readValue(json, AppliedConfig.class);
    }

    public static AppliedConfig parseCurrentAppliedConfig(final HasMetadata resource) throws IOException {
        return parseCurrentAppliedConfig(resource.getMetadata().getAnnotations().get(AnnotationKeys.APPLIED_CONFIGURATION));
    }

    public static void setCurrentAppliedConfig(final HasMetadata metadata, AppliedConfig config) throws JsonProcessingException {
        if (metadata.getMetadata().getAnnotations() == null) {
            metadata.getMetadata().setAnnotations(new HashMap<>());
        }
        // Remove old annotation no longer used
        metadata.getMetadata().getAnnotations().remove(AnnotationKeys.APPLIED_PLAN);

        metadata.getMetadata().getAnnotations().put(AnnotationKeys.APPLIED_CONFIGURATION, mapper.writeValueAsString(config));
    }

    public static AppliedConfig create(AddressSpaceSpec spec, AuthenticationServiceSettings authServiceSettings) {
        AppliedConfig config = new AppliedConfig();
        config.setAddressSpaceSpec(spec);
        config.setAuthenticationServiceSettings(authServiceSettings);
        return config;
    }

    public AuthenticationServiceSettings getAuthenticationServiceSettings() {
        return authenticationServiceSettings;
    }

    public void setAuthenticationServiceSettings(AuthenticationServiceSettings authenticationServiceSettings) {
        this.authenticationServiceSettings = authenticationServiceSettings;
    }

    public AddressSpaceSpec getAddressSpaceSpec() {
        return addressSpaceSpec;
    }

    public void setAddressSpaceSpec(AddressSpaceSpec addressSpaceSpec) {
        this.addressSpaceSpec = addressSpaceSpec;
    }
}
