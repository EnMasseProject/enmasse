package enmasse.config.service.podsense;

import enmasse.config.service.openshift.MessageEncoder;
import enmasse.config.service.openshift.ObserverOptions;
import enmasse.config.service.openshift.SubscriptionConfig;
import io.fabric8.openshift.client.OpenShiftClient;
import org.junit.Test;

import java.util.Collections;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;

public class PodSenseSubscriptionConfigTest {

    @Test
    public void testDefaultSetup() {
        SubscriptionConfig config = new PodSenseSubscriptionConfig();

        MessageEncoder encoder = config.getMessageEncoder();
        assertNotNull(encoder);


        OpenShiftClient client = mock(OpenShiftClient.class);
        ObserverOptions options = config.getObserverOptions(client, Collections.singletonMap("my", "filter"));
        assertNotNull(options);
        assertThat(options.getLabelMap().get("my"), is("filter"));
        assertThat(options.getOperations().length, is(1));
    }
}
