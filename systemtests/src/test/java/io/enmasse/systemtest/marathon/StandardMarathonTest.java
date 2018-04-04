/*
 * Copyright 2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.systemtest.marathon;

import io.enmasse.systemtest.AddressSpace;
import io.enmasse.systemtest.AddressSpaceType;
import io.enmasse.systemtest.AddressType;
import io.enmasse.systemtest.AuthService;
import org.junit.jupiter.api.Test;

public class StandardMarathonTest extends MarathonTestBase {

    @Test
    public void testCreateDeleteAddressSpaceLong() throws Exception {
        doTestCreateDeleteAddressSpaceLong(
                new AddressSpace("test-create-delete-standard-space", AddressSpaceType.STANDARD));
    }

    @Test
    public void testCreateDeleteAddressesLongStandard() throws Exception {
        doTestCreateDeleteAddressesLong(
                new AddressSpace("test-create-delete-addresses-standard", AddressSpaceType.STANDARD));
    }

    @Test
    public void testCreateDeleteAddressesWithAuthLong() throws Exception {
        doTestCreateDeleteAddressesWithAuthLong(
                new AddressSpace("test-create-delete-addresses-auth-standard",
                        AddressSpaceType.STANDARD, AuthService.STANDARD));
    }

    @Test
    public void testQueueSendReceiveLong() throws Exception {
        doTestQueueSendReceiveLong(
                new AddressSpace("test-queue-sendreceive-standard", AddressSpaceType.STANDARD));
    }

    @Test
    public void testCreateDeleteUsersLong() throws Exception {
        doTestCreateDeleteUsersLong(
                new AddressSpace("test-create-delete-users-standard",
                        AddressSpaceType.STANDARD, AuthService.STANDARD));
    }

    @Test
    public void testAuthSendReceiveLong() throws Exception {
        doTestAuthSendReceiveLong(
                new AddressSpace("test-auth-send-receive-standard",
                        AddressSpaceType.STANDARD, AuthService.STANDARD));
    }

    @Test
    public void testCreateDeleteUsersRestartKeyCloakLong() throws Exception {
        doTestCreateDeleteUsersRestartKeyCloakLong(
                new AddressSpace("test-create-delete-users-restart-standard",
                        AddressSpaceType.STANDARD, AuthService.STANDARD));
    }

    @Test
    public void testTopicPubSubLong() throws Exception {
        doTestTopicPubSubLong(
                new AddressSpace("test-topic-pubsub-standard", AddressSpaceType.STANDARD));
    }

    @Test
    public void testCreateDeleteAddressesViaAgentLong() throws Exception {
        doTestCreateDeleteAddressesViaAgentLong(
                new AddressSpace("standard-marathon-web-console",
                        AddressSpaceType.STANDARD, AuthService.STANDARD));
    }

    @Override
    protected String getDefaultPlan(AddressType addressType) {
        switch (addressType) {
            case QUEUE:
                return "pooled-queue";
            case TOPIC:
                return "pooled-topic";
        }
        return null;
    }
}
