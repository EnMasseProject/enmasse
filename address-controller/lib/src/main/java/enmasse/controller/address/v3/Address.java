package enmasse.controller.address.v3;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.node.ObjectNode;
import enmasse.controller.model.Destination;

import java.io.IOException;
import java.util.Optional;

@JsonSerialize(using = Address.Serializer.class)
@JsonDeserialize(using = Address.Deserializer.class)
public class Address {
    private static final ObjectMapper mapper = new ObjectMapper();
    private final Destination destination;

    public Address(Destination destination) {
        this.destination = destination;
    }


    public Destination getDestination() {
        return destination;
    }

    public static String kind() {
        return Address.class.getSimpleName();
    }

    protected static class Deserializer extends JsonDeserializer<Address> {

        @Override
        public Address deserialize(JsonParser p, DeserializationContext ctxt) throws IOException, JsonProcessingException {
            ObjectNode node = mapper.readValue(p, ObjectNode.class);
            ObjectNode spec = (ObjectNode) node.get(ResourceKeys.SPEC);
            ObjectNode metadata = (ObjectNode) node.get(ResourceKeys.METADATA);
            String address = metadata.get(ResourceKeys.NAME).asText();
            String group = spec.has(ResourceKeys.GROUP) ? spec.get(ResourceKeys.GROUP).asText() : address;

            return new Address(new Destination.Builder(address, group)
                .storeAndForward(spec.get(ResourceKeys.STORE_AND_FORWARD).asBoolean())
                .multicast(spec.get(ResourceKeys.MULTICAST).asBoolean())
                .flavor(Optional.ofNullable(spec.get(ResourceKeys.FLAVOR)).map(JsonNode::asText))
                .uuid(Optional.ofNullable(metadata.get(ResourceKeys.UUID)).map(JsonNode::asText))
                .build());
        }
    }

    protected static class Serializer extends JsonSerializer<Address> {
        @Override
        public void serialize(Address value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
            ObjectNode node = mapper.createObjectNode();
            Destination destination = value.destination;

            node.put(ResourceKeys.KIND, kind());
            node.put(ResourceKeys.APIVERSION, "v3");
            node.put(ResourceKeys.STATUS, "/api/v3/status/" + destination.address());

            ObjectNode metadata = node.putObject(ResourceKeys.METADATA);
            metadata.put(ResourceKeys.NAME, destination.address());
            destination.uuid().ifPresent(u -> metadata.put(ResourceKeys.UUID, u));

            ObjectNode spec = node.putObject(ResourceKeys.SPEC);
            spec.put(ResourceKeys.STORE_AND_FORWARD, destination.storeAndForward());
            spec.put(ResourceKeys.MULTICAST, destination.multicast());
            spec.put(ResourceKeys.GROUP, destination.group());
            destination.flavor().ifPresent(f -> spec.put(ResourceKeys.FLAVOR, f));

            mapper.writeValue(gen, node);
        }
    }
}
