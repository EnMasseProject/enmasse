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

package enmasse.address.controller.api.v3.http;

import enmasse.address.controller.api.TestAddressManager;
import enmasse.address.controller.api.TestAddressManagerFactory;
import enmasse.address.controller.api.v3.ApiHandler;
import enmasse.address.controller.model.Destination;
import enmasse.address.controller.model.DestinationGroup;
import enmasse.address.controller.api.v3.Address;
import enmasse.address.controller.api.v3.AddressList;
import enmasse.address.controller.model.InstanceId;
import org.junit.Before;
import org.junit.Test;
import org.mockito.internal.util.collections.Sets;

import javax.ws.rs.core.Response;
import java.util.Collections;
import java.util.Optional;
import java.util.Set;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.*;

public class HttpAddressingApiTest {
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
        Response response = addressingService.getAddress("queue1");
        assertThat(response.getStatus(), is(200));
        Destination data = ((Address)response.getEntity()).getDestination();

        assertThat(data.address(), is("queue1"));
        assertTrue(data.storeAndForward());
        assertFalse(data.multicast());
        assertThat(data.flavor().get(), is("vanilla"));
    }

    @Test
    public void testGetException() {
        addressManager.throwException = true;
        Response response = addressingService.listAddresses();
        assertThat(response.getStatus(), is(500));
    }

    @Test
    public void testGetUnknown() {
        Response response = addressingService.getAddress("unknown");
        assertThat(response.getStatus(), is(404));
    }


    @Test
    public void testPut() {
        Set<Destination> input = Sets.newSet(
                new Destination("addr2", "addr2", false, false, Optional.empty(), Optional.empty()),
                new Destination("topic", "topic", true, true, Optional.of("vanilla"), Optional.empty()));

        Response response = addressingService.putAddresses(AddressList.fromSet(input));
        Set<Destination> result = ((AddressList)response.getEntity()).getDestinations();

        assertThat(result, is(input));

        assertThat(addressManager.destinationList.size(), is(2));
        assertDestination(new Destination("addr2", "addr2", false, false, Optional.empty(), Optional.empty()));
        assertDestination(new Destination("topic", "topic", true, true, Optional.of("vanilla"), Optional.empty()));
        assertNotDestination(new Destination("addr1", "addr1", false, false, Optional.empty(), Optional.empty()));
    }

    @Test
    public void testPutException() {
        addressManager.throwException = true;
        Response response = addressingService.putAddresses(AddressList.fromSet(Collections.singleton(
                    new Destination("newaddr", "newaddr", true, false, Optional.of("vanilla"), Optional.empty()))));
        assertThat(response.getStatus(), is(500));
    }

    @Test
    public void testDelete() {

        Response response = addressingService.deleteAddress("addr1");
        Set<Destination> result = ((AddressList)response.getEntity()).getDestinations();

        assertThat(result.size(), is(1));
        assertThat(result.iterator().next().address(), is("queue1"));

        assertThat(addressManager.destinationList.size(), is(1));
        assertDestination(new Destination("queue1", "queue1", true, false, Optional.of("vanilla"), Optional.empty()));
        assertNotDestination(new Destination("addr1", "addr1", false, false, Optional.empty(), Optional.empty()));
    }

    @Test
    public void testDeleteException() {
        addressManager.throwException = true;
        Response response = addressingService.deleteAddress("throw");
        assertThat(response.getStatus(), is(500));
    }

    private static DestinationGroup createGroup(Destination destination) {
        return new DestinationGroup(destination.address(), Collections.singleton(destination));
    }

    @Test
    public void testAppend() {
        Response response = addressingService.appendAddress(new Address(new Destination("addr2", "addr2",
                false, false, Optional.empty(), Optional.empty())));
        Set<Destination> result = ((AddressList)response.getEntity()).getDestinations();

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

    @Test
    public void testAppendException() {
        addressManager.throwException = true;
        Response response = addressingService.appendAddress(new Address(
                new Destination("newaddr", "newaddr", true, false, Optional.of("vanilla"), Optional.empty())));
        assertThat(response.getStatus(), is(500));
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
