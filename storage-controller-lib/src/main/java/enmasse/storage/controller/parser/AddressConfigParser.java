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
 * @author lulf
 */
public class AddressConfigParser {

    private static final ObjectMapper mapper = new ObjectMapper();

    public static AddressConfig parse(JsonNode root) throws IOException {
        List<Destination> destinationList = new ArrayList<>();
        Iterator<Map.Entry<String, JsonNode>> it = root.fields();
        while (it.hasNext()) {
            Map.Entry<String, JsonNode> entry = it.next();
            JsonNode node = entry.getValue();
            Destination destination = new Destination(
                    entry.getKey(),
                    node.get("store_and_forward").asBoolean(),
                    node.get("multicast").asBoolean(),
                    node.has("flavor") ? node.get("flavor").asText() : "");
            destinationList.add(destination);
        }

        return new AddressConfig(destinationList);
    }
}
