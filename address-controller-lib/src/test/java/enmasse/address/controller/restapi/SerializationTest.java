package enmasse.address.controller.restapi;

import com.fasterxml.jackson.databind.ObjectMapper;
import enmasse.address.controller.model.Destination;
import enmasse.address.controller.restapi.v3.Address;
import enmasse.address.controller.restapi.v3.AddressList;
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
}
