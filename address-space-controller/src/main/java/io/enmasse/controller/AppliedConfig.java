/*
 * Copyright 2017-2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package io.enmasse.controller;

import java.io.IOException;
import java.util.HashMap;
import java.util.Objects;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.enmasse.address.model.AddressSpaceSpec;
import io.enmasse.address.model.AuthenticationServiceSettings;
import io.enmasse.config.AnnotationKeys;
import io.fabric8.kubernetes.api.model.HasMetadata;

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

    public static <T> T normalize(Class<T> clazz, final T data) throws JsonProcessingException {
        if ( data == null ) {
            return null;
        } else {
            return mapper.readValue(mapper.writeValueAsString(data), clazz);
        }
    }

    public static AppliedConfig create(AddressSpaceSpec spec, AuthenticationServiceSettings authServiceSettings) throws JsonProcessingException {
        // First normalize the values to your JSON convention.
        // If we don't do this, then a serialized object might not be equal to an in-memory object
        final AddressSpaceSpec normalizedSpec = normalize(AddressSpaceSpec.class, spec);
        final AuthenticationServiceSettings normalizedAuth= normalize(AuthenticationServiceSettings.class, authServiceSettings);

        // create the new instance
        final AppliedConfig config = new AppliedConfig();
        config.addressSpaceSpec = normalizedSpec;
        config.authenticationServiceSettings = normalizedAuth;

        // and return it
        return config;
    }

    public AuthenticationServiceSettings getAuthenticationServiceSettings() {
        return authenticationServiceSettings;
    }

    public AddressSpaceSpec getAddressSpaceSpec() {
        return addressSpaceSpec;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AppliedConfig that = (AppliedConfig) o;
        return Objects.equals(authenticationServiceSettings, that.authenticationServiceSettings) &&
                Objects.equals(addressSpaceSpec, that.addressSpaceSpec);
    }

    @Override
    public int hashCode() {
        return Objects.hash(authenticationServiceSettings, addressSpaceSpec);
    }
}
