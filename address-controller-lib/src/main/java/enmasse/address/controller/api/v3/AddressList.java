package enmasse.address.controller.api.v3;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.node.ObjectNode;
import enmasse.address.controller.model.Destination;
import enmasse.address.controller.model.DestinationGroup;

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

    protected static class Deserializer extends JsonDeserializer<AddressList> {

        @Override
        public AddressList deserialize(JsonParser p, DeserializationContext ctxt) throws IOException, JsonProcessingException {
            ObjectNode node = mapper.readValue(p, ObjectNode.class);

            ObjectNode addresses = node.has(ResourceKeys.ADDRESSES) ? (ObjectNode) node.get(ResourceKeys.ADDRESSES) : mapper.createObjectNode();
            Iterator<Map.Entry<String, JsonNode>> it = addresses.fields();
            Set<Destination> destinations = new LinkedHashSet<>();
            while (it.hasNext()) {
                Map.Entry<String, JsonNode> entry = it.next();
                ObjectNode value = (ObjectNode) entry.getValue();
                String address = entry.getKey();
                String group = value.has(ResourceKeys.GROUP) ? value.get(ResourceKeys.GROUP).asText() : address;
                destinations.add(new Destination.Builder(address, group)
                        .storeAndForward(value.get(ResourceKeys.STORE_AND_FORWARD).asBoolean())
                        .multicast(value.get(ResourceKeys.MULTICAST).asBoolean())
                        .flavor(Optional.ofNullable(value.get(ResourceKeys.FLAVOR)).map(JsonNode::asText))
                        .build());
            }
            return new AddressList(destinations);
        }
    }

    protected static class Serializer extends JsonSerializer<AddressList> {
        @Override
        public void serialize(AddressList value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
            ObjectNode node = mapper.createObjectNode();
            Set<Destination> destinations = value.destinations;

            node.put(ResourceKeys.KIND, "AddressList");
            node.put(ResourceKeys.APIVERSION, "v3");

            ObjectNode addresses = node.putObject(ResourceKeys.ADDRESSES);

            for (Destination destination : destinations) {
                ObjectNode address = addresses.putObject(destination.address());
                address.put(ResourceKeys.STORE_AND_FORWARD, destination.storeAndForward());
                address.put(ResourceKeys.MULTICAST, destination.multicast());
                address.put(ResourceKeys.GROUP, destination.group());
                destination.flavor().ifPresent(f -> address.put(ResourceKeys.FLAVOR, f));
            }
            mapper.writeValue(gen, node);
        }
    }
}
