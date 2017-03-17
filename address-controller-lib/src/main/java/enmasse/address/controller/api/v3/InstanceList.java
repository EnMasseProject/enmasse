package enmasse.address.controller.api.v3;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import enmasse.address.controller.model.Instance;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

@JsonSerialize(using = InstanceList.Serializer.class)
@JsonDeserialize(using = InstanceList.Deserializer.class)
public class InstanceList {
    private static final ObjectMapper mapper = new ObjectMapper();

    private final Set<Instance> instances;

    private InstanceList(Set<Instance> instances) {
        this.instances = instances;
    }

    public static InstanceList fromSet(Set<Instance> instances) {
        return new InstanceList(instances);
    }

    public Set<Instance> getInstances() {
        return instances;
    }

    public static String kind() {
        return InstanceList.class.getSimpleName();
    }

    protected static class Deserializer extends JsonDeserializer<InstanceList> {

        @Override
        public InstanceList deserialize(JsonParser p, DeserializationContext ctxt) throws IOException, JsonProcessingException {
            ObjectNode node = mapper.readValue(p, ObjectNode.class);

            ArrayNode items = node.has(ResourceKeys.ITEMS) ? (ArrayNode) node.get(ResourceKeys.ITEMS) : mapper.createArrayNode();
            Set<Instance> instances = new HashSet<>();
            for (int i = 0; i < items.size(); i++) {
                instances.add(mapper.convertValue(items.get(i), enmasse.address.controller.api.v3.Instance.class).getInstance());
            }
            return new InstanceList(instances);
        }
    }

    protected static class Serializer extends JsonSerializer<InstanceList> {
        @Override
        public void serialize(InstanceList value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
            ObjectNode node = mapper.createObjectNode();

            node.put(ResourceKeys.KIND, kind());
            node.put(ResourceKeys.APIVERSION, "v3");

            ArrayNode items = node.putArray(ResourceKeys.ITEMS);

            for (Instance instance : value.instances) {
                items.add(mapper.valueToTree(new enmasse.address.controller.api.v3.Instance(instance)));
            }
            mapper.writeValue(gen, node);
        }
    }
}
