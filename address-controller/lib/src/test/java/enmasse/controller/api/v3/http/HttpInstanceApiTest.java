package enmasse.controller.api.v3.http;

import enmasse.controller.api.TestInstanceApi;
import enmasse.controller.instance.v3.InstanceList;
import enmasse.controller.model.Instance;
import enmasse.controller.model.InstanceId;
import org.junit.Before;
import org.junit.Test;

import javax.ws.rs.core.Response;
import java.util.Optional;
import java.util.Set;

import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

public class HttpInstanceApiTest {
    private InstanceService instanceService;
    private TestInstanceApi instanceManager;
    private Instance instance1;
    private Instance instance2;

    /*
    @Before
    public void setup() {
        instanceManager = new TestInstanceApi();
        instanceService = new InstanceService(instanceManager);
        instance1 = new Instance.Builder(InstanceId.withId("instance1"))
                .messagingHost(Optional.of("messaging.example.com"))
                .mqttHost(Optional.of("mqtt.example.com"))
                .build();
        instance2 = new Instance.Builder(InstanceId.withIdAndNamespace("instance2", "othernamespace"))
                .messagingHost(Optional.of("messaging2.example.com"))
                .build();
    }

    @Test
    public void testList() {
        instanceManager.create(instance1);
        instanceManager.create(instance2);
        Response response = instanceService.listInstances();
        assertThat(response.getStatus(), is(200));
        Set<Instance> data = ((InstanceList)response.getEntity()).getInstances();

        assertThat(data.size(), is(2));
        assertThat(data, hasItem(instance1));
        assertThat(data, hasItem(instance2));
    }

    @Test
    public void testListException() {
        instanceManager.throwException = true;
        Response response = instanceService.listInstances();
        assertThat(response.getStatus(), is(500));
    }

    @Test
    public void testGet() {
        instanceManager.create(instance1);
        Response response = instanceService.getInstance("instance1");
        assertThat(response.getStatus(), is(200));
        Instance data = ((enmasse.controller.instance.v3.Instance)response.getEntity()).getInstance();

        assertThat(data, is(instance1));
        assertThat(data.messagingHost(), is(instance1.messagingHost()));
    }

    @Test
    public void testGetException() {
        instanceManager.throwException = true;
        Response response = instanceService.getInstance("instance1");
        assertThat(response.getStatus(), is(500));
    }

    @Test
    public void testGetUnknown() {
        Response response = instanceService.getInstance("doesnotexist");
        assertThat(response.getStatus(), is(404));
    }


    @Test
    public void testCreate() {
        Response response = instanceService.createInstance(new enmasse.controller.instance.v3.Instance(instance1));
        assertThat(response.getStatus(), is(200));

        assertThat(instanceManager.list(), hasItem(instance1));
    }

    @Test
    public void testCreateException() {
        instanceManager.throwException = true;
        Response response = instanceService.createInstance(new enmasse.controller.instance.v3.Instance(instance1));
        assertThat(response.getStatus(), is(500));
    }

    @Test
    public void testDelete() {
        instanceManager.create(instance1);
        instanceManager.create(instance2);

        Response response = instanceService.deleteInstance("instance1", new enmasse.controller.instance.v3.Instance(instance1));
        assertThat(response.getStatus(), is(200));

        assertThat(instanceManager.list(), hasItem(instance2));
        assertThat(instanceManager.list().size(), is(1));
    }

    @Test
    public void testDeleteException() {
        instanceManager.create(instance1);
        instanceManager.throwException = true;
        Response response = instanceService.deleteInstance("instance1", new enmasse.controller.instance.v3.Instance(instance1));
        assertThat(response.getStatus(), is(500));
    }
    */
}
