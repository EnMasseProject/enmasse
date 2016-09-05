package enmasse.storage.controller.admin;

import com.fasterxml.jackson.databind.JsonNode;

import java.io.IOException;

/**
 * @author Ulf Lilleengen
 */
public interface ConfigSubscriber {
    void configUpdated(JsonNode jsonConfig) throws IOException;
}
