package enmasse.storage.controller.parser;

import com.fasterxml.jackson.databind.JsonNode;
import enmasse.storage.controller.model.Flavor;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Parser for the flavor config.
 */
public class FlavorParser {
    private static final String KEY_TEMPLATE_NAME = "templateName";
    private static final String KEY_TEMPLATE_PARAMETERS = "templateParameters";

    public static Map<String, Flavor> parse(JsonNode root) {
        Map<String, Flavor> flavorMap = new LinkedHashMap<>();
        Iterator<Map.Entry<String, JsonNode>> it = root.fields();
        while (it.hasNext()) {
            Map.Entry<String, JsonNode> entry = it.next();
            String name = entry.getKey();
            Flavor flavor = parseFlavor(entry.getValue());
            flavorMap.put(name, flavor);
        }
        return flavorMap;
    }

    private static Flavor parseFlavor(JsonNode node) {
        Flavor.Builder builder = new Flavor.Builder();
        builder.templateName(node.get(KEY_TEMPLATE_NAME).asText());

        if (node.has(KEY_TEMPLATE_PARAMETERS)) {
            Iterator<Map.Entry<String, JsonNode>> it = node.get(KEY_TEMPLATE_PARAMETERS).fields();
            while (it.hasNext()) {
                Map.Entry<String, JsonNode> entry = it.next();
                builder.templateParameter(entry.getKey(), entry.getValue().asText());
            }
        }
        return builder.build();
    }
}
