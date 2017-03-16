/*
 * Copyright 2016 Red Hat Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package enmasse.address.controller.api.v3.amqp;

import com.fasterxml.jackson.databind.ObjectMapper;
import enmasse.address.controller.api.TestAddressManager;
import enmasse.address.controller.api.TestAddressManagerFactory;
import enmasse.address.controller.api.v3.Address;
import enmasse.address.controller.api.v3.AddressList;
import enmasse.address.controller.api.v3.ApiHandler;
import enmasse.address.controller.model.Destination;
import enmasse.address.controller.model.DestinationGroup;
import enmasse.address.controller.model.InstanceId;
import org.apache.qpid.proton.amqp.messaging.AmqpValue;
import org.apache.qpid.proton.amqp.messaging.ApplicationProperties;
import org.apache.qpid.proton.message.Message;
import org.junit.Before;
import org.junit.Test;
import org.mockito.internal.util.collections.Sets;

import java.io.IOException;
import java.util.*;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.*;

public class AmqpAddressingApiTest {
    private static ObjectMapper mapper = new ObjectMapper();
    private AddressingService addressingService;
    private TestAddressManagerFactory instanceManager;
    private TestAddressManager addressManager;

    @Before
    public void setup() {
        addressManager = new TestAddressManager();
        instanceManager = new TestAddressManagerFactory();
        instanceManager.addManager(InstanceId.withId("myinstance"), addressManager);
        addressingService = new AddressingService(new ApiHandler(instanceManager));
        addressManager.destinationsUpdated(Sets.newSet(
            createGroup(new Destination("addr1", "addr1", false, false, Optional.empty(), Optional.empty())),
            createGroup(new Destination("queue1", "queue1", true, false, Optional.of("vanilla"), Optional.empty()))));
    }

    @Test
    public void testList() throws IOException {
        Message response = doRequest("GET", "", Optional.empty());
        Set<Destination> data = decodeAs(AddressList.class, response).getDestinations();

        assertThat(data.size(), is(2));
        assertDestinationName(data, "addr1");
        assertDestinationName(data, "queue1");
    }

    private static <T> T decodeAs(Class<T> clazz, Message message) throws IOException {
        System.out.println("Decode body: " + ((AmqpValue)message.getBody()).getValue());
        return mapper.readValue((String)((AmqpValue)message.getBody()).getValue(), clazz);
    }

    private Message doRequest(String method, Object body, Optional<String> addressProperty) throws IOException {
        Message message = Message.Factory.create();
        message.setAddress("$address");
        message.setContentType("application/json");
        message.setBody(new AmqpValue(mapper.writeValueAsString(body)));
        Map<String, String> properties = new LinkedHashMap<>();
        properties.put("method", method);
        addressProperty.ifPresent(address -> properties.put("address", address));
        message.setApplicationProperties(new ApplicationProperties(properties));

        return addressingService.handleMessage(message);
    }

    @Test
    public void testGet() throws IOException {
        Message response = doRequest("GET", "", Optional.of("queue1"));
        Destination data = decodeAs(Address.class, response).getDestination();

        assertThat(data.address(), is("queue1"));
        assertTrue(data.storeAndForward());
        assertFalse(data.multicast());
        assertThat(data.flavor().get(), is("vanilla"));
    }

    @Test(expected = RuntimeException.class)
    public void testGetException() throws IOException {
        addressManager.throwException = true;
        doRequest("GET", "", Optional.empty());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testGetUnknown() throws IOException {
        doRequest("GET", "", Optional.of("unknown"));
    }


    @Test
    public void testPut() throws IOException {
        Set<Destination> input = Sets.newSet(
                new Destination("addr2", "addr2", false, false, Optional.empty(), Optional.empty()),
                new Destination("topic", "topic", true, true, Optional.of("vanilla"), Optional.empty()));

        Message response = doRequest("PUT", AddressList.fromSet(input), Optional.empty());
        Set<Destination> result = decodeAs(AddressList.class, response).getDestinations();

        assertThat(result, is(input));

        assertThat(addressManager.destinationList.size(), is(2));
        assertDestination(new Destination("addr2", "addr2", false, false, Optional.empty(), Optional.empty()));
        assertDestination(new Destination("topic", "topic", true, true, Optional.of("vanilla"), Optional.empty()));
        assertNotDestination(new Destination("addr1", "addr1", false, false, Optional.empty(), Optional.empty()));
    }

    @Test(expected = RuntimeException.class)
    public void testPutException() throws IOException {
        addressManager.throwException = true;
        doRequest("PUT", AddressList.fromSet(Collections.singleton( new Destination("newaddr", "newaddr", true, false, Optional.of("vanilla"), Optional.empty()))), Optional.empty());
    }

    @Test
    public void testDelete() throws IOException {

        Message response = doRequest("DELETE", "", Optional.of("addr1"));
        Set<Destination> result = decodeAs(AddressList.class, response).getDestinations();

        assertThat(result.size(), is(1));
        assertThat(result.iterator().next().address(), is("queue1"));

        assertThat(addressManager.destinationList.size(), is(1));
        assertDestination(new Destination("queue1", "queue1", true, false, Optional.of("vanilla"), Optional.empty()));
        assertNotDestination(new Destination("addr1", "addr1", false, false, Optional.empty(), Optional.empty()));
    }

    @Test(expected = RuntimeException.class)
    public void testDeleteException() throws IOException {
        addressManager.throwException = true;
        doRequest("DELETE", "", Optional.of("throw"));
    }

    private static DestinationGroup createGroup(Destination destination) {
        return new DestinationGroup(destination.address(), Collections.singleton(destination));
    }

    @Test
    public void testAppend() throws IOException {
        Message response = doRequest("POST", new Address(new Destination("addr2", "addr2", false, false, Optional.empty(), Optional.empty())), Optional.empty());
        Set<Destination> result = decodeAs(AddressList.class, response).getDestinations();

        assertThat(result.size(), is(3));
        assertDestinationName(result, "addr1");
        assertDestinationName(result, "queue1");
        assertDestinationName(result, "addr2");

        assertThat(addressManager.destinationList.size(), is(3));
        assertDestination(new Destination("addr2", "addr2", false, false, Optional.empty(), Optional.empty()));
        assertDestination(new Destination("queue1", "queue1", true, false, Optional.of("vanilla"), Optional.empty()));
        assertDestination(new Destination("addr1", "addr1", false, false, Optional.empty(), Optional.empty()));
    }

    private void assertDestinationName(Set<Destination> actual, String expectedAddress) {
        Destination found = null;
        for (Destination destination : actual) {
            if (destination.address().equals(expectedAddress)) {
                found = destination;
                break;
            }
        }
        assertNotNull(found);
        assertThat(found.address(), is(expectedAddress));
    }

    @Test(expected = RuntimeException.class)
    public void testAppendException() throws IOException {
        addressManager.throwException = true;
        doRequest("POST", new Address(new Destination("newaddr", "newaddr", true, false, Optional.of("vanilla"), Optional.empty())), Optional.empty());
    }

    private void assertNotDestination(Destination destination) {
        assertFalse(addressManager.destinationList.contains(destination));
    }

    private void assertDestination(Destination dest) {
        Destination actual = null;
        for (DestinationGroup group : addressManager.destinationList) {
            for (Destination d : group.getDestinations()) {
                if (d.address().equals(dest.address())) {
                    actual = d;
                    break;
                }
            }
        }
        assertNotNull(actual);
        assertTrue(actual.equals(dest));
    }
}
