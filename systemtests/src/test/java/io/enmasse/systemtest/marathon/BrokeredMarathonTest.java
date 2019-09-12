/*
 * Copyright 2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.systemtest.marathon;

import io.enmasse.address.model.AddressSpaceBuilder;
import io.enmasse.systemtest.bases.isolated.ITestIsolatedBrokered;
import io.enmasse.systemtest.bases.marathon.MarathonTestBase;
import io.enmasse.systemtest.model.addressspace.AddressSpacePlans;
import io.enmasse.systemtest.model.addressspace.AddressSpaceType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;

class BrokeredMarathonTest extends MarathonTestBase implements ITestIsolatedBrokered {

    @Test
    void testCreateDeleteAddressSpaceLong() throws Exception {
        doTestCreateDeleteAddressSpaceLong(() ->
                new AddressSpaceBuilder()
                        .withNewMetadata()
                        .withName("test-create-delete-addr-space")
                        .withNamespace(kubernetes.getInfraNamespace())
                        .endMetadata()
                        .withNewSpec()
                        .withType(AddressSpaceType.BROKERED.toString())
                        .withPlan(AddressSpacePlans.BROKERED)
                        .withNewAuthenticationService()
                        .withName("standard-authservice")
                        .endAuthenticationService()
                        .endSpec()
                        .build());
    }

    @Test
    void testCreateDeleteAddressesWithAuthLong() throws Exception {
        doTestCreateDeleteAddressesWithAuthLong(
                new AddressSpaceBuilder()
                        .withNewMetadata()
                        .withName("test-create-delete-addresses")
                        .withNamespace(kubernetes.getInfraNamespace())
                        .endMetadata()
                        .withNewSpec()
                        .withType(AddressSpaceType.BROKERED.toString())
                        .withPlan(AddressSpacePlans.BROKERED)
                        .withNewAuthenticationService()
                        .withName("standard-authservice")
                        .endAuthenticationService()
                        .endSpec()
                        .build());
    }

    @Test
    void testQueueSendReceiveLong() throws Exception {
        doTestQueueSendReceiveLong(
                new AddressSpaceBuilder()
                        .withNewMetadata()
                        .withName("test-send-receive-brokered")
                        .withNamespace(kubernetes.getInfraNamespace())
                        .endMetadata()
                        .withNewSpec()
                        .withType(AddressSpaceType.BROKERED.toString())
                        .withPlan(AddressSpacePlans.BROKERED)
                        .withNewAuthenticationService()
                        .withName("standard-authservice")
                        .endAuthenticationService()
                        .endSpec()
                        .build());
    }

    @Test
    void testAuthSendReceiveLong() throws Exception {
        doTestAuthSendReceiveLong(
                new AddressSpaceBuilder()
                        .withNewMetadata()
                        .withName("test-auth-send-receive-brokered")
                        .withNamespace(kubernetes.getInfraNamespace())
                        .endMetadata()
                        .withNewSpec()
                        .withType(AddressSpaceType.BROKERED.toString())
                        .withPlan(AddressSpacePlans.BROKERED)
                        .withNewAuthenticationService()
                        .withName("standard-authservice")
                        .endAuthenticationService()
                        .endSpec()
                        .build());
    }

    @Test
    void testTopicPubSubLong() throws Exception {
        doTestTopicPubSubLong(
                new AddressSpaceBuilder()
                        .withNewMetadata()
                        .withName("test-topic-pubsub-brokered")
                        .withNamespace(kubernetes.getInfraNamespace())
                        .endMetadata()
                        .withNewSpec()
                        .withType(AddressSpaceType.BROKERED.toString())
                        .withPlan(AddressSpacePlans.BROKERED)
                        .withNewAuthenticationService()
                        .withName("standard-authservice")
                        .endAuthenticationService()
                        .endSpec()
                        .build());
    }

    @Test
    void testCreateDeleteAddressesViaAgentLong(TestInfo info) throws Exception {
        doTestCreateDeleteAddressesViaAgentLong(
                new AddressSpaceBuilder()
                        .withNewMetadata()
                        .withName("brokered-marathon-web-console")
                        .withNamespace(kubernetes.getInfraNamespace())
                        .endMetadata()
                        .withNewSpec()
                        .withType(AddressSpaceType.BROKERED.toString())
                        .withPlan(AddressSpacePlans.BROKERED)
                        .withNewAuthenticationService()
                        .withName("standard-authservice")
                        .endAuthenticationService()
                        .endSpec()
                        .build(),
                info.getTestClass().get().getName(), info.getTestMethod().get().getName());
    }
}
