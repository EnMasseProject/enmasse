package enmasse.storage.controller.admin;

import com.fasterxml.jackson.databind.JsonNode;

import java.io.IOException;

/**
 * Represents a component subscribes to JSON config updates.
 */
public interface ConfigSubscriber {
    void configUpdated(JsonNode jsonConfig) throws IOException;
}
