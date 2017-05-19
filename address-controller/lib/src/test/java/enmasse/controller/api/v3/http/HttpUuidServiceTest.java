package enmasse.controller.api.v3.http;

import enmasse.controller.api.TestAddressManager;
import enmasse.controller.api.TestAddressSpace;
import enmasse.controller.api.TestInstanceApi;
import enmasse.controller.address.v3.Address;
import enmasse.controller.model.Destination;
import enmasse.controller.model.Instance;
import enmasse.controller.model.InstanceId;
import org.junit.Before;
import org.junit.Test;
import org.mockito.internal.util.collections.Sets;

import javax.ws.rs.core.Response;
import java.util.Optional;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

public class HttpUuidServiceTest {
    private TestInstanceApi instanceManager;
    private TestAddressSpace addressSpace;
    private UuidService uuidService;

    @Before
    public void setup() {
        instanceManager = new TestInstanceApi();
        instanceManager.create(new Instance.Builder(InstanceId.withId("myinstance")).uuid(Optional.of("iuid1")).build());
        addressSpace = new TestAddressSpace();
        addressSpace.setDestinations(Sets.newSet(
                new Destination("addr1", "addr1", false, false, Optional.empty(), Optional.of("uid1")),
                new Destination("queue1", "queue1", true, false, Optional.of("vanilla"), Optional.of("uid2"))));
        TestAddressManager addressManager = new TestAddressManager();
        addressManager.addManager(InstanceId.withId("myinstance"), addressSpace);

        uuidService = new UuidService(instanceManager, addressManager);
    }

    @Test
    public void testGet() {
        Response response = uuidService.getResource("uid1");
        assertThat(response.getStatus(), is(200));
        Destination data = ((Address)response.getEntity()).getDestination();
        assertThat(data.address(), is("addr1"));

        response = uuidService.getResource("iuid1");
        assertThat(response.getStatus(), is(200));
        Instance instance = ((enmasse.controller.instance.v3.Instance)response.getEntity()).getInstance();
        assertThat(instance.id().getId(), is("myinstance"));

        response = uuidService.getResource("notexists");
        assertThat(response.getStatus(), is(404));
    }

    @Test
    public void testDelete() {
        assertThat(addressSpace.getDestinations().size(), is(2));
        Response response = uuidService.deleteResource("uid1");
        assertThat(response.getStatus(), is(200));
        assertThat(addressSpace.getDestinations().size(), is(1));

        uuidService.deleteResource("uid2");
        response = uuidService.deleteResource("iuid1");
        assertThat(response.getStatus(), is(200));
        assertThat(instanceManager.list().size(), is(0));
    }
}
