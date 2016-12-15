package enmasse.config.service.openshift;

import io.fabric8.kubernetes.api.model.HasMetadata;

/**
 * Extends a Kubernetes resource with metadata and applies hashCode and equals
 */
public class Resource<T extends HasMetadata> {
    T resource;

    public Resource(T resource) {
        this.resource = resource;
    }

    public T getResource() {
        return resource;
    }

    @Override
    public int hashCode() {
        int result = resource.getMetadata().getName().hashCode();
        result = 31 * result + resource.getKind().hashCode();
        return result;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Resource<T> other = (Resource<T>) o;
        return resource.getMetadata().getName().equals(other.getResource().getMetadata().getName()) &&
            resource.getKind().equals(other.getResource().getKind());
    }

    @Override
    public String toString() {
        return resource.getKind() + ":" + resource.getMetadata().getName();
    }
}
