/*
 * Copyright 2019, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package io.enmasse.iot.registry.infinispan.device.impl;

import static io.enmasse.iot.registry.infinispan.device.impl.CredentialsManagementServiceImpl.calculateDifference;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static org.hamcrest.collection.IsEmptyCollection.empty;
import static org.hamcrest.core.IsCollectionContaining.hasItem;

import java.util.Arrays;
import org.junit.jupiter.api.Test;

import io.enmasse.iot.registry.infinispan.device.data.CredentialsKey;
import io.enmasse.iot.registry.infinispan.device.data.DeviceCredential;

public class CredentialsManagementServiceImplTest {

    private static final String TENANT = "tenant";

    @Test
    public void testDiffEmpty() {
        var result = calculateDifference(TENANT, emptyList(), emptyList());
        assertThat(result, empty());
    }

    @Test
    public void testEmptyAdd() {
        final DeviceCredential cred1 = new DeviceCredential();
        cred1.setAuthId("auth1");
        cred1.setType("type1");
        cred1.setSecrets(Arrays.asList("{}"));

        var result = calculateDifference(TENANT, emptyList(), asList(cred1));
        assertThat(result, hasSize(1));
        assertThat(result, hasItem(new CredentialsKey(TENANT, "auth1", "type1")));
    }

    @Test
    public void testRemove1() {
        final DeviceCredential cred1 = new DeviceCredential();
        cred1.setAuthId("auth1");
        cred1.setType("type1");
        cred1.setSecrets(Arrays.asList("{}"));

        var result = calculateDifference(TENANT, asList(cred1), emptyList());
        assertThat(result, hasSize(1));
        assertThat(result, hasItem(new CredentialsKey(TENANT, "auth1", "type1")));
    }

    @Test
    public void testUpdate1() {
        final DeviceCredential cred1 = new DeviceCredential();
        cred1.setAuthId("auth1");
        cred1.setType("type1");
        cred1.setSecrets(Arrays.asList("{}"));

        final DeviceCredential cred2 = new DeviceCredential();
        cred2.setAuthId("auth1");
        cred2.setType("type1");
        cred2.setSecrets(Arrays.asList("{\"enabled\":true}"));

        var result = calculateDifference(TENANT, asList(cred1), asList(cred2));
        assertThat(result, hasSize(1));
        assertThat(result, hasItem(new CredentialsKey(TENANT, "auth1", "type1")));
    }

    @Test
    public void testExchange1() {
        final DeviceCredential cred1 = new DeviceCredential();
        cred1.setAuthId("auth1");
        cred1.setType("type1");
        cred1.setSecrets(Arrays.asList("{}"));

        final DeviceCredential cred2 = new DeviceCredential();
        cred2.setAuthId("auth2");
        cred2.setType("type1");
        cred2.setSecrets(Arrays.asList("{}"));

        var result = calculateDifference(TENANT, asList(cred1), asList(cred2));
        assertThat(result, hasSize(2));
        assertThat(result, hasItem(new CredentialsKey(TENANT, "auth1", "type1")));
        assertThat(result, hasItem(new CredentialsKey(TENANT, "auth2", "type1")));
    }

}
