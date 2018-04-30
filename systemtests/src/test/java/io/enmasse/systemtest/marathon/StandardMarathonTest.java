/*
 * Copyright 2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.systemtest.marathon;

import io.enmasse.systemtest.AddressSpace;
import io.enmasse.systemtest.AddressSpaceType;
import io.enmasse.systemtest.AuthService;
import io.enmasse.systemtest.ability.ITestBaseStandard;
import org.junit.jupiter.api.Test;

class StandardMarathonTest extends MarathonTestBase implements ITestBaseStandard {

    @Test
    void testCreateDeleteAddressSpaceLong() {
        doTestCreateDeleteAddressSpaceLong(
                new AddressSpace("test-create-delete-standard-space", AddressSpaceType.STANDARD));
    }

    @Test
    void testCreateDeleteAddressesLongStandard() throws Exception {
        doTestCreateDeleteAddressesLong(
                new AddressSpace("test-create-delete-addresses-standard", AddressSpaceType.STANDARD));
    }

    @Test
    void testCreateDeleteAddressesWithAuthLong() throws Exception {
        doTestCreateDeleteAddressesWithAuthLong(
                new AddressSpace("test-create-delete-addresses-auth-standard",
                        AddressSpaceType.STANDARD, AuthService.STANDARD));
    }

    @Test
    void testQueueSendReceiveLong() throws Exception {
        doTestQueueSendReceiveLong(
                new AddressSpace("test-queue-sendreceive-standard", AddressSpaceType.STANDARD));
    }

    @Test
    void testCreateDeleteUsersLong() throws Exception {
        doTestCreateDeleteUsersLong(
                new AddressSpace("test-create-delete-users-standard",
                        AddressSpaceType.STANDARD, AuthService.STANDARD));
    }

    @Test
    void testAuthSendReceiveLong() throws Exception {
        doTestAuthSendReceiveLong(
                new AddressSpace("test-auth-send-receive-standard",
                        AddressSpaceType.STANDARD, AuthService.STANDARD));
    }

    @Test
    void testCreateDeleteUsersRestartKeyCloakLong() throws Exception {
        doTestCreateDeleteUsersRestartKeyCloakLong(
                new AddressSpace("test-create-delete-users-restart-standard",
                        AddressSpaceType.STANDARD, AuthService.STANDARD));
    }

    @Test
    void testTopicPubSubLong() throws Exception {
        doTestTopicPubSubLong(
                new AddressSpace("test-topic-pubsub-standard", AddressSpaceType.STANDARD));
    }

    @Test
    void testCreateDeleteAddressesViaAgentLong() throws Exception {
        doTestCreateDeleteAddressesViaAgentLong(
                new AddressSpace("standard-marathon-web-console",
                        AddressSpaceType.STANDARD, AuthService.STANDARD));
    }
}
