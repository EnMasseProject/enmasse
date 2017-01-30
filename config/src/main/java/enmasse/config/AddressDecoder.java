package enmasse.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Optional;

/**
 * Encodes and decodes destination properties
 */
public class AddressDecoder {
    private static final Logger log = LoggerFactory.getLogger(AddressDecoder.class.getName());
    private static final ObjectMapper mapper = new ObjectMapper();
    private final ObjectNode properties;

    public AddressDecoder(String json) {
        try {
            properties = (ObjectNode) mapper.readTree(json);
        } catch (IOException e) {
            log.error("Error decoding address properties", e);
            throw new IllegalArgumentException(e);
        }
    }

    public boolean storeAndForward() {
        return properties.get("store_and_forward").asBoolean();
    }

    public boolean multicast() {
        return properties.get("multicast").asBoolean();
    }

    public Optional<String> flavor() {
        return properties.has("flavor") ? Optional.of(properties.get("flavor").asText()) : Optional.empty();
    }
}
