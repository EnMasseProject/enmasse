package io.enmasse.config.service.podsense;

import io.enmasse.config.service.kubernetes.MessageEncoder;
import io.enmasse.config.service.kubernetes.ObserverOptions;
import io.enmasse.config.service.kubernetes.SubscriptionConfig;
import io.fabric8.kubernetes.client.KubernetesClient;
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


        KubernetesClient client = mock(KubernetesClient.class);
        ObserverOptions options = config.getObserverOptions(client);
        assertNotNull(options);
        assertTrue(options.getObserverFilter().isEmpty());
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
