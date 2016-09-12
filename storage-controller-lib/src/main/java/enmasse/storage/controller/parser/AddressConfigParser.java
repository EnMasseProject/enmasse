package enmasse.storage.controller.parser;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import enmasse.storage.controller.model.AddressConfig;
import enmasse.storage.controller.model.Destination;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * Parser for the addressing config.
 */
public class AddressConfigParser {
    private static final String STORE_AND_FORWARD = "store_and_forward";
    private static final String MULTICAST = "multicast";
    private static final String FLAVOR = "flavor";

    public static AddressConfig parse(JsonNode root) throws IOException {
        List<Destination> destinationList = new ArrayList<>();
        Iterator<Map.Entry<String, JsonNode>> it = root.fields();
        while (it.hasNext()) {
            Map.Entry<String, JsonNode> entry = it.next();
            JsonNode node = entry.getValue();
            Destination destination = new Destination(
                    entry.getKey(),
                    node.get(STORE_AND_FORWARD).asBoolean(),
                    node.get(MULTICAST).asBoolean(),
                    node.has(FLAVOR) ? node.get(FLAVOR).asText() : "");
            destinationList.add(destination);
        }

        return new AddressConfig(destinationList);
    }
}
