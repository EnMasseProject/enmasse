package io.enmasse.controller.common;

import java.util.Set;

/**
 * Handles changes to a resource
 */
public interface Watcher<T> {
    void resourcesUpdated(Set<T> resources) throws Exception;
}
