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

package enmasse.address.controller.restapi;

import enmasse.address.controller.admin.AddressManager;
import enmasse.address.controller.model.Destination;
import enmasse.address.controller.model.DestinationGroup;
import enmasse.address.controller.restapi.common.AddressProperties;
import enmasse.address.controller.restapi.v1.RestService;
import io.vertx.core.Vertx;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.internal.util.collections.Sets;

import javax.ws.rs.container.AsyncResponse;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.*;
import java.util.function.Consumer;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class RestServiceTest {
    private RestService restService;
    private TestManager addressManager;
    private Vertx vertx;

    @Before
    public void setup() {
        vertx = Vertx.vertx();
        addressManager = new TestManager();
        restService = new RestService(addressManager, vertx);
    }

    @After
    public void teardown() {
        vertx.close();
    }

    @Test
    public void testGet() {
        addressManager.destinationsUpdated(Sets.newSet(
                createGroup(new Destination("addr1", false, false, Optional.empty())),
                createGroup(new Destination("queue1", true, false, Optional.of("vanilla")))));
        Response response = invokeRequest(restService::getAddresses);
        assertThat(response.getMediaType().toString(), is(MediaType.APPLICATION_JSON));
        assertThat(response.getStatus(), is(200));
        Map<String, AddressProperties> data = (Map<String, AddressProperties>) response.getEntity();
        assertThat(data.size(), is(2));
        assertNotNull(data.get("addr1"));
        assertNotNull(data.get("queue1"));
    }

    private Response invokeRequest(Consumer<AsyncResponse> fn) {
        AsyncResponse asyncResponse = mock(AsyncResponse.class);
        ArgumentCaptor<Response> responseArgumentCaptor = ArgumentCaptor.forClass(Response.class);
        fn.accept(asyncResponse);
        verify(asyncResponse, timeout(30_000)).resume(responseArgumentCaptor.capture());
        Response response = responseArgumentCaptor.getValue();
        assertNotNull(response);
        return response;
    }

    @Test
    public void testGetException() {
        addressManager.throwException = true;
        Response response = invokeRequest(restService::getAddresses);
        assertThat(response.getStatus(), is(500));
    }

    @Test
    public void testPut() {
        addressManager.destinationsUpdated(Sets.newSet(createGroup(new Destination("addr1", false, false, Optional.empty()))));

        Map<String, AddressProperties> input = new LinkedHashMap<>();
        input.put("addr2", new AddressProperties(false, false, null));
        input.put("topic", new AddressProperties(true,true, "vanilla"));

        Response response = invokeRequest(r -> restService.putAddresses(input, r));

        Map <String, AddressProperties > result = (Map<String, AddressProperties>) response.getEntity();
        assertThat(result, is(input));

        assertThat(addressManager.destinationList.size(), is(2));
        assertDestination(new Destination("addr2", false, false, Optional.empty()));
        assertDestination(new Destination("topic", true, true, Optional.of("vanilla")));
        assertNotDestination(new Destination("addr1", false, false, Optional.empty()));
    }

    @Test
    public void testPutException() {
        addressManager.throwException = true;
        Response response = invokeRequest(r -> restService.putAddresses(Collections.singletonMap("addr1", new AddressProperties(true, false, "vanilla")), r));
        assertThat(response.getStatus(), is(500));
    }

    @Test
    public void testDelete() {
        addressManager.destinationsUpdated(Sets.newSet(createGroup(new Destination("addr1", false, false, Optional.empty()))));
        addressManager.destinationsUpdated(Sets.newSet(createGroup(new Destination("addr2", false, false, Optional.empty()))));

        Response response = invokeRequest(r -> restService.deleteAddresses(Collections.singletonList("addr1"), r));

        Map<String, AddressProperties> result = (Map<String, AddressProperties>) response.getEntity();
        assertThat(result.size(), is(1));
        assertTrue(result.containsKey("addr2"));
        assertFalse(result.containsKey("addr1"));

        assertThat(addressManager.destinationList.size(), is(1));
        assertDestination(new Destination("addr2", false, false, Optional.empty()));
        assertNotDestination(new Destination("addr1", false, false, Optional.empty()));
    }

    @Test
    public void testDeleteException() {
        addressManager.throwException = true;
        Response response = invokeRequest(r -> restService.deleteAddresses(Collections.singletonList("addr1"), r));
        assertThat(response.getStatus(), is(500));
    }

    private static DestinationGroup createGroup(Destination destination) {
        return new DestinationGroup(destination.address(), Collections.singleton(destination));
    }

    @Test
    public void testAppend() {
        addressManager.destinationsUpdated(Sets.newSet(createGroup(new Destination("addr1", false, false, Optional.empty()))));

        Map<String, AddressProperties> input = new LinkedHashMap<>();
        input.put("addr2", new AddressProperties(false, false, null));
        input.put("topic", new AddressProperties(true,true, "vanilla"));

        Response response = invokeRequest(r -> restService.appendAddresses(input, r));

        Map<String, AddressProperties> result = (Map<String, AddressProperties>) response.getEntity();
        assertThat(result.size(), is(3));
        assertTrue(result.containsKey("addr1"));
        assertTrue(result.containsKey("addr2"));
        assertTrue(result.containsKey("topic"));

        assertThat(addressManager.destinationList.size(), is(3));
        assertDestination(new Destination("addr2", false, false, Optional.empty()));
        assertDestination(new Destination("topic", true, true, Optional.of("vanilla")));
        assertDestination(new Destination("addr1", false, false, Optional.empty()));
    }

    @Test
    public void testAppendException() {
        addressManager.throwException = true;
        Response response = invokeRequest(r -> restService.appendAddresses(Collections.singletonMap("addr1", new AddressProperties(true, false, "vanilla")), r));
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

    private static class TestManager implements AddressManager {
        Set<DestinationGroup> destinationList = new LinkedHashSet<>();
        boolean throwException = false;

        @Override
        public void destinationsUpdated(Set<DestinationGroup> destinationList) {
            if (throwException) {
                throw new RuntimeException();
            }
            this.destinationList = new LinkedHashSet<>(destinationList);
        }

        @Override
        public Set<DestinationGroup> listDestinationGroups() {
            if (throwException) {
                throw new RuntimeException();
            }
            return this.destinationList;
        }
    }
}
