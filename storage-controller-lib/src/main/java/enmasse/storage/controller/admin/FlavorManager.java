package enmasse.storage.controller.admin;

import com.fasterxml.jackson.databind.JsonNode;
import enmasse.storage.controller.model.Flavor;
import enmasse.storage.controller.parser.FlavorParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @author Ulf Lilleengen
 */
public class FlavorManager implements FlavorRepository {
    private static final Logger log = LoggerFactory.getLogger(FlavorManager.class.getName());
    private volatile Map<String, Flavor> flavorMap = Collections.emptyMap();

    @Override
    public Flavor getFlavor(String flavorName, long timeoutInMillis) {
        long endTime = System.currentTimeMillis() + timeoutInMillis;
        Flavor flavor = null;
        try {
            while (System.currentTimeMillis() < endTime && flavor == null) {
                flavor = flavorMap.get(flavorName);
                if (flavor == null) {
                    Thread.sleep(1000);
                }
            }
        } catch (InterruptedException e) {
            log.warn("Interrupted while retrieving flavor");
        }
        if (flavor == null) {
            String flavors = flavorMap.keySet().stream().collect(Collectors.joining(","));
            throw new IllegalArgumentException(String.format("No flavor with name '%s' exists, have [%s]", flavorName, flavors));
        }
        return flavor;
    }

    public void flavorsUpdated(Map<String, Flavor> flavorMap) {
        this.flavorMap = flavorMap;
        if (log.isInfoEnabled()) {
            String flavors = flavorMap.keySet().stream().collect(Collectors.joining(","));
            log.info(String.format("Got new set of flavors: [%s]", flavors));
        }

    }

    public void configUpdated(JsonNode jsonConfig) {
        flavorsUpdated(FlavorParser.parse(jsonConfig));
    }
}
