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
import org.junit.jupiter.api.TestInfo;

class BrokeredMarathonTest extends MarathonTestBase implements ITestBaseBrokered {

    @Test
    void testCreateDeleteAddressSpaceLong() throws Exception {
        doTestCreateDeleteAddressSpaceLong(() ->
                new AddressSpace("test-create-delete-brokered-space", AddressSpaceType.BROKERED, AuthService.STANDARD));
    }

    @Test
    void testCreateDeleteAddressesWithAuthLong() throws Exception {
        doTestCreateDeleteAddressesWithAuthLong(
                new AddressSpace("test-create-delete-addresses-auth-brokered",
                        AddressSpaceType.BROKERED, AuthService.STANDARD));
    }

    @Test
    void testCreateHighAddressCountCheckStatusDeleteLong() throws Exception {
        doTestCreateHighAddressCountCheckStatusDeleteLong(
                new AddressSpace("test-create-addresses-check-status-delete",
                        AddressSpaceType.BROKERED, AuthService.STANDARD));
    }

    @Test
    void testQueueSendReceiveLong() throws Exception {
        doTestQueueSendReceiveLong(
                new AddressSpace("test-queue-sendreceive-brokered", AddressSpaceType.BROKERED, AuthService.STANDARD));
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
    void testTopicPubSubLong() throws Exception {
        doTestTopicPubSubLong(
                new AddressSpace("test-topic-pubsub-brokered", AddressSpaceType.BROKERED, AuthService.STANDARD));
    }

    @Test
    void testCreateDeleteAddressesViaAgentLong(TestInfo info) throws Exception {
        doTestCreateDeleteAddressesViaAgentLong(
                new AddressSpace("brokered-marathon-web-console",
                        AddressSpaceType.BROKERED, AuthService.STANDARD), info.getTestClass().get().getName(), info.getTestMethod().get().getName());
    }
}
