/*
 * Copyright 2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.systemtest.soak;

import io.enmasse.address.model.AddressSpaceBuilder;
import io.enmasse.systemtest.bases.isolated.ITestIsolatedStandard;
import io.enmasse.systemtest.bases.soak.SoakTestBase;
import io.enmasse.systemtest.model.addressplan.DestinationPlan;
import io.enmasse.systemtest.model.addressspace.AddressSpacePlans;
import io.enmasse.systemtest.model.addressspace.AddressSpaceType;
import org.junit.jupiter.api.Test;

class StandardSoakTest extends SoakTestBase implements ITestIsolatedStandard {

    @Test
    void testQueueSendReceiveLong() throws Exception {
        doTestQueueSendReceiveLong(
                new AddressSpaceBuilder()
                        .withNewMetadata()
                        .withName("test-send-receive-standard")
                        .withNamespace(KUBERNETES.getInfraNamespace())
                        .endMetadata()
                        .withNewSpec()
                        .withType(AddressSpaceType.STANDARD.toString())
                        .withPlan(AddressSpacePlans.STANDARD_MEDIUM)
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
                        .withName("test-auth-send-receive-standard")
                        .withNamespace(KUBERNETES.getInfraNamespace())
                        .endMetadata()
                        .withNewSpec()
                        .withType(AddressSpaceType.STANDARD.toString())
                        .withPlan(AddressSpacePlans.STANDARD_MEDIUM)
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
                        .withName("test-topic-pubsub-standard")
                        .withNamespace(KUBERNETES.getInfraNamespace())
                        .endMetadata()
                        .withNewSpec()
                        .withType(AddressSpaceType.STANDARD.toString())
                        .withPlan(AddressSpacePlans.STANDARD_MEDIUM)
                        .withNewAuthenticationService()
                        .withName("standard-authservice")
                        .endAuthenticationService()
                        .endSpec()
                        .build());
    }

    @Test
    void testTestLoadLong() throws Exception {
        doTestLoad(AddressSpaceType.STANDARD, AddressSpacePlans.STANDARD_MEDIUM, DestinationPlan.STANDARD_SMALL_QUEUE);
    }
}
