/*
 * Copyright 2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.systemtest.marathon;

import io.enmasse.systemtest.AddressSpace;
import io.enmasse.systemtest.AddressSpaceType;
import io.enmasse.systemtest.AuthService;
import io.enmasse.systemtest.bases.ITestBaseBrokered;
import org.junit.jupiter.api.Test;

public class BrokeredMarathonTest extends MarathonTestBase implements ITestBaseBrokered {

    @Test
    public void testCreateDeleteAddressSpaceLong() throws Exception {
        doTestCreateDeleteAddressSpaceLong(
                new AddressSpace("test-create-delete-brokered-space", AddressSpaceType.BROKERED));
    }

    @Test
    public void testCreateDeleteAddressesLong() throws Exception {
        doTestCreateDeleteAddressesLong(
                new AddressSpace("test-create-delete-addresses-brokered", AddressSpaceType.BROKERED));
    }

    @Test
    public void testCreateDeleteAddressesWithAuthLong() throws Exception {
        doTestCreateDeleteAddressesWithAuthLong(
                new AddressSpace("test-create-delete-addresses-auth-brokered",
                        AddressSpaceType.BROKERED, AuthService.STANDARD));
    }

    @Test
    public void testQueueSendReceiveLong() throws Exception {
        doTestQueueSendReceiveLong(
                new AddressSpace("test-queue-sendreceive-brokered", AddressSpaceType.BROKERED));
    }

    @Test
    public void testCreateDeleteUsersLong() throws Exception {
        doTestCreateDeleteUsersLong(
                new AddressSpace("test-create-delete-users-brokered",
                        AddressSpaceType.BROKERED, AuthService.STANDARD));
    }

    @Test
    public void testAuthSendReceiveLong() throws Exception {
        doTestAuthSendReceiveLong(
                new AddressSpace("test-auth-send-receive-brokered",
                        AddressSpaceType.BROKERED, AuthService.STANDARD));
    }

    @Test
    public void testCreateDeleteUsersRestartKeyCloakLong() throws Exception {
        doTestCreateDeleteUsersRestartKeyCloakLong(
                new AddressSpace("test-create-delete-users-restart-brokered",
                        AddressSpaceType.BROKERED, AuthService.STANDARD));
    }

    @Test
    public void testTopicPubSubLong() throws Exception {
        doTestTopicPubSubLong(
                new AddressSpace("test-topic-pubsub-brokered", AddressSpaceType.BROKERED));
    }

    @Test
    public void testCreateDeleteAddressesViaAgentLong() throws Exception {
        doTestCreateDeleteAddressesViaAgentLong(
                new AddressSpace("brokered-marathon-web-console",
                        AddressSpaceType.BROKERED, AuthService.STANDARD));
    }
}
