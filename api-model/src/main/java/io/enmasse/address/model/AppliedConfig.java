/*
 * Copyright 2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package io.enmasse.address.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.enmasse.config.AnnotationKeys;
import io.fabric8.kubernetes.api.model.HasMetadata;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.HashMap;
import java.util.Objects;
import java.util.Optional;

public final class AppliedConfig {
    private static final ObjectMapper mapper = new ObjectMapper();

    private AddressSpec addressSpec;

    public static AppliedConfig parseCurrentAppliedConfig(String json) throws IOException {
        if (json == null) {
            return null;
        }
        return mapper.readValue(json, AppliedConfig.class);
    }

    public static AppliedConfig parseCurrentAppliedConfig(final HasMetadata resource) {
        try {
            if (resource.getMetadata().getAnnotations() == null || resource.getMetadata().getAnnotations().isEmpty()) {
                return null;
            }
            return parseCurrentAppliedConfig(resource.getMetadata().getAnnotations().get(AnnotationKeys.APPLIED_CONFIGURATION));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public static void setCurrentAppliedConfig(final HasMetadata metadata, AppliedConfig config) {
        if (metadata.getMetadata().getAnnotations() == null) {
            metadata.getMetadata().setAnnotations(new HashMap<>());
        }
        // Remove old annotation no longer used
        metadata.getMetadata().getAnnotations().remove(AnnotationKeys.APPLIED_PLAN);

        try {
            metadata.getMetadata().getAnnotations().put(AnnotationKeys.APPLIED_CONFIGURATION, mapper.writeValueAsString(config));
        } catch (JsonProcessingException e) {
            throw new UncheckedIOException(e);
        }
    }

    public static <T> T normalize(Class<T> clazz, final T data) throws JsonProcessingException {
        if ( data == null ) {
            return null;
        } else {
            return mapper.readValue(mapper.writeValueAsString(data), clazz);
        }
    }

    public static AppliedConfig create(AddressSpec spec) {
        // First normalize the values to your JSON convention.
        // If we don't do this, then a serialized object might not be equal to an in-memory object
        final AddressSpec normalizedSpec;
        try {
            normalizedSpec = normalize(AddressSpec.class, spec);
        } catch (JsonProcessingException e) {
            throw new UncheckedIOException(e);
        }

        // create the new instance
        final AppliedConfig config = new AppliedConfig();
        config.addressSpec = normalizedSpec;

        // and return it
        return config;
    }

    public static Optional<String> getCurrentAppliedPlanFromAddress(Address address) {
        Optional<AddressSpec> currentAppliedAddressSpec = getCurrentAppliedAddressSpec(address);
        if (currentAppliedAddressSpec.isEmpty()) {
            return Optional.empty();
        }
        return Optional.ofNullable(currentAppliedAddressSpec.get().getPlan());
    }

    public static Optional<AddressSpec> getCurrentAppliedAddressSpec(Address address) {
        try {
            AppliedConfig appliedConfig = parseCurrentAppliedConfig(address);
            if (appliedConfig == null) {
                return Optional.empty();
            } else {
                return Optional.ofNullable(appliedConfig.getAddressSpec());
            }
        } catch (UncheckedIOException e) {
            return Optional.empty();
        }
    }

    public AddressSpec getAddressSpec() {
        return addressSpec;
    }

    @JsonIgnore
    public Optional<String> getPlan() {
        if (addressSpec == null || addressSpec.getPlan() == null) {
            return Optional.empty();
        } else {
            return Optional.of(addressSpec.getPlan());
        }
    }


    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AppliedConfig that = (AppliedConfig) o;
        return Objects.equals(addressSpec, that.addressSpec);
    }

    @Override
    public int hashCode() {
        return Objects.hash(addressSpec);
    }
}
