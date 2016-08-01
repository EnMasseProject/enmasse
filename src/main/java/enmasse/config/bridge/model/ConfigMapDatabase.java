package enmasse.config.bridge.model;

/**
 * Represents a database of config maps that can be subscribed to.
 *
 * @author lulf
 */
public interface ConfigMapDatabase {
    void subscribe(String name, ConfigSubscriber configSubscriber);
}
