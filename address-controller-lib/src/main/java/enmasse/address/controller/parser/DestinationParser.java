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
import enmasse.address.controller.model.Destination;
import enmasse.address.controller.model.DestinationGroup;

import java.io.IOException;
import java.util.*;

/**
 * Parser for the addressing config.
 */
public class DestinationParser {
    private static final String STORE_AND_FORWARD = "store_and_forward";
    private static final String MULTICAST = "multicast";
    private static final String FLAVOR = "flavor";

    public static Set<DestinationGroup> parse(JsonNode root) throws IOException {
        Set<DestinationGroup> destinationGroups = new HashSet<>();
        Iterator<Map.Entry<String, JsonNode>> it = root.fields();
        while (it.hasNext()) {
            Map.Entry<String, JsonNode> entry = it.next();
            JsonNode node = entry.getValue();

            DestinationGroup.Builder builder = new DestinationGroup.Builder(entry.getKey());

            // Supports parsing the old syntax
            if (node.has(STORE_AND_FORWARD) && !node.get(STORE_AND_FORWARD).isObject()) {
                builder.destination(parseDestination(entry.getKey(), node));
            } else {
                Iterator<Map.Entry<String, JsonNode>> destIt = node.fields();
                while (destIt.hasNext()) {
                    Map.Entry<String, JsonNode> destEntry = it.next();
                    builder.destination(parseDestination(destEntry.getKey(), destEntry.getValue()));
                }

            }
            destinationGroups.add(builder.build());
        }
        return destinationGroups;
    }

    private static Destination parseDestination(String key, JsonNode node) {
        return new Destination(key,
                node.get(STORE_AND_FORWARD).asBoolean(),
                node.get(MULTICAST).asBoolean(),
                node.has(FLAVOR) ? Optional.of(node.get(FLAVOR).asText()) : Optional.empty());
    }
}
