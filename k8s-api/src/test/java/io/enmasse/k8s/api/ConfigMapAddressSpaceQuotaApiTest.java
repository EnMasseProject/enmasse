/*
 * Copyright 2017-2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.k8s.api;

import io.enmasse.address.model.v1.quota.AddressSpaceQuota;
import io.enmasse.address.model.v1.quota.AddressSpaceQuotaMetadata;
import io.enmasse.address.model.v1.quota.AddressSpaceQuotaRule;
import io.enmasse.address.model.v1.quota.AddressSpaceQuotaSpec;
import io.fabric8.kubernetes.client.NamespacedKubernetesClient;
import io.fabric8.kubernetes.client.server.mock.KubernetesServer;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.Optional;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class ConfigMapAddressSpaceQuotaApiTest {
    @Rule
    public KubernetesServer server = new KubernetesServer(true, true);

    private NamespacedKubernetesClient client;

    @Before
    public void setup() {
        client = server.getClient();
    }

    @Test
    public void testApi() {
        AddressSpaceQuotaApi api = new ConfigMapAddressSpaceQuotaApi(client);

        AddressSpaceQuota quota = new AddressSpaceQuota(
                new AddressSpaceQuotaMetadata("myquota", null, null),
                new AddressSpaceQuotaSpec("developer", Arrays.asList(
                        new AddressSpaceQuotaRule(1, "standard", "unlimited-standard"),
                        new AddressSpaceQuotaRule(2, "brokered", "unlimited-brokered"))));

        api.createAddressSpaceQuota(quota);

        assertEquals(1, client.configMaps().list().getItems().size());

        Optional<AddressSpaceQuota> retrieved = api.getAddressSpaceQuotaWithName("myquota");
        assertTrue(retrieved.isPresent());
        assertQuotaEquals(quota, retrieved.get());

        AddressSpaceQuota anotherQuota = new AddressSpaceQuota(
                new AddressSpaceQuotaMetadata("secondquota", Collections.singletonMap("key1", "value1"), null),
                new AddressSpaceQuotaSpec("otheruser", Arrays.asList(
                        new AddressSpaceQuotaRule(3, "standard", "unlimited-standard"))));
        api.createAddressSpaceQuota(anotherQuota);

        assertEquals(2, api.listAddressSpaceQuotas().getItems().size());

        api.deleteAddressSpaceQuota("myquota");
        assertEquals(1, api.listAddressSpaceQuotas().getItems().size());
        assertQuotaEquals(anotherQuota, api.listAddressSpaceQuotas().getItems().get(0));
    }

    private static void assertQuotaEquals(AddressSpaceQuota expected, AddressSpaceQuota actual) {
        assertEquals(expected.getMetadata().getName(), actual.getMetadata().getName());
        assertEquals(expected.getSpec().getUser(), actual.getSpec().getUser());
        assertEquals(expected.getSpec().getRules().size(), actual.getSpec().getRules().size());
    }
}
