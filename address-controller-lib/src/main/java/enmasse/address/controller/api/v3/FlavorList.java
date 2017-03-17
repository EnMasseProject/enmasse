package enmasse.address.controller.api.v3;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import enmasse.address.controller.model.Flavor;

import java.io.IOException;
import java.util.Set;

@JsonSerialize(using = FlavorList.Serializer.class)
public class FlavorList {
    private static final ObjectMapper mapper = new ObjectMapper();

    private final Set<Flavor> flavors;

    private FlavorList(Set<Flavor> flavors) {
        this.flavors = flavors;
    }

    public static FlavorList fromSet(Set<Flavor> flavors) {
        return new FlavorList(flavors);
    }

    public Set<Flavor> getFlavors() {
        return flavors;
    }

    public static String kind() {
        return FlavorList.class.getSimpleName();
    }

    protected static class Serializer extends JsonSerializer<FlavorList> {
        @Override
        public void serialize(FlavorList value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
            ObjectNode node = mapper.createObjectNode();

            node.put(ResourceKeys.KIND, kind());
            node.put(ResourceKeys.APIVERSION, "v3");

            ArrayNode items = node.putArray(ResourceKeys.ITEMS);

            for (Flavor flavor : value.flavors) {
                items.add(mapper.valueToTree(new enmasse.address.controller.api.v3.Flavor(flavor)));
            }
            mapper.writeValue(gen, node);
        }
    }
}
