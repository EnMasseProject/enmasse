/*
 * Copyright 2016 Red Hat Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package enmasse.address.controller.parser;

import com.fasterxml.jackson.databind.JsonNode;
import enmasse.address.controller.model.Flavor;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Parser for the flavor config.
 */
public class FlavorParser {
    private static final String KEY_TEMPLATE_NAME = "templateName";
    private static final String KEY_TEMPLATE_PARAMETERS = "templateParameters";
    private static final String KEY_TYPE = "type";
    private static final String KEY_DESCRIPTION = "description";

    public static Map<String, Flavor> parse(JsonNode root) {
        Map<String, Flavor> flavorMap = new LinkedHashMap<>();
        Iterator<Map.Entry<String, JsonNode>> it = root.fields();
        while (it.hasNext()) {
            Map.Entry<String, JsonNode> entry = it.next();
            String name = entry.getKey();
            Flavor flavor = parseFlavor(name, entry.getValue());
            flavorMap.put(name, flavor);
        }
        return flavorMap;
    }

    private static Flavor parseFlavor(String name, JsonNode node) {
        Flavor.Builder builder = new Flavor.Builder(name, node.get(KEY_TEMPLATE_NAME).asText());

        if (node.has(KEY_TYPE)) {
            builder.type(node.get(KEY_TYPE).asText());
        }

        if (node.has(KEY_DESCRIPTION)) {
            builder.description(node.get(KEY_DESCRIPTION).asText());
        }

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
