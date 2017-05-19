package enmasse.controller.api.v3;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import enmasse.controller.address.v3.Address;
import enmasse.controller.address.v3.AddressList;
import enmasse.controller.flavor.v3.FlavorList;
import enmasse.controller.instance.v3.InstanceList;
import enmasse.controller.model.Destination;
import enmasse.controller.model.Flavor;
import enmasse.controller.model.Instance;
import enmasse.controller.model.InstanceId;
import org.junit.Test;
import org.mockito.internal.util.collections.Sets;

import java.io.IOException;
import java.util.Optional;
import java.util.Set;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;

public class SerializationTest {
    private static final ObjectMapper mapper = new ObjectMapper();

    @Test
    public void testSerializeAddress() throws IOException {
        Destination destination = new Destination.Builder("addr1", "group0")
                .multicast(false)
                .storeAndForward(true)
                .flavor(Optional.of("myflavor"))
                .uuid(Optional.of("uuid1"))
                .build();

        Address address = new Address(destination);
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

        String serialized = mapper.writeValueAsString(list);

        Set<Destination> deserialized = mapper.readValue(serialized, AddressList.class).getDestinations();
        assertThat(deserialized, is(destinations));
    }

    @Test
    public void testSerializeFlavorList() throws IOException {
        Set<Flavor> flavors = Sets.newSet(
                new Flavor.Builder("flavor1", "template1").type("queue").description("Simple queue").build(),
                new Flavor.Builder("flavor2", "template2").type("topic").description("Simple topic").build());

        FlavorList list = FlavorList.fromSet(flavors);

        String serialized = mapper.writeValueAsString(list);

        ObjectNode deserialized = mapper.readValue(serialized, ObjectNode.class);
        assertThat(deserialized.get("kind").asText(), is("FlavorList"));
        assertThat(deserialized.get("items").size(), is(2));
        assertThat(deserialized.get("items").get(0).get("spec").get("type").asText(), is("queue"));
        assertThat(deserialized.get("items").get(0).get("spec").get("description").asText(), is("Simple queue"));
        assertThat(deserialized.get("items").get(1).get("spec").get("type").asText(), is("topic"));
        assertThat(deserialized.get("items").get(1).get("spec").get("description").asText(), is("Simple topic"));
    }

    @Test
    public void testSerializeFlavor() throws IOException {
        Flavor flavor = new Flavor.Builder("flavor1", "template1").type("queue").description("Simple queue").build();

        String serialized = mapper.writeValueAsString(new enmasse.controller.flavor.v3.Flavor(flavor));

        ObjectNode deserialized = mapper.readValue(serialized, ObjectNode.class);
        assertThat(deserialized.get("kind").asText(), is("Flavor"));
        assertThat(deserialized.get("metadata").get("name").asText(), is("flavor1"));
        assertThat(deserialized.get("spec").get("type").asText(), is("queue"));
        assertThat(deserialized.get("spec").get("description").asText(), is("Simple queue"));
    }

    @Test
    public void testSerializeInstance() throws IOException {
        Instance instance = new Instance.Builder(InstanceId.withIdAndNamespace("myid", "mynamespace"))
                .messagingHost(Optional.of("messaging.com"))
                .mqttHost(Optional.of("mqtt.com"))
                .build();

        String serialized = mapper.writeValueAsString(new enmasse.controller.instance.v3.Instance(instance));

        Instance deserialized = mapper.readValue(serialized, enmasse.controller.instance.v3.Instance.class).getInstance();

        assertThat(deserialized.id(), is(instance.id()));
        assertThat(deserialized.messagingHost(), is(instance.messagingHost()));
        assertThat(deserialized.mqttHost(), is(instance.mqttHost()));
        assertThat(deserialized.consoleHost(), is(instance.consoleHost()));
    }

    @Test
    public void testSerializeInstanceList() throws IOException {
        Instance i1 = new Instance.Builder(InstanceId.withIdAndNamespace("myid", "mynamespace"))
                .messagingHost(Optional.of("messaging.com"))
                .mqttHost(Optional.of("mqtt.com"))
                .build();

        Instance i2 = new Instance.Builder(InstanceId.withIdAndNamespace("myother", "bar"))
                .messagingHost(Optional.of("mymessaging.com"))
                .consoleHost(Optional.of("myconsole.com"))
                .build();

        String serialized = mapper.writeValueAsString(InstanceList.fromSet(Sets.newSet(i1, i2)));

        Set<Instance> deserialized = mapper.readValue(serialized, InstanceList.class).getInstances();

        assertInstance(deserialized, i1);
        assertInstance(deserialized, i2);
    }

    private void assertInstance(Set<Instance> deserialized, Instance expected) {
        Instance found = null;
        for (Instance instance : deserialized) {
            if (instance.id().equals(expected.id())) {
                found = instance;
                break;
            }

        }
        assertNotNull(found);
        assertThat(found.id(), is(expected.id()));
        assertThat(found.messagingHost(), is(expected.messagingHost()));
        assertThat(found.mqttHost(), is(expected.mqttHost()));
        assertThat(found.consoleHost(), is(expected.consoleHost()));
    }
}
