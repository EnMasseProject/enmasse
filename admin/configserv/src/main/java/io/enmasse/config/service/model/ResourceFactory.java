package io.enmasse.config.service.model;

import io.fabric8.kubernetes.api.model.HasMetadata;

/**
 * Creates the appropriate resource wrapper for a given type.
 */
public interface ResourceFactory<T extends Resource> {
    T createResource(HasMetadata in);
}
