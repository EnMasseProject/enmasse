package enmasse.storage.controller.admin;

import com.fasterxml.jackson.databind.JsonNode;
import enmasse.storage.controller.model.FlavorConfig;
import enmasse.storage.controller.model.parser.FlavorConfigParser;

import java.io.IOException;
import java.util.Collections;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * @author Ulf Lilleengen
 */
public class FlavorManager implements ConfigManager, FlavorRepository {
    private static final Logger log = Logger.getLogger(FlavorManager.class.getName());
    private volatile Map<String, FlavorConfig> flavorMap = Collections.emptyMap();
    private final FlavorConfigParser parser = new FlavorConfigParser();

    @Override
    public FlavorConfig getFlavor(String flavorName) {
        FlavorConfig flavor = flavorMap.get(flavorName);
        if (flavor == null) {
            String flavors = flavorMap.keySet().stream().collect(Collectors.joining(","));
            throw new IllegalArgumentException(String.format("No flavor with name '%s' exists, have [%s]", flavorName, flavors));
        }
        return flavor;
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
        if (log.isLoggable(Level.INFO)) {
            String flavors = flavorMap.keySet().stream().collect(Collectors.joining(","));
            log.log(Level.INFO, String.format("Got new set of flavors: [%s]", flavors));
        }

    }
}
