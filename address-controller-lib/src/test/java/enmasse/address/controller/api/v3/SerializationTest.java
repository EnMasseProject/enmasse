package enmasse.address.controller.api.v3;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import enmasse.address.controller.model.Destination;
import enmasse.address.controller.model.Flavor;
import org.junit.Test;
import org.mockito.internal.util.collections.Sets;

import java.io.IOException;
import java.util.Optional;
import java.util.Set;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

public class SerializationTest {
    @Test
    public void testSerializeAddress() throws IOException {
        Destination destination = new Destination.Builder("addr1", "group0")
                .multicast(false)
                .storeAndForward(true)
                .flavor(Optional.of("myflavor"))
                .uuid(Optional.of("uuid1"))
                .build();

        Address address = new Address(destination);
        ObjectMapper mapper = new ObjectMapper();
        String serialized = mapper.writeValueAsString(address);

        Destination next = mapper.readValue(serialized, Address.class).getDestination();
        assertThat(next.address(), is(destination.address()));
        assertThat(next.group(), is(destination.group()));
        assertThat(next.storeAndForward(), is(destination.storeAndForward()));
        assertThat(next.multicast(), is(destination.multicast()));
        assertThat(next.flavor(), is(destination.flavor()));
        assertThat(next.uuid(), is(destination.uuid()));
    }

    @Test
    public void testSerializeAddressList() throws IOException {
        Set<Destination> destinations = Sets.newSet(
                new Destination.Builder("direct1", "group1").multicast(false).storeAndForward(false).build(),
                new Destination.Builder("queue1", "group2").multicast(false).storeAndForward(true).flavor(Optional.of("vanilla")).build());

        AddressList list = AddressList.fromSet(destinations);

        ObjectMapper mapper = new ObjectMapper();
        String serialized = mapper.writeValueAsString(list);

        Set<Destination> deserialized = mapper.readValue(serialized, AddressList.class).getDestinations();
        assertThat(deserialized, is(destinations));
    }

    @Test
    public void testSerializeFlavorList() throws IOException {
        Set<enmasse.address.controller.model.Flavor> flavors = Sets.newSet(
                new Flavor.Builder("flavor1", "template1").type("queue").description("Simple queue").build(),
                new Flavor.Builder("flavor2", "template2").type("topic").description("Simple topic").build());

        FlavorList list = FlavorList.fromSet(flavors);

        ObjectMapper mapper = new ObjectMapper();
        String serialized = mapper.writeValueAsString(list);

        ObjectNode deserialized = mapper.readValue(serialized, ObjectNode.class);
        assertThat(deserialized.get("kind").asText(), is("FlavorList"));
        assertThat(deserialized.get("flavors").size(), is(2));
        assertThat(deserialized.get("flavors").get("flavor1").get("type").asText(), is("queue"));
        assertThat(deserialized.get("flavors").get("flavor1").get("description").asText(), is("Simple queue"));
        assertThat(deserialized.get("flavors").get("flavor2").get("type").asText(), is("topic"));
        assertThat(deserialized.get("flavors").get("flavor2").get("description").asText(), is("Simple topic"));
    }

    @Test
    public void testSerializeFlavor() throws IOException {
        Flavor flavor = new Flavor.Builder("flavor1", "template1").type("queue").description("Simple queue").build();

        ObjectMapper mapper = new ObjectMapper();
        String serialized = mapper.writeValueAsString(new enmasse.address.controller.api.v3.Flavor(flavor));

        ObjectNode deserialized = mapper.readValue(serialized, ObjectNode.class);
        assertThat(deserialized.get("kind").asText(), is("Flavor"));
        assertThat(deserialized.get("metadata").get("name").asText(), is("flavor1"));
        assertThat(deserialized.get("spec").get("type").asText(), is("queue"));
        assertThat(deserialized.get("spec").get("description").asText(), is("Simple queue"));
    }
}
