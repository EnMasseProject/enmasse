package enmasse.storage.controller.admin;

import com.fasterxml.jackson.databind.JsonNode;
import enmasse.storage.controller.model.FlavorConfig;
import enmasse.storage.controller.model.parser.FlavorConfigParser;

import java.io.IOException;
import java.util.Map;

/**
 * @author Ulf Lilleengen
 */
public class FlavorManager implements ConfigManager, FlavorRepository {
    private volatile Map<String, FlavorConfig> flavorMap;
    private final FlavorConfigParser parser = new FlavorConfigParser();

    @Override
    public FlavorConfig getFlavor(String flavorName) {
        return flavorMap.get(flavorName);
    }

    @Override
    public FlavorConfig getDefaultFlavor() {
        return new FlavorConfig.Builder().build();
    }

    @Override
    public void configUpdated(JsonNode jsonConfig) throws IOException {
        configUpdated(parser.parse(jsonConfig));
    }

    public void configUpdated(Map<String, FlavorConfig> flavorMap) {
        this.flavorMap = flavorMap;
    }
}
