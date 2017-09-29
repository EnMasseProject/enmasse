package io.enmasse.config.service.model;

import io.fabric8.kubernetes.api.model.HasMetadata;

/**
 * Defines a resource that can be used in a set.
 */
public abstract class Resource {
    abstract public int hashCode();
    abstract public boolean equals(Object o);
    abstract public String getName();
    abstract public String getKind();
    abstract public String toString();
}
