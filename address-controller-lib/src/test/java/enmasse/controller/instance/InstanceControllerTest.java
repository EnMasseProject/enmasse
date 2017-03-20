package enmasse.controller.instance;

import enmasse.controller.common.OpenShift;
import enmasse.controller.model.Instance;
import enmasse.controller.model.InstanceId;
import io.fabric8.kubernetes.api.model.KubernetesList;
import io.fabric8.openshift.client.ParameterValue;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.verification.VerificationMode;

import java.util.List;
import java.util.Optional;

import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.argThat;
import static org.mockito.Matchers.matches;
import static org.mockito.Mockito.*;

public class InstanceControllerTest {
    @Test
    public void testMultitenantController() {
        OpenShift mockClient = mock(OpenShift.class);
        when(mockClient.mutateClient(any())).thenReturn(mockClient);
        ArgumentCaptor<ParameterValue> captor = ArgumentCaptor.forClass(ParameterValue.class);
        when(mockClient.processTemplate(matches("test"), captor.capture())).thenReturn(new KubernetesList());

        InstanceController controller = new InstanceControllerImpl(mockClient, "test", true);

        Instance i1 = new Instance.Builder(InstanceId.withIdAndNamespace("myid", "mynamespace")).messagingHost(Optional.of("messaging.example.com")).build();
        Instance i2 = new Instance.Builder(InstanceId.withIdAndNamespace("myid2", "other")).mqttHost(Optional.of("mqtt.example.com")).build();

        controller.create(i1);
        controller.create(i2);

        ArgumentCaptor<InstanceId> idCaptor = ArgumentCaptor.forClass(InstanceId.class);
        verify(mockClient, times(2)).createNamespace(idCaptor.capture());

        assertThat(idCaptor.getAllValues(), hasItem(i1.id()));
        assertThat(idCaptor.getAllValues(), hasItem(i2.id()));

        List<ParameterValue> values = captor.getAllValues();
        assertThat(values.size(), is(4));
        assertParameter(values, "INSTANCE", "myid");
        assertParameter(values, "INSTANCE", "myid2");
        assertParameter(values, "MESSAGING_HOSTNAME", "messaging.example.com");
        assertParameter(values, "MQTT_GATEWAY_HOSTNAME", "mqtt.example.com");
    }

    private void assertParameter(List<ParameterValue> values, String name, String value) {
        for (ParameterValue parameterValue : values) {
            if (parameterValue.getName().equals(name) && parameterValue.getValue().equals(value)) {
                return;
            }
        }
        assertFalse("Unable to find parameter with name " + name + " in list", true);
    }
}
