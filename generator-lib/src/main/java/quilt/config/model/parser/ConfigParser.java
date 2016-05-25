package quilt.config.model.parser;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import quilt.config.model.Broker;
import quilt.config.model.Config;

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

        List<Broker> brokerList = new ArrayList<>();
        while (it.hasNext()) {
            Map.Entry<String, JsonNode> entry = it.next();
            Broker broker = new Broker(
                    entry.getKey(),
                    entry.getValue().get("store-and-forward").asBoolean(),
                    entry.getValue().get("multicast").asBoolean());
            brokerList.add(broker);
        }

        return new Config(brokerList);
    }
}
