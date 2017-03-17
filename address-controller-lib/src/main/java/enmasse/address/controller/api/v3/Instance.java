package enmasse.address.controller.api.v3;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.node.ObjectNode;
import enmasse.address.controller.model.InstanceId;

import java.io.IOException;
import java.util.Optional;

@JsonSerialize(using = Instance.Serializer.class)
@JsonDeserialize(using = Instance.Deserializer.class)
public class Instance {
    private static final ObjectMapper mapper = new ObjectMapper();
    private final enmasse.address.controller.model.Instance instance;

    public Instance(enmasse.address.controller.model.Instance instance) {
        this.instance = instance;
    }


    public enmasse.address.controller.model.Instance getInstance() {
        return instance;
    }

    public static String kind() {
        return Instance.class.getSimpleName();
    }

    protected static class Deserializer extends JsonDeserializer<Instance> {

        @Override
        public Instance deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
            ObjectNode node = mapper.readValue(p, ObjectNode.class);
            ObjectNode spec = (ObjectNode) node.get(ResourceKeys.SPEC);
            ObjectNode metadata = (ObjectNode) node.get(ResourceKeys.METADATA);
            String name = metadata.get(ResourceKeys.NAME).asText();

            InstanceId id = spec.has(ResourceKeys.NAMESPACE) ? InstanceId.withIdAndNamespace(name, spec.get(ResourceKeys.NAMESPACE).asText()) : InstanceId.withId(name);
            enmasse.address.controller.model.Instance.Builder instance = new enmasse.address.controller.model.Instance.Builder(id);

            instance.messagingHost(Optional.ofNullable(spec.get(ResourceKeys.MESSAGING_HOST)).map(JsonNode::asText));
            instance.mqttHost(Optional.ofNullable(spec.get(ResourceKeys.MQTT_HOST)).map(JsonNode::asText));
            instance.consoleHost(Optional.ofNullable(spec.get(ResourceKeys.CONSOLE_HOST)).map(JsonNode::asText));

            return new Instance(instance.build());
        }
    }

    protected static class Serializer extends JsonSerializer<Instance> {
        @Override
        public void serialize(Instance value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
            ObjectNode node = mapper.createObjectNode();

            node.put(ResourceKeys.KIND, Instance.kind());
            node.put(ResourceKeys.APIVERSION, "v3");

            ObjectNode metadata = node.putObject(ResourceKeys.METADATA);
            metadata.put(ResourceKeys.NAME, value.instance.id().getId());

            ObjectNode spec = node.putObject(ResourceKeys.SPEC);
            spec.put(ResourceKeys.NAMESPACE, value.instance.id().getNamespace());
            value.instance.messagingHost().ifPresent(h -> spec.put(ResourceKeys.MESSAGING_HOST, h));
            value.instance.mqttHost().ifPresent(h -> spec.put(ResourceKeys.MQTT_HOST, h));
            value.instance.consoleHost().ifPresent(h -> spec.put(ResourceKeys.CONSOLE_HOST, h));

            mapper.writeValue(gen, node);
        }
    }
}
