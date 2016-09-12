package enmasse.config.bridge.model;

import java.util.Map;

/**
 * Represents a subscriber that will get notified of config updates.
 */
public interface ConfigSubscriber {
    void configUpdated(String name, String version, Map<String, String> values);
}
