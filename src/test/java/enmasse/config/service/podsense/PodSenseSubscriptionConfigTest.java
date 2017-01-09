package enmasse.config.service.podsense;

import enmasse.config.service.openshift.MessageEncoder;
import enmasse.config.service.openshift.ObserverOptions;
import enmasse.config.service.openshift.SubscriptionConfig;
import io.fabric8.openshift.client.OpenShiftClient;
import org.junit.Test;

import java.util.Collections;
import java.util.function.Predicate;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;

public class PodSenseSubscriptionConfigTest {

    @Test
    public void testDefaultSetup() {
        SubscriptionConfig<PodResource> config = new PodSenseSubscriptionConfig();

        MessageEncoder encoder = config.getMessageEncoder();
        assertNotNull(encoder);


        OpenShiftClient client = mock(OpenShiftClient.class);
        ObserverOptions options = config.getObserverOptions(client, Collections.singletonMap("my", "filter"));
        assertNotNull(options);
        assertThat(options.getLabelMap().get("my"), is("filter"));
        assertThat(options.getOperations().length, is(1));
    }

    @Test
    public void testFilter() {
        Predicate<PodResource> filter = new PodSenseSubscriptionConfig().getResourceFilter();
        assertFalse(filter.test(PodSenseMessageEncoderTest.createPod("p1", null, "", Collections.emptyMap())));
        assertFalse(filter.test(PodSenseMessageEncoderTest.createPod("p1", "", "", Collections.emptyMap())));
        assertTrue(filter.test(PodSenseMessageEncoderTest.createPod("p1", "myhost", "", Collections.emptyMap())));
    }
}
