/*
 * Copyright 2016-2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package io.enmasse.controller.api.v3.amqp;

import static org.hamcrest.CoreMatchers.is;

public class AmqpAddressingApiTest {
    /*
    private static ObjectMapper mapper = new ObjectMapper();
    private AddressingService addressingService;
    private TestAddressSpaceApi instanceManager;
    private TestAddressApi addressSpace;

    @Before
    public void setup() {
        instanceManager = new TestAddressSpaceApi();
        instanceManager.create(new Instance.Builder(AddressSpaceId.withId("myinstance")).build());
        addressSpace = new TestAddressApi();
        addressSpace.addDestination(new Destination("addr1", "addr1", false, false, Optional.empty(), Optional.empty(), status));
        addressSpace.addDestination(new Destination("queue1", "queue1", true, false, Optional.of("vanilla"), Optional.empty(), status));
        TestAddressManager addressManager = new TestAddressManager();
        addressManager.addManager(AddressSpaceId.withId("myinstance"), addressSpace);
        addressingService = new AddressingService(AddressSpaceId.withId("myinstance"), new AddressApiHelper(instanceManager, addressManager));

    }

    @Test
    public void testList() throws Exception {
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

    private Message doRequest(String method, Object body, Optional<String> addressProperty) throws Exception {
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
    public void testGet() throws Exception {
        Message response = doRequest("GET", "", Optional.of("queue1"));
        Destination data = decodeAs(Address.class, response).getDestination();

        assertThat(data.address(), is("queue1"));
        assertTrue(data.storeAndForward());
        assertFalse(data.multicast());
        assertThat(data.flavor().get(), is("vanilla"));
    }

    @Test(expected = RuntimeException.class)
    public void testGetException() throws Exception {
        addressSpace.throwException = true;
        doRequest("GET", "", Optional.empty());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testGetUnknown() throws Exception {
        doRequest("GET", "", Optional.of("unknown"));
    }


    @Test
    public void testPut() throws Exception {
        Set<Destination> input = Sets.newSet(
                new Destination("addr2", "addr2", false, false, Optional.empty(), Optional.empty(), status),
                new Destination("topic", "topic", true, true, Optional.of("vanilla"), Optional.empty(), status));

        Message response = doRequest("PUT", AddressList.fromSet(input), Optional.empty());
        Set<Destination> result = decodeAs(AddressList.class, response).getDestinations();

        assertThat(result, is(input));

        assertThat(addressSpace.getDestinations().size(), is(2));
        assertDestination(new Destination("addr2", "addr2", false, false, Optional.empty(), Optional.empty(), status));
        assertDestination(new Destination("topic", "topic", true, true, Optional.of("vanilla"), Optional.empty(), status));
        assertNotDestination(new Destination("addr1", "addr1", false, false, Optional.empty(), Optional.empty(), status));
    }

    @Test(expected = RuntimeException.class)
    public void testPutException() throws Exception {
        addressSpace.throwException = true;
        doRequest("PUT", AddressList.fromSet(Collections.singleton( new Destination("newaddr", "newaddr", true, false, Optional.of("vanilla"), Optional.empty(), status))), Optional.empty());
    }

    @Test
    public void testDelete() throws Exception {

        Message response = doRequest("DELETE", "", Optional.of("addr1"));
        Set<Destination> result = decodeAs(AddressList.class, response).getDestinations();

        assertThat(result.size(), is(1));
        assertThat(result.iterator().next().address(), is("queue1"));

        assertThat(addressSpace.getDestinations().size(), is(1));
        assertDestination(new Destination("queue1", "queue1", true, false, Optional.of("vanilla"), Optional.empty(), status));
        assertNotDestination(new Destination("addr1", "addr1", false, false, Optional.empty(), Optional.empty(), status));
    }

    @Test(expected = RuntimeException.class)
    public void testDeleteException() throws Exception {
        addressSpace.throwException = true;
        doRequest("DELETE", "", Optional.of("throw"));
    }

    private static Set<Destination> createGroup(Destination destination) {
        return Collections.singleton(destination);
    }

    @Test
    public void testAppend() throws Exception {
        Message response = doRequest("POST", new Address(new Destination("addr2", "addr2", false, false, Optional.empty(), Optional.empty(), status)), Optional.empty());
        Set<Destination> result = decodeAs(AddressList.class, response).getDestinations();

        assertThat(result.size(), is(3));
        assertDestinationName(result, "addr1");
        assertDestinationName(result, "queue1");
        assertDestinationName(result, "addr2");

        assertThat(addressSpace.getDestinations().size(), is(3));
        assertDestination(new Destination("addr2", "addr2", false, false, Optional.empty(), Optional.empty(), status));
        assertDestination(new Destination("queue1", "queue1", true, false, Optional.of("vanilla"), Optional.empty(), status));
        assertDestination(new Destination("addr1", "addr1", false, false, Optional.empty(), Optional.empty(), status));
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
    public void testAppendException() throws Exception {
        addressSpace.throwException = true;
        doRequest("POST", new Address(new Destination("newaddr", "newaddr", true, false, Optional.of("vanilla"), Optional.empty(), status)), Optional.empty());
    }

    private void assertNotDestination(Destination destination) {
        assertFalse(addressSpace.getDestinations().contains(destination));
    }

    private void assertDestination(Destination dest) {
        Destination actual = null;
        for (Destination d : addressSpace.getDestinations()) {
            if (d.address().equals(dest.address())) {
                actual = d;
                break;
            }
        }
        assertNotNull(actual);
        assertTrue(actual.equals(dest));
    }
    */
}
