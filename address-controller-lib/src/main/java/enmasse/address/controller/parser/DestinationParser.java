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

import java.io.IOException;
import java.util.*;

/**
 * Parser for the addressing config.
 */
public class DestinationParser {
    private static final String STORE_AND_FORWARD = "store_and_forward";
    private static final String MULTICAST = "multicast";
    private static final String FLAVOR = "flavor";

    public static Set<Destination> parse(JsonNode root) throws IOException {
        Set<Destination> destinations = new HashSet<>();
        Iterator<Map.Entry<String, JsonNode>> it = root.fields();
        while (it.hasNext()) {
            Map.Entry<String, JsonNode> entry = it.next();
            JsonNode node = entry.getValue();
            Destination destination = new Destination(
                    entry.getKey(),
                    node.get(STORE_AND_FORWARD).asBoolean(),
                    node.get(MULTICAST).asBoolean(),
                    node.has(FLAVOR) ? Optional.of(node.get(FLAVOR).asText()) : Optional.empty());
            destinations.add(destination);
        }

        return destinations;
    }
}
