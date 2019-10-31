/*
 * Copyright 2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.systemtest.soak;

import io.enmasse.address.model.AddressSpaceBuilder;
import io.enmasse.systemtest.bases.isolated.ITestIsolatedBrokered;
import io.enmasse.systemtest.bases.soak.SoakTestBase;
import io.enmasse.systemtest.model.addressplan.DestinationPlan;
import io.enmasse.systemtest.model.addressspace.AddressSpacePlans;
import io.enmasse.systemtest.model.addressspace.AddressSpaceType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;

class BrokeredSoakTest extends SoakTestBase implements ITestIsolatedBrokered {

    @Test
    void testQueueSendReceiveLong() throws Exception {
        doTestQueueSendReceiveLong(
                new AddressSpaceBuilder()
                        .withNewMetadata()
                        .withName("test-send-receive-brokered")
                        .withNamespace(KUBERNETES.getInfraNamespace())
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
                        .withNamespace(KUBERNETES.getInfraNamespace())
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
                        .withNamespace(KUBERNETES.getInfraNamespace())
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
    void testTestLoadLong() throws Exception {
        doTestLoad(AddressSpaceType.BROKERED, AddressSpacePlans.BROKERED, DestinationPlan.BROKERED_QUEUE);
    }
}
