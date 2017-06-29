/*
 * Copyright 2017 Red Hat Inc.
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
package enmasse.controller.address.v3;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import enmasse.controller.model.Destination;

import java.io.IOException;
import java.util.LinkedHashSet;
import java.util.Set;

@JsonSerialize(using = AddressList.Serializer.class)
@JsonDeserialize(using = AddressList.Deserializer.class)
public class AddressList {
    private static final ObjectMapper mapper = new ObjectMapper();

    private final Set<Destination> destinations;

    private AddressList(Set<Destination> destinations) {
        this.destinations = destinations;
    }

    public static AddressList fromSet(Set<Destination> destinations) {
        return new AddressList(destinations);
    }

    public Set<Destination> getDestinations() {
        return destinations;
    }

    public static String kind() {
        return AddressList.class.getSimpleName();
    }

    protected static class Deserializer extends JsonDeserializer<AddressList> {

        @Override
        public AddressList deserialize(JsonParser p, DeserializationContext ctxt) throws IOException, JsonProcessingException {
            ObjectNode node = mapper.readValue(p, ObjectNode.class);

            ArrayNode items = node.has(ResourceKeys.ITEMS) ? (ArrayNode) node.get(ResourceKeys.ITEMS) : mapper.createArrayNode();
            Set<Destination> destinations = new LinkedHashSet<>();

            for (int i = 0; i < items.size(); i++) {
                destinations.add(mapper.convertValue(items.get(i), Address.class).getDestination());
            }
            return new AddressList(destinations);
        }
    }

    protected static class Serializer extends JsonSerializer<AddressList> {
        @Override
        public void serialize(AddressList value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
            ObjectNode node = mapper.createObjectNode();

            node.put(ResourceKeys.KIND, kind());
            node.put(ResourceKeys.APIVERSION, "v3");

            ArrayNode items = node.putArray(ResourceKeys.ITEMS);

            for (Destination destination : value.destinations) {
                items.add(mapper.valueToTree(new Address(destination)));
            }
            mapper.writeValue(gen, node);
        }
    }
}
