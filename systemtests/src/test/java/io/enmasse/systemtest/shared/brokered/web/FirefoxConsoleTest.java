/*
 * Copyright 2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.systemtest.shared.brokered.web;

import io.enmasse.address.model.AddressBuilder;
import io.enmasse.systemtest.UserCredentials;
import io.enmasse.systemtest.bases.shared.ITestSharedBrokered;
import io.enmasse.systemtest.bases.web.ConsoleTest;
import io.enmasse.systemtest.messagingclients.ExternalClients;
import io.enmasse.systemtest.model.address.AddressType;
import io.enmasse.systemtest.selenium.SeleniumFirefox;
import io.enmasse.systemtest.utils.AddressUtils;

import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

@SeleniumFirefox
class FirefoxConsoleTest extends ConsoleTest implements ITestSharedBrokered {

    @Test
    void testCreateDeleteQueue() throws Exception {
        doTestCreateDeleteAddress(getSharedAddressSpace(), new AddressBuilder()
                .withNewMetadata()
                .withNamespace(getSharedAddressSpace().getMetadata().getNamespace())
                .withName(AddressUtils.generateAddressMetadataName(getSharedAddressSpace(), "test-queue"))
                .endMetadata()
                .withNewSpec()
                .withType("queue")
                .withAddress("test-queue")
                .withPlan(getDefaultPlan(AddressType.QUEUE))
                .endSpec()
                .build());
    }


    @Test
    void testCreateDeleteTopic() throws Exception {
        doTestCreateDeleteAddress(getSharedAddressSpace(), new AddressBuilder()
                .withNewMetadata()
                .withNamespace(getSharedAddressSpace().getMetadata().getNamespace())
                .withName(AddressUtils.generateAddressMetadataName(getSharedAddressSpace(), "test-topic"))
                .endMetadata()
                .withNewSpec()
                .withType("topic")
                .withAddress("test-topic")
                .withPlan(getDefaultPlan(AddressType.TOPIC))
                .endSpec()
                .build());
    }

    @Test
    void testFilterAddressesByType() throws Exception {
        doTestFilterAddressesByType(getSharedAddressSpace());
    }

    @Test
    void testFilterAddressesByName() throws Exception {
        doTestFilterAddressesByName(getSharedAddressSpace());
    }

    @Test
    @ExternalClients
    void testPurgeAddress() throws Exception {
        doTestPurgeMessages(getSharedAddressSpace());
    }

    @Test
    void testDeleteFilteredAddress() throws Exception {
        doTestDeleteFilteredAddress(getSharedAddressSpace());
    }

    @Test
    void testSortAddressesByName() throws Exception {
        doTestSortAddressesByName(getSharedAddressSpace());
    }

    @Test
    @ExternalClients
    @Disabled
    void testSortConnectionsBySenders() throws Exception {
        doTestSortConnectionsBySenders(getSharedAddressSpace());
    }

    @Test
    @ExternalClients
    @Disabled
    void testSortConnectionsByReceivers() throws Exception {
        doTestSortConnectionsByReceivers(getSharedAddressSpace());
    }

    @Test
    @ExternalClients
    @Disabled
    void testFilterConnectionsByContainerId() throws Exception {
        doTestFilterConnectionsByContainerId(getSharedAddressSpace());
    }

    @Test
    @ExternalClients
    @Disabled
    void testSortConnectionsByContainerId() throws Exception {
        doTestSortConnectionsByContainerId(getSharedAddressSpace());
    }

    @Test
    void testMessagesStoredMetrics() throws Exception {
        doTestMessagesStoredMetrics(getSharedAddressSpace());
    }

    @Test
    @ExternalClients
    @Disabled
    void testClientsMetrics() throws Exception {
        doTestClientsMetrics(getSharedAddressSpace());
    }

    @Test()
    void testCannotOpenConsolePage() {
        assertThrows(IllegalAccessException.class,
                () -> doTestCanOpenConsolePage(getSharedAddressSpace(), new UserCredentials("noexistuser", "pepaPa555"), false));
    }

    @Test
    void testCanOpenConsolePage() throws Exception {
        doTestCanOpenConsolePage(getSharedAddressSpace(), clusterUser, true);
    }

    @Test
    void testAddressStatus() throws Exception {
        doTestAddressStatus(getSharedAddressSpace(), new AddressBuilder()
                .withNewMetadata()
                .withNamespace(getSharedAddressSpace().getMetadata().getNamespace())
                .withName(AddressUtils.generateAddressMetadataName(getSharedAddressSpace(), "test-queue"))
                .endMetadata()
                .withNewSpec()
                .withType("queue")
                .withAddress("test-queue")
                .withPlan(getDefaultPlan(AddressType.QUEUE))
                .endSpec()
                .build());
        doTestAddressStatus(getSharedAddressSpace(), new AddressBuilder()
                .withNewMetadata()
                .withNamespace(getSharedAddressSpace().getMetadata().getNamespace())
                .withName(AddressUtils.generateAddressMetadataName(getSharedAddressSpace(), "test-topic"))
                .endMetadata()
                .withNewSpec()
                .withType("queue")
                .withAddress("test-topic")
                .withPlan(getDefaultPlan(AddressType.QUEUE))
                .endSpec()
                .build());
    }

    @Test
    void testCreateAddressWithSpecialCharsShowsErrorMessage() throws Exception {
        doTestCreateAddressWithSpecialCharsShowsErrorMessage(getSharedAddressSpace());
    }

}
