package enmasse.address.controller.restapi;

import com.fasterxml.jackson.databind.ObjectMapper;
import enmasse.address.controller.model.Destination;
import enmasse.address.controller.restapi.v3.Address;
import org.junit.Test;

import java.io.IOException;
import java.util.Optional;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

public class SerializationTest {
    @Test
    public void testSerialize() throws IOException {
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
}
