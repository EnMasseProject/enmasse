package enmasse.config.bridge.model;

import java.util.Map;

/**
 * @author lulf
 */
public interface ConfigSubscriber {
    void configUpdated(String name, String version, Map<String, String> values);
}
