package enmasse.config;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;

/**
 * Encodes and decodes destination properties
 */
public class AddressEncoder {
    private static final Logger log = LoggerFactory.getLogger(AddressEncoder.class.getName());
    private static final ObjectMapper mapper = new ObjectMapper();
    private final ObjectNode properties;

    public AddressEncoder() {
        this.properties = mapper.createObjectNode();
    }

    public AddressEncoder(ObjectNode properties) {
        this.properties = properties;
    }

    public void encode(boolean storeAndForward, boolean multicast, Optional<String> flavor) {
        properties.put("store_and_forward", storeAndForward);
        properties.put("multicast", multicast);
        flavor.ifPresent(f -> properties.put("flavor", f));
    }

    public String toJson() {
        try {
            return mapper.writeValueAsString(properties);
        } catch (JsonProcessingException e) {
            log.error("Error encoding address properties", e);
            throw new RuntimeException(e);
        }
    }
}
