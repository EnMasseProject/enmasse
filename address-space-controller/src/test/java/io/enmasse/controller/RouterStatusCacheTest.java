/*
 * Copyright 2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.controller;

import io.enmasse.address.model.AddressSpace;
import io.enmasse.address.model.AddressSpaceBuilder;
import io.enmasse.k8s.api.LogEventLogger;
import io.fabric8.kubernetes.client.NamespacedKubernetesClient;
import io.fabric8.kubernetes.client.server.mock.KubernetesServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

public class RouterStatusCacheTest {

    private KubernetesServer server = new KubernetesServer(true, true);
    private NamespacedKubernetesClient client;

    @BeforeEach
    public void setup() {
        server.before();
        client = server.getClient();
    }

    @AfterEach
    public void teardown() {
        server.after();
    }

    @Test
    public void testResetCache() {
        AddressSpace a1 = new AddressSpaceBuilder()
                .editOrNewMetadata()
                .withName("a1")
                .withNamespace("n")
                .endMetadata()
                .editOrNewSpec()
                .withType("standard")
                .withPlan("small")
                .endSpec()
                .build();

        AddressSpace a2 = new AddressSpaceBuilder()
                .editOrNewMetadata()
                .withName("a2")
                .withNamespace("n")
                .endMetadata()
                .editOrNewSpec()
                .withType("standard")
                .withPlan("small")
                .endSpec()
                .build();

        RouterStatusCache cache = new RouterStatusCache(new LogEventLogger(), Duration.ofDays(1), client, "n", Duration.ofSeconds(1), Duration.ofSeconds(1));
        cache.reconcileAll(List.of(a1, a2));

        assertNull(cache.getLatestResult(a1));
        assertNull(cache.getLatestResult(a2));

        cache.checkRouterStatus(addressSpace -> Collections.singletonList(new RouterStatus("r1",
                new RouterConnections(Collections.singletonList("example.com"), Collections.singletonList(true), Collections.singletonList("up")),
                Collections.emptyList(),
                0)));

        assertNotNull(cache.getLatestResult(a1));
        assertNotNull(cache.getLatestResult(a2));

        cache.reconcileAll(List.of(a1, a2));

        assertNotNull(cache.getLatestResult(a1));
        assertNotNull(cache.getLatestResult(a2));

        cache.reconcileAll(List.of(a1));

        assertNotNull(cache.getLatestResult(a1));
        assertNull(cache.getLatestResult(a2));
    }
}
