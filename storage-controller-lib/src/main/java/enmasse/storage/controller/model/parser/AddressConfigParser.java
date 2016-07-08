package enmasse.storage.controller.model.parser;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import enmasse.storage.controller.admin.FlavorRepository;
import enmasse.storage.controller.model.AddressConfig;
import enmasse.storage.controller.model.Destination;
import enmasse.storage.controller.model.FlavorConfig;

import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * @author lulf
 */
public class AddressConfigParser {
    private final ObjectMapper mapper = new ObjectMapper();
    private final FlavorRepository flavorRepository;

    public AddressConfigParser(FlavorRepository flavorRepository) {
        this.flavorRepository = flavorRepository;
    }

    public AddressConfig parse(Reader config) throws IOException {
        JsonNode root = mapper.readTree(config);
        return parse(root);
    }

    public AddressConfig parse(JsonNode root) throws IOException {
        Iterator<Map.Entry<String, JsonNode>> it = root.fields();

        List<Destination> destinationList = new ArrayList<>();
        while (it.hasNext()) {
            Map.Entry<String, JsonNode> entry = it.next();
            FlavorConfig flavorConfig = entry.getValue().has("flavor") ? flavorRepository.getFlavor(entry.getValue().get("flavor").asText()) : flavorRepository.getDefaultFlavor();
            Destination destination = new Destination(
                    entry.getKey(),
                    entry.getValue().get("store_and_forward").asBoolean(),
                    entry.getValue().get("multicast").asBoolean(),
                    flavorConfig);
            destinationList.add(destination);
        }

        return new AddressConfig(destinationList);
    }
}
