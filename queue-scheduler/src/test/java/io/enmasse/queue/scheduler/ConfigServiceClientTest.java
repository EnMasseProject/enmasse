/*
 * Copyright 2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.queue.scheduler;

import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;

import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import io.enmasse.address.model.Address;
import io.fabric8.kubernetes.client.KubernetesClient;
import org.junit.Before;
import org.junit.Test;
import org.mockito.internal.util.collections.Sets;

public class ConfigServiceClientTest {
    private ConfigServiceClient client;
    private TestListener listener;

    @Before
    public void setup() throws Exception {
        listener = new TestListener();
        KubernetesClient kubeClient = mock(KubernetesClient.class);

        client = new ConfigServiceClient(listener, kubeClient, "default");
    }

    @Test
    public void testClientUpdatesListener() throws Exception {
        assertNull(listener.addressMap);
        client.resourcesUpdated(Sets.newSet(
                createAddress("queue1", "queue", "pooled-inmemory"),
                createAddress("queue2", "queue", "pooled-inmemory"),
                createAddress("queue3", "queue", "inmemory"),
                createAddress("direct1", "anycast", "standard")));

        assertNotNull(listener.addressMap);
        assertThat(listener.addressMap.size(), is(2));
        assertNotNull(listener.addressMap.get("pooled-inmemory"));
        assertNotNull(listener.addressMap.get("queue3"));
        assertThat(listener.addressMap.get("pooled-inmemory"), hasItem("queue1"));
        assertThat(listener.addressMap.get("pooled-inmemory"), hasItem("queue2"));
        assertThat(listener.addressMap.get("queue3"), hasItem("queue3"));
    }

    private Address createAddress(String name, String type, String planName) {
        return new Address.Builder()
                .setName(name)
                .setType(type)
                .setPlan(planName)
                .build();
    }

    private static class TestListener implements ConfigListener {
        volatile Map<String, Set<String>> addressMap;

        @Override
        public void addressesChanged(Map<String, Set<Address>> addressMap) {

            this.addressMap = addressMap.entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey,
                                          e -> e.getValue().stream().map(Address::getAddress).collect(Collectors.toSet())));
        }

    }

}
