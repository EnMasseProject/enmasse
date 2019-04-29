/*
 * Copyright 2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.systemtest.marathon;

import io.enmasse.address.model.AddressSpaceBuilder;
import io.enmasse.systemtest.AddressSpacePlans;
import io.enmasse.systemtest.AddressSpaceType;
import io.enmasse.systemtest.ability.ITestBaseStandard;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;

class StandardMarathonTest extends MarathonTestBase implements ITestBaseStandard {

    @Test
    void testCreateDeleteAddressSpaceLong() throws Exception {
        doTestCreateDeleteAddressSpaceLong(() ->
                new AddressSpaceBuilder()
                        .withNewMetadata()
                        .withName("test-create-delete-addr-space")
                        .withNamespace(kubernetes.getInfraNamespace())
                        .endMetadata()
                        .withNewSpec()
                        .withType(AddressSpaceType.STANDARD.toString().toLowerCase())
                        .withPlan(AddressSpacePlans.STANDARD_MEDIUM)
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
                        .withType(AddressSpaceType.STANDARD.toString().toLowerCase())
                        .withPlan(AddressSpacePlans.STANDARD_MEDIUM)
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
                        .withName("test-send-receive-standard")
                        .withNamespace(kubernetes.getInfraNamespace())
                        .endMetadata()
                        .withNewSpec()
                        .withType(AddressSpaceType.STANDARD.toString().toLowerCase())
                        .withPlan(AddressSpacePlans.STANDARD_MEDIUM)
                        .withNewAuthenticationService()
                        .withName("standard-authservice")
                        .endAuthenticationService()
                        .endSpec()
                        .build());
    }

    @Test
    void testCreateDeleteUsersLong() throws Exception {
        doTestCreateDeleteUsersLong(
                new AddressSpaceBuilder()
                        .withNewMetadata()
                        .withName("test-create-delete-users-standard")
                        .withNamespace(kubernetes.getInfraNamespace())
                        .endMetadata()
                        .withNewSpec()
                        .withType(AddressSpaceType.STANDARD.toString().toLowerCase())
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
                        .withNamespace(kubernetes.getInfraNamespace())
                        .endMetadata()
                        .withNewSpec()
                        .withType(AddressSpaceType.STANDARD.toString().toLowerCase())
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
                        .withNamespace(kubernetes.getInfraNamespace())
                        .endMetadata()
                        .withNewSpec()
                        .withType(AddressSpaceType.STANDARD.toString().toLowerCase())
                        .withPlan(AddressSpacePlans.STANDARD_MEDIUM)
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
                        .withName("standard-marathon-web-console")
                        .withNamespace(kubernetes.getInfraNamespace())
                        .endMetadata()
                        .withNewSpec()
                        .withType(AddressSpaceType.STANDARD.toString().toLowerCase())
                        .withPlan(AddressSpacePlans.STANDARD_MEDIUM)
                        .withNewAuthenticationService()
                        .withName("standard-authservice")
                        .endAuthenticationService()
                        .endSpec()
                        .build(),
                info.getTestClass().get().getName(), info.getTestMethod().get().getName());
    }
}
