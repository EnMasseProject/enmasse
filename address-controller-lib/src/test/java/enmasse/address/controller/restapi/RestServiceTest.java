package enmasse.address.controller.restapi;

import enmasse.address.controller.admin.AddressManager;
import enmasse.address.controller.model.Destination;
import org.junit.Before;
import org.junit.Test;
import org.mockito.internal.util.collections.Sets;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.*;

import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.*;

public class RestServiceTest {
    private RestService restService;
    private TestManager addressManager;

    @Before
    public void setup() {
        addressManager = new TestManager();
        restService = new RestService(addressManager);
    }

    @Test
    public void testGet() {
        addressManager.destinationsUpdated(Sets.newSet(
                new Destination("addr1", false, false, null),
                new Destination("queue1", true, false, "vanilla")));
        Response response = restService.getAddresses();
        assertThat(response.getMediaType().toString(), is(MediaType.APPLICATION_JSON));
        assertThat(response.getStatus(), is(200));
        Map<String, AddressProperties> data = (Map<String, AddressProperties>) response.getEntity();
        assertThat(data.size(), is(2));
        assertNotNull(data.get("addr1"));
        assertNotNull(data.get("queue1"));
    }

    @Test
    public void testGetException() {
        addressManager.throwException = true;
        Response response = restService.getAddresses();
        assertThat(response.getStatus(), is(500));
    }

    @Test
    public void testPut() {
        addressManager.destinationsUpdated(Sets.newSet(new Destination("addr1", false, false, null)));

        Map<String, AddressProperties> input = new LinkedHashMap<>();
        input.put("addr2", new AddressProperties(false, false, null));
        input.put("topic", new AddressProperties(true,true, "vanilla"));

        Response response = restService.putAddresses(input);

        Map<String, AddressProperties> result = (Map<String, AddressProperties>) response.getEntity();
        assertThat(result, is(input));

        assertThat(addressManager.destinationList.size(), is(2));
        assertDestination(new Destination("addr2", false, false, null));
        assertDestination(new Destination("topic", true, true, "vanilla"));
        assertNotDestination(new Destination("addr1", false, false, null));
    }

    @Test
    public void testPutException() {
        addressManager.throwException = true;
        Response response = restService.putAddresses(Collections.singletonMap("addr1", new AddressProperties(true, false, "vanilla")));
        assertThat(response.getStatus(), is(500));
    }

    @Test
    public void testDelete() {
        addressManager.destinationsUpdated(Sets.newSet(new Destination("addr1", false, false, null)));
        addressManager.destinationsUpdated(Sets.newSet(new Destination("addr2", false, false, null)));

        Response response = restService.deleteAddresses(Arrays.asList("addr1"));

        Map<String, AddressProperties> result = (Map<String, AddressProperties>) response.getEntity();
        assertThat(result.size(), is(1));
        assertTrue(result.containsKey("addr2"));
        assertFalse(result.containsKey("addr1"));

        assertThat(addressManager.destinationList.size(), is(1));
        assertDestination(new Destination("addr2", false, false, null));
        assertNotDestination(new Destination("addr1", false, false, null));
    }

    @Test
    public void testDeleteException() {
        addressManager.throwException = true;
        Response response = restService.deleteAddresses(Collections.singletonList("addr1"));
        assertThat(response.getStatus(), is(500));
    }

    @Test
    public void testAppend() {
        addressManager.destinationsUpdated(Sets.newSet(new Destination("addr1", false, false, null)));

        Map<String, AddressProperties> input = new LinkedHashMap<>();
        input.put("addr2", new AddressProperties(false, false, null));
        input.put("topic", new AddressProperties(true,true, "vanilla"));

        Response response = restService.appendAddresses(input);

        Map<String, AddressProperties> result = (Map<String, AddressProperties>) response.getEntity();
        assertThat(result.size(), is(3));
        assertTrue(result.containsKey("addr1"));
        assertTrue(result.containsKey("addr2"));
        assertTrue(result.containsKey("topic"));

        assertThat(addressManager.destinationList.size(), is(3));
        assertDestination(new Destination("addr2", false, false, null));
        assertDestination(new Destination("topic", true, true, "vanilla"));
        assertDestination(new Destination("addr1", false, false, null));
    }

    @Test
    public void testAppendException() {
        addressManager.throwException = true;
        Response response = restService.appendAddresses(Collections.singletonMap("addr1", new AddressProperties(true, false, "vanilla")));
        assertThat(response.getStatus(), is(500));
    }

    private void assertNotDestination(Destination destination) {
        assertFalse(addressManager.destinationList.contains(destination));
    }

    private void assertDestination(Destination destination) {
        assertThat(addressManager.destinationList, hasItem(destination));
    }

    private static class TestManager implements AddressManager {
        Set<Destination> destinationList = new LinkedHashSet<>();
        boolean throwException = false;

        @Override
        public void destinationsUpdated(Set<Destination> destinationList) {
            if (throwException) {
                throw new RuntimeException();
            }
            this.destinationList = new LinkedHashSet<>(destinationList);
        }

        @Override
        public Set<Destination> listDestinations() {
            if (throwException) {
                throw new RuntimeException();
            }
            return this.destinationList;
        }
    }
}
