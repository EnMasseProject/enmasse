/*
 * Copyright 2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.systemtest.marathon;

import io.enmasse.systemtest.AddressSpace;
import io.enmasse.systemtest.AddressSpaceType;
import io.enmasse.systemtest.AuthService;
import io.enmasse.systemtest.ability.ITestBaseBrokered;
import org.junit.jupiter.api.Test;

class BrokeredMarathonTest extends MarathonTestBase implements ITestBaseBrokered {

    @Test
    void testCreateDeleteAddressSpaceLong() throws Exception {
        doTestCreateDeleteAddressSpaceLong(
                new AddressSpace("test-create-delete-brokered-space", AddressSpaceType.BROKERED));
    }

    @Test
    void testCreateDeleteAddressesLong() throws Exception {
        doTestCreateDeleteAddressesLong(
                new AddressSpace("test-create-delete-addresses-brokered", AddressSpaceType.BROKERED));
    }

    @Test
    void testCreateDeleteAddressesWithAuthLong() throws Exception {
        doTestCreateDeleteAddressesWithAuthLong(
                new AddressSpace("test-create-delete-addresses-auth-brokered",
                        AddressSpaceType.BROKERED, AuthService.STANDARD));
    }

    @Test
    void testQueueSendReceiveLong() throws Exception {
        doTestQueueSendReceiveLong(
                new AddressSpace("test-queue-sendreceive-brokered", AddressSpaceType.BROKERED));
    }

    @Test
    void testCreateDeleteUsersLong() throws Exception {
        doTestCreateDeleteUsersLong(
                new AddressSpace("test-create-delete-users-brokered",
                        AddressSpaceType.BROKERED, AuthService.STANDARD));
    }

    @Test
    void testAuthSendReceiveLong() throws Exception {
        doTestAuthSendReceiveLong(
                new AddressSpace("test-auth-send-receive-brokered",
                        AddressSpaceType.BROKERED, AuthService.STANDARD));
    }

    @Test
    void testCreateDeleteUsersRestartKeyCloakLong() throws Exception {
        doTestCreateDeleteUsersRestartKeyCloakLong(
                new AddressSpace("test-create-delete-users-restart-brokered",
                        AddressSpaceType.BROKERED, AuthService.STANDARD));
    }

    @Test
    void testTopicPubSubLong() throws Exception {
        doTestTopicPubSubLong(
                new AddressSpace("test-topic-pubsub-brokered", AddressSpaceType.BROKERED));
    }

    @Test
    void testCreateDeleteAddressesViaAgentLong() throws Exception {
        doTestCreateDeleteAddressesViaAgentLong(
                new AddressSpace("brokered-marathon-web-console",
                        AddressSpaceType.BROKERED, AuthService.STANDARD));
    }
}
