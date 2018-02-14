/*
 * Copyright 2016-2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package io.enmasse.controller.api.v3.http;


import io.enmasse.k8s.api.TestAddressApi;
import io.enmasse.k8s.api.TestAddressSpaceApi;

import static org.hamcrest.CoreMatchers.is;

public class HttpAddressingApiTest {
    //private AddressingService addressingService;
    private TestAddressSpaceApi instanceManager;
    private TestAddressApi addressSpace;

    /*
    @Before
    public void setup() {
        instanceManager = new TestAddressSpaceApi();
        instanceManager.create(new Instance.Builder(AddressSpaceId.withId("myinstance")).build());
        addressSpace = new TestAddressApi();
        addressSpace.setDestinations(Sets.newSet(
                new Destination("addr1", "addr1", false, false, Optional.empty(), Optional.empty(), status),
                new Destination("queue1", "queue1", true, false, Optional.of("vanilla"), Optional.empty(), status)));
        TestAddressManager addressManager = new TestAddressManager();
        addressManager.addManager(AddressSpaceId.withId("myinstance"), addressSpace);

        addressingService = new AddressingService(AddressSpaceId.withId("myinstance"), new AddressApiHelper(instanceManager, addressManager));
    }

    @Test
    public void testList() {
        Response response = addressingService.listAddresses();
        assertThat(response.getStatus(), is(200));
        Set<Destination> data = ((AddressList)response.getEntity()).getDestinations();

        assertThat(data.size(), is(2));
        assertDestinationName(data, "addr1");
        assertDestinationName(data, "queue1");
    }

    @Test
    public void testGet() {
        Response response = addressingService.getAddressWithName("queue1");
        assertThat(response.getStatus(), is(200));
        Destination data = ((Address)response.getEntity()).getDestination();

        assertThat(data.address(), is("queue1"));
        assertTrue(data.storeAndForward());
        assertFalse(data.multicast());
        assertThat(data.flavor().get(), is("vanilla"));
    }

    @Test
    public void testGetException() {
        addressSpace.throwException = true;
        Response response = addressingService.listAddresses();
        assertThat(response.getStatus(), is(500));
    }

    @Test
    public void testGetUnknown() {
        Response response = addressingService.getAddressWithName("unknown");
        assertThat(response.getStatus(), is(404));
    }


    @Test
    public void testPut() {
        Set<Destination> input = Sets.newSet(
                new Destination("addr2", "addr2", false, false, Optional.empty(), Optional.empty(), status),
                new Destination("topic", "topic", true, true, Optional.of("vanilla"), Optional.empty(), status));

        Response response = addressingService.putAddresses(AddressList.fromSet(input));
        Set<Destination> result = ((AddressList)response.getEntity()).getDestinations();

        assertThat(result, is(input));

        assertThat(addressSpace.getDestinations().size(), is(2));
        assertDestination(new Destination("addr2", "addr2", false, false, Optional.empty(), Optional.empty(), status));
        assertDestination(new Destination("topic", "topic", true, true, Optional.of("vanilla"), Optional.empty(), status));
        assertNotDestination(new Destination("addr1", "addr1", false, false, Optional.empty(), Optional.empty(), status));
    }

    @Test
    public void testPutException() {
        addressSpace.throwException = true;
        Response response = addressingService.putAddresses(AddressList.fromSet(Collections.singleton(
                    new Destination("newaddr", "newaddr", true, false, Optional.of("vanilla"), Optional.empty(), status))));
        assertThat(response.getStatus(), is(500));
    }

    @Test
    public void testDelete() {

        Response response = addressingService.deleteAddress("addr1");
        Set<Destination> result = ((AddressList)response.getEntity()).getDestinations();

        assertThat(result.size(), is(1));
        assertThat(result.iterator().next().address(), is("queue1"));

        assertThat(addressSpace.getDestinations().size(), is(1));
        assertDestination(new Destination("queue1", "queue1", true, false, Optional.of("vanilla"), Optional.empty(), status));
        assertNotDestination(new Destination("addr1", "addr1", false, false, Optional.empty(), Optional.empty(), status));
    }

    @Test
    public void testDeleteException() {
        addressSpace.throwException = true;
        Response response = addressingService.deleteAddress("throw");
        assertThat(response.getStatus(), is(500));
    }

    private static Set<Destination> createGroup(Destination destination) {
        return Collections.singleton(destination);
    }

    @Test
    public void testAppend() {
        Response response = addressingService.appendAddress(new Address(new Destination("addr2", "addr2",
                false, false, Optional.empty(), Optional.empty(), status)));
        Set<Destination> result = ((AddressList)response.getEntity()).getDestinations();

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

    @Test
    public void testAppendException() {
        addressSpace.throwException = true;
        Response response = addressingService.appendAddress(new Address(
                new Destination("newaddr", "newaddr", true, false, Optional.of("vanilla"), Optional.empty(), status)));
        assertThat(response.getStatus(), is(500));
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
