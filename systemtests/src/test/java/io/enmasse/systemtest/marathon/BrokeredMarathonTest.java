/*
 * Copyright 2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.systemtest.marathon;

import io.enmasse.address.model.AuthenticationServiceType;
import io.enmasse.systemtest.AddressSpaceType;
import io.enmasse.systemtest.ability.ITestBaseBrokered;
import io.enmasse.systemtest.utils.AddressSpaceUtils;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;

class BrokeredMarathonTest extends MarathonTestBase implements ITestBaseBrokered {

    @Test
    void testCreateDeleteAddressSpaceLong() throws Exception {
        doTestCreateDeleteAddressSpaceLong(() ->
                AddressSpaceUtils.createAddressSpaceObject("test-create-delete-brokered-space", AddressSpaceType.BROKERED, AuthenticationServiceType.STANDARD));
    }

    @Test
    void testCreateDeleteAddressesWithAuthLong() throws Exception {
        doTestCreateDeleteAddressesWithAuthLong(
                AddressSpaceUtils.createAddressSpaceObject("test-create-delete-addresses-auth-brokered",
                        AddressSpaceType.BROKERED, AuthenticationServiceType.STANDARD));
    }

    @Test
    @Disabled("test failing in ci, using all server resources")
    void testCreateHighAddressCountCheckStatusDeleteLong() throws Exception {
        doTestCreateHighAddressCountCheckStatusDeleteLong(
                AddressSpaceUtils.createAddressSpaceObject("test-create-addresses-check-status-delete",
                        AddressSpaceType.BROKERED, AuthenticationServiceType.STANDARD));
    }

    @Test
    void testQueueSendReceiveLong() throws Exception {
        doTestQueueSendReceiveLong(
                AddressSpaceUtils.createAddressSpaceObject("test-queue-sendreceive-brokered", AddressSpaceType.BROKERED, AuthenticationServiceType.STANDARD));
    }

    @Test
    void testCreateDeleteUsersLong() throws Exception {
        doTestCreateDeleteUsersLong(
                AddressSpaceUtils.createAddressSpaceObject("test-create-delete-users-brokered",
                        AddressSpaceType.BROKERED, AuthenticationServiceType.STANDARD));
    }

    @Test
    void testAuthSendReceiveLong() throws Exception {
        doTestAuthSendReceiveLong(
                AddressSpaceUtils.createAddressSpaceObject("test-auth-send-receive-brokered",
                        AddressSpaceType.BROKERED, AuthenticationServiceType.STANDARD));
    }

    @Test
    void testTopicPubSubLong() throws Exception {
        doTestTopicPubSubLong(
                AddressSpaceUtils.createAddressSpaceObject("test-topic-pubsub-brokered", AddressSpaceType.BROKERED, AuthenticationServiceType.STANDARD));
    }

    @Test
    void testCreateDeleteAddressesViaAgentLong(TestInfo info) throws Exception {
        doTestCreateDeleteAddressesViaAgentLong(
                AddressSpaceUtils.createAddressSpaceObject("brokered-marathon-web-console",
                        AddressSpaceType.BROKERED, AuthenticationServiceType.STANDARD), info.getTestClass().get().getName(), info.getTestMethod().get().getName());
    }
}
