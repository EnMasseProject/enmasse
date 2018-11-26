/*
 * Copyright 2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.systemtest.marathon;

import io.enmasse.systemtest.AddressSpace;
import io.enmasse.systemtest.AddressSpaceType;
import io.enmasse.systemtest.AuthService;
import io.enmasse.systemtest.ability.ITestBaseStandard;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;

class StandardMarathonTest extends MarathonTestBase implements ITestBaseStandard {

    @Test
    void testCreateDeleteAddressSpaceLong() throws Exception {
        doTestCreateDeleteAddressSpaceLong(() ->
                new AddressSpace("test-create-delete-standard-space", AddressSpaceType.STANDARD, AuthService.STANDARD));
    }

    @Test
    void testCreateDeleteAddressesWithAuthLong() throws Exception {
        doTestCreateDeleteAddressesWithAuthLong(
                new AddressSpace("test-create-delete-addresses-auth-standard",
                        AddressSpaceType.STANDARD, AuthService.STANDARD));
    }

    @Test
    @Disabled("test failing in ci, using all server resources")
    void testCreateHighAddressCountCheckStatusDeleteLong() throws Exception {
        doTestCreateHighAddressCountCheckStatusDeleteLong(
                new AddressSpace("test-create-addresses-check-status-delete",
                        AddressSpaceType.STANDARD, AuthService.STANDARD));
    }

    @Test
    void testQueueSendReceiveLong() throws Exception {
        doTestQueueSendReceiveLong(
                new AddressSpace("test-queue-sendreceive-standard", AddressSpaceType.STANDARD, AuthService.STANDARD));
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
    void testTopicPubSubLong() throws Exception {
        doTestTopicPubSubLong(
                new AddressSpace("test-topic-pubsub-standard", AddressSpaceType.STANDARD, AuthService.STANDARD));
    }

    @Test
    void testCreateDeleteAddressesViaAgentLong(TestInfo info) throws Exception {
        doTestCreateDeleteAddressesViaAgentLong(
                new AddressSpace("standard-marathon-web-console",
                        AddressSpaceType.STANDARD, AuthService.STANDARD), info.getTestClass().get().getName(), info.getTestMethod().get().getName());
    }
}
