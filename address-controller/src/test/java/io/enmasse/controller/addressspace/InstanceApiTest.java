/*
 * Copyright 2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.controller.addressspace;

import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.any;

public class InstanceApiTest {
        /*
    @Test
    public void testMultitenantController() throws Exception {
        Kubernetes mockClient = mock(Kubernetes.class);
        when(mockClient.withAddressSpace(any())).thenReturn(mockClient);
        ArgumentCaptor<ParameterValue> captor = ArgumentCaptor.forClass(ParameterValue.class);
        when(mockClient.processTemplate(matches("test"), captor.capture())).thenReturn(new KubernetesList());

        AddressSpaceApi controller = new ControllerHelper(mockClient, "test", true);

        Instance i1 = new Instance.Builder(AddressSpaceId.withIdAndNamespace("myid", "mynamespace")).messagingHost(Optional.of("messaging.example.com")).build();
        Instance i2 = new Instance.Builder(AddressSpaceId.withIdAndNamespace("myid2", "other")).mqttHost(Optional.of("mqtt.example.com")).build();

        controller.create(i1);
        controller.create(i2);

        ArgumentCaptor<AddressSpaceId> idCaptor = ArgumentCaptor.forClass(AddressSpaceId.class);
        verify(mockClient, times(2)).createNamespace(idCaptor.capture());

        assertThat(idCaptor.getAllValues(), hasItem(i1.id()));
        assertThat(idCaptor.getAllValues(), hasItem(i2.id()));

        List<ParameterValue> values = captor.getAllValues();
        assertThat(values.size(), is(8));
        assertParameter(values, "ADDRESS_SPACE", "myid");
        assertParameter(values, "ADDRESS_SPACE", "myid2");
        assertParameter(values, "MESSAGING_HOSTNAME", "messaging.example.com");
        assertParameter(values, "MQTT_GATEWAY_HOSTNAME", "mqtt.example.com");

        // Try to delete addressspace that has addresses
        when(mockClient.listClusters()).thenReturn(Arrays.asList(new AddressCluster(mockClient, Collections.emptySet(), new KubernetesList())))
                .thenReturn(Collections.emptyList());

        try {
            controller.delete(i1);
            fail("Expected exception when deleting addressspace that is in use");
        } catch (IllegalArgumentException e) {
            // pass
        }

        // second time works
        controller.delete(i1);
    }

    private void assertParameter(List<ParameterValue> values, String name, String value) {
        for (ParameterValue parameterValue : values) {
            if (parameterValue.getName().equals(name) && parameterValue.getValue().equals(value)) {
                return;
            }
        }
        assertFalse("Unable to find parameter with name " + name + " in list", true);
    }
    */
}
