package enmasse.controller.api.v3;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import enmasse.controller.model.Destination;
import enmasse.controller.model.DestinationGroup;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

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

    public static AddressList fromGroups(Set<DestinationGroup> destinationGroups) {
        return fromSet(destinationGroups.stream()
                .flatMap(g -> g.getDestinations().stream())
                .collect(Collectors.toSet()));
    }

    public Set<Destination> getDestinations() {
        return destinations;
    }

    public Set<DestinationGroup> getDestinationGroups() {
        Map<String, DestinationGroup.Builder> groupMap = new LinkedHashMap<>();
        for (Destination destination : destinations) {
            if (!groupMap.containsKey(destination.group())) {
                groupMap.put(destination.group(), new DestinationGroup.Builder(destination.group()));
            }
            groupMap.get(destination.group()).destination(destination);
        }
        return groupMap.values().stream()
                .map(DestinationGroup.Builder::build)
                .collect(Collectors.toSet());
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
                destinations.add(mapper.convertValue(items.get(i), enmasse.controller.api.v3.Address.class).getDestination());
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
                items.add(mapper.valueToTree(new enmasse.controller.api.v3.Address(destination)));
            }
            mapper.writeValue(gen, node);
        }
    }
}
