package enmasse.storage.controller.model.parser;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import enmasse.storage.controller.model.Destination;
import enmasse.storage.controller.model.Config;

import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * @author lulf
 */
public class ConfigParser {
    private final ObjectMapper mapper = new ObjectMapper();

    public Config parse(Reader config) throws IOException {
        JsonNode root = mapper.readTree(config);
        return parse(root);
    }

    public Config parse(JsonNode root) throws IOException {
        Iterator<Map.Entry<String, JsonNode>> it = root.fields();

        List<Destination> destinationList = new ArrayList<>();
        while (it.hasNext()) {
            Map.Entry<String, JsonNode> entry = it.next();
            Destination destination = new Destination(
                    entry.getKey(),
                    entry.getValue().get("store_and_forward").asBoolean(),
                    entry.getValue().get("multicast").asBoolean());
            destinationList.add(destination);
        }

        return new Config(destinationList);
    }
}
