/*
 * Copyright 2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.config.service.podsense;

import io.enmasse.config.service.kubernetes.MessageEncoder;
import io.enmasse.config.service.kubernetes.ObserverOptions;
import io.enmasse.config.service.kubernetes.SubscriptionConfig;
import io.enmasse.config.service.model.ObserverKey;
import io.enmasse.k8s.api.Resource;
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
        SubscriptionConfig<Pod> config = new PodSenseSubscriptionConfig();

        MessageEncoder encoder = config.getMessageEncoder();
        assertNotNull(encoder);


        KubernetesClient client = mock(KubernetesClient.class);
        Resource<Pod> resource = config.getResource(new ObserverKey(Collections.emptyMap(), Collections.emptyMap()), client);
        assertNotNull(resource);
    }

    @Test
    public void testFilter() {
        Predicate<Pod> filter = new PodSenseSubscriptionConfig().getResourceFilter();
        assertFalse(filter.test(PodSenseMessageEncoderTest.createPod("p1", null, "", Collections.emptyMap())));
        assertFalse(filter.test(PodSenseMessageEncoderTest.createPod("p1", "", "", Collections.emptyMap())));
        assertTrue(filter.test(PodSenseMessageEncoderTest.createPod("p1", "myhost", "", Collections.emptyMap())));
    }
}
