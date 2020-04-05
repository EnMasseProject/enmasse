/*
 * Copyright 2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.systemtest.shared.brokered.web;

import io.enmasse.address.model.AddressBuilder;
import io.enmasse.systemtest.UserCredentials;
import io.enmasse.systemtest.annotations.ExternalClients;
import io.enmasse.systemtest.annotations.SeleniumFirefox;
import io.enmasse.systemtest.bases.web.ConsoleTest;
import io.enmasse.systemtest.model.address.AddressType;
import io.enmasse.systemtest.model.addressplan.DestinationPlan;
import io.enmasse.systemtest.model.addressspace.AddressSpacePlans;
import io.enmasse.systemtest.model.addressspace.AddressSpaceType;
import io.enmasse.systemtest.utils.AddressUtils;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static io.enmasse.systemtest.TestTag.SHARED;
import static org.junit.jupiter.api.Assertions.assertThrows;

@Tag(SHARED)
@SeleniumFirefox
class FirefoxConsoleTest extends ConsoleTest {

    @BeforeAll
    void initMessaging() throws Exception {
        resourceManager.createDefaultMessaging(AddressSpaceType.BROKERED, AddressSpacePlans.BROKERED);
    }

    @Test
    void testCreateDeleteQueue() throws Exception {
        doTestCreateDeleteAddress(resourceManager.getDefaultAddressSpace(), new AddressBuilder()
                .withNewMetadata()
                .withNamespace(resourceManager.getDefaultAddressSpace().getMetadata().getNamespace())
                .withName(AddressUtils.generateAddressMetadataName(resourceManager.getDefaultAddressSpace(), "test-queue"))
                .endMetadata()
                .withNewSpec()
                .withType("queue")
                .withAddress("test-queue")
                .withPlan(resourceManager.getDefaultAddressPlan(AddressType.QUEUE))
                .endSpec()
                .build());
    }


    @Test
    void testCreateDeleteTopic() throws Exception {
        doTestCreateDeleteAddress(resourceManager.getDefaultAddressSpace(), new AddressBuilder()
                .withNewMetadata()
                .withNamespace(resourceManager.getDefaultAddressSpace().getMetadata().getNamespace())
                .withName(AddressUtils.generateAddressMetadataName(resourceManager.getDefaultAddressSpace(), "test-topic"))
                .endMetadata()
                .withNewSpec()
                .withType("topic")
                .withAddress("test-topic")
                .withPlan(resourceManager.getDefaultAddressPlan(AddressType.TOPIC))
                .endSpec()
                .build());
    }

    @Test
    void testFilterAddressesByType() throws Exception {
        doTestFilterAddressesByType(resourceManager.getDefaultAddressSpace());
    }

    @Test
    void testFilterAddressesByName() throws Exception {
        doTestFilterAddressesByName(resourceManager.getDefaultAddressSpace());
    }

    @Test
    @ExternalClients
    void testPurgeAddress() throws Exception {
        doTestPurgeMessages(resourceManager.getDefaultAddressSpace());
    }

    @Test
    void testDeleteFilteredAddress() throws Exception {
        doTestDeleteFilteredAddress(resourceManager.getDefaultAddressSpace());
    }

    @Test
    void testSortAddressesByName() throws Exception {
        doTestSortAddressesByName(resourceManager.getDefaultAddressSpace());
    }

    @Test
    @ExternalClients
    @Disabled
    void testSortConnectionsBySenders() throws Exception {
        doTestSortConnectionsBySenders(resourceManager.getDefaultAddressSpace());
    }

    @Test
    @ExternalClients
    @Disabled
    void testSortConnectionsByReceivers() throws Exception {
        doTestSortConnectionsByReceivers(resourceManager.getDefaultAddressSpace());
    }

    @Test
    @ExternalClients
    @Disabled
    void testFilterConnectionsByContainerId() throws Exception {
        doTestFilterConnectionsByContainerId(resourceManager.getDefaultAddressSpace());
    }

    @Test
    @ExternalClients
    @Disabled
    void testSortConnectionsByContainerId() throws Exception {
        doTestSortConnectionsByContainerId(resourceManager.getDefaultAddressSpace());
    }

    @Test
    void testMessagesStoredMetrics() throws Exception {
        doTestMessagesStoredMetrics(resourceManager.getDefaultAddressSpace());
    }

    @Test
    @ExternalClients
    @Disabled
    void testClientsMetrics() throws Exception {
        doTestClientsMetrics(resourceManager.getDefaultAddressSpace());
    }

    @Test()
    void testCannotOpenConsolePage() {
        assertThrows(IllegalAccessException.class,
                () -> doTestCanOpenConsolePage(resourceManager.getDefaultAddressSpace(), new UserCredentials("noexistuser", "pepaPa555"), false));
    }

    @Test
    void testCanOpenConsolePage() throws Exception {
        doTestCanOpenConsolePage(resourceManager.getDefaultAddressSpace(), clusterUser, true);
    }

    @Test
    void testAddressStatus() throws Exception {
        doTestAddressStatus(resourceManager.getDefaultAddressSpace(), new AddressBuilder()
                .withNewMetadata()
                .withNamespace(resourceManager.getDefaultAddressSpace().getMetadata().getNamespace())
                .withName(AddressUtils.generateAddressMetadataName(resourceManager.getDefaultAddressSpace(), "test-queue"))
                .endMetadata()
                .withNewSpec()
                .withType("queue")
                .withAddress("test-queue")
                .withPlan(resourceManager.getDefaultAddressPlan(AddressType.QUEUE))
                .endSpec()
                .build());
        doTestAddressStatus(resourceManager.getDefaultAddressSpace(), new AddressBuilder()
                .withNewMetadata()
                .withNamespace(resourceManager.getDefaultAddressSpace().getMetadata().getNamespace())
                .withName(AddressUtils.generateAddressMetadataName(resourceManager.getDefaultAddressSpace(), "test-topic"))
                .endMetadata()
                .withNewSpec()
                .withType("queue")
                .withAddress("test-topic")
                .withPlan(resourceManager.getDefaultAddressPlan(AddressType.QUEUE))
                .endSpec()
                .build());
    }

    @Test
    void testValidAddressNames() throws Exception {
        doTestValidAddressNames(resourceManager.getDefaultAddressSpace());
    }

    @Test
    @ExternalClients
    void testAddressLinks() throws Exception {
        doTestAddressLinks(resourceManager.getDefaultAddressSpace(), DestinationPlan.BROKERED_QUEUE);
    }
}
