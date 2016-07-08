package enmasse.storage.controller.model.parser;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.openshift.restclient.images.DockerImageURI;
import enmasse.storage.controller.model.FlavorConfig;
import enmasse.storage.controller.model.StorageConfig;

import java.io.IOException;
import java.io.Reader;
import java.util.*;

/**
 * @author Ulf Lilleengen
 */
public class FlavorConfigParser {
    private final ObjectMapper mapper = new ObjectMapper();

    public Map<String, FlavorConfig> parse(Reader config) throws IOException {
        JsonNode root = mapper.readTree(config);
        return parse(root);
    }

    public Map<String, FlavorConfig> parse(JsonNode root) throws IOException {
        Iterator<Map.Entry<String, JsonNode>> it = root.fields();
        Map<String, FlavorConfig> flavorMap = new LinkedHashMap<>();
        while (it.hasNext()) {
            Map.Entry<String, JsonNode> entry = it.next();
            flavorMap.put(entry.getKey(), parseFlavor(entry.getValue()));
        }
        return flavorMap;
    }

    private FlavorConfig parseFlavor(JsonNode node) {
        FlavorConfig.Builder builder = new FlavorConfig.Builder();
        Iterator<Map.Entry<String, JsonNode>> it = node.fields();
        while (it.hasNext()) {
            Map.Entry<String, JsonNode> entry = it.next();
            JsonNode value = entry.getValue();
            String key = entry.getKey();

            if (key.equals("broker")) {
                parseBroker(builder, value);
            } else if (key.equals("router")) {
                parseRouter(builder, value);
            } else if (key.equals("shared")) {
                builder.isShared(value.asBoolean());
            }
        }
        return builder.build();
    }

    private void parseRouter(FlavorConfig.Builder builder, JsonNode value) {
        builder.routerImage(new DockerImageURI(value.get("dockerImage").asText()));
        if (value.has("certPath")) {
            builder.routerSecretPath(value.get("certPath").asText());
        }
        if (value.has("certSecretName")) {
            builder.routerSecretName(value.get("certSecretName").asText());
        }
    }

    private void parseBroker(FlavorConfig.Builder builder, JsonNode value) {
        builder.brokerImage(new DockerImageURI(value.get("dockerImage").asText()));
        if (value.has("storage")) {
            parseStorage(builder, value.get("storage"));
        }
    }

    private void parseStorage(FlavorConfig.Builder builder, JsonNode value) {
        String volumeType = value.get("volumeType").asText();
        String mountPath = value.get("mountPath").asText();
        String size = "0Gi";
        if (value.has("size")) {
            size = value.get("size").asText();
        }
        builder.storage(new StorageConfig(volumeType, size, mountPath));
    }
}
