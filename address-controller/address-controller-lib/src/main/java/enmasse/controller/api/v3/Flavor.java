package enmasse.controller.api.v3;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.io.IOException;

@JsonSerialize(using = Flavor.Serializer.class)
public class Flavor {
    private static final ObjectMapper mapper = new ObjectMapper();
    private final enmasse.controller.model.Flavor flavor;

    public Flavor(enmasse.controller.model.Flavor flavor) {
        this.flavor = flavor;
    }


    public enmasse.controller.model.Flavor getFlavor() {
        return flavor;
    }

    public static String kind() {
        return Flavor.class.getSimpleName();
    }

    protected static class Serializer extends JsonSerializer<Flavor> {
        @Override
        public void serialize(Flavor value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
            ObjectNode node = mapper.createObjectNode();
            enmasse.controller.model.Flavor flavor = value.flavor;

            node.put(ResourceKeys.KIND, kind());
            node.put(ResourceKeys.APIVERSION, "v3");

            ObjectNode metadata = node.putObject(ResourceKeys.METADATA);
            metadata.put(ResourceKeys.NAME, flavor.name());
            flavor.uuid().ifPresent(u -> metadata.put(ResourceKeys.UUID, u));

            ObjectNode spec = node.putObject(ResourceKeys.SPEC);
            spec.put(ResourceKeys.TYPE, flavor.type());
            spec.put(ResourceKeys.DESCRIPTION, flavor.description());

            mapper.writeValue(gen, node);
        }
    }
}
