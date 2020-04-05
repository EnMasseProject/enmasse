/*
 * Copyright 2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.systemtest.shared.standard.web;

import io.enmasse.address.model.AddressBuilder;
import io.enmasse.systemtest.UserCredentials;
import io.enmasse.systemtest.annotations.DefaultMessaging;
import io.enmasse.systemtest.annotations.ExternalClients;
import io.enmasse.systemtest.annotations.SeleniumFirefox;
import io.enmasse.systemtest.bases.web.ConsoleTest;
import io.enmasse.systemtest.info.TestInfo;
import io.enmasse.systemtest.model.address.AddressType;
import io.enmasse.systemtest.model.addressplan.DestinationPlan;
import io.enmasse.systemtest.model.addressspace.AddressSpacePlans;
import io.enmasse.systemtest.model.addressspace.AddressSpaceType;
import io.enmasse.systemtest.utils.AddressUtils;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static io.enmasse.systemtest.TestTag.SHARED;
import static org.junit.jupiter.api.Assertions.assertThrows;

@Tag(SHARED)
@SeleniumFirefox
@DefaultMessaging(type = AddressSpaceType.STANDARD, plan = AddressSpacePlans.STANDARD_UNLIMITED)
public class FirefoxConsoleTest extends ConsoleTest {

    @Test
    void testCreateDeleteQueue() throws Exception {
        doTestCreateDeleteAddress(resourceManager.getDefaultAddressSpace(),
                new AddressBuilder()
                        .withNewMetadata()
                        .withNamespace(resourceManager.getDefaultAddressSpace().getMetadata().getNamespace())
                        .withName(AddressUtils.generateAddressMetadataName(resourceManager.getDefaultAddressSpace(), "test-queue2"))
                        .endMetadata()
                        .withNewSpec()
                        .withType("queue")
                        .withAddress("test-queue2")
                        .withPlan(DestinationPlan.STANDARD_SMALL_QUEUE)
                        .endSpec()
                        .build(),
                new AddressBuilder()
                        .withNewMetadata()
                        .withNamespace(resourceManager.getDefaultAddressSpace().getMetadata().getNamespace())
                        .withName(AddressUtils.generateAddressMetadataName(resourceManager.getDefaultAddressSpace(), "test-queue"))
                        .endMetadata()
                        .withNewSpec()
                        .withType("queue")
                        .withAddress("test-queue")
                        .withPlan(DestinationPlan.STANDARD_LARGE_QUEUE)
                        .endSpec()
                        .build());
    }


    @Test
    void testCreateDeleteTopic() throws Exception {
        doTestCreateDeleteAddress(resourceManager.getDefaultAddressSpace(),
                new AddressBuilder()
                        .withNewMetadata()
                        .withNamespace(resourceManager.getDefaultAddressSpace().getMetadata().getNamespace())
                        .withName(AddressUtils.generateAddressMetadataName(resourceManager.getDefaultAddressSpace(), "test-topic"))
                        .endMetadata()
                        .withNewSpec()
                        .withType("topic")
                        .withAddress("test-topic")
                        .withPlan(DestinationPlan.STANDARD_LARGE_TOPIC)
                        .endSpec()
                        .build(),
                new AddressBuilder()
                        .withNewMetadata()
                        .withNamespace(resourceManager.getDefaultAddressSpace().getMetadata().getNamespace())
                        .withName(AddressUtils.generateAddressMetadataName(resourceManager.getDefaultAddressSpace(), "test-topic2"))
                        .endMetadata()
                        .withNewSpec()
                        .withType("topic")
                        .withAddress("test-topic2")
                        .withPlan(DestinationPlan.STANDARD_SMALL_TOPIC)
                        .endSpec()
                        .build());
    }

    @Test
    void testCreateDeleteDurableSubscription() throws Exception {
     doTestCreateDeleteAddress(resourceManager.getDefaultAddressSpace(),
                new AddressBuilder()
                        .withNewMetadata()
                        .withNamespace(resourceManager.getDefaultAddressSpace().getMetadata().getNamespace())
                        .withName(AddressUtils.generateAddressMetadataName(resourceManager.getDefaultAddressSpace(), "test-topic"))
                        .endMetadata()
                        .withNewSpec()
                        .withType("topic")
                        .withAddress("test-topic")
                        .withPlan(DestinationPlan.STANDARD_LARGE_TOPIC)
                        .endSpec()
                        .build(),
                new AddressBuilder()
                        .withNewMetadata()
                        .withNamespace(resourceManager.getDefaultAddressSpace().getMetadata().getNamespace())
                        .withName(AddressUtils.generateAddressMetadataName(resourceManager.getDefaultAddressSpace(), "test-sub"))
                        .endMetadata()
                        .withNewSpec()
                        .withType("subscription")
                        .withAddress("test-sub")
                        .withTopic("test-topic")
                        .withPlan(DestinationPlan.STANDARD_LARGE_SUBSCRIPTION)
                        .endSpec()
                        .build());
    }

    @Test
    void testCreateDeleteAnycast() throws Exception {
        doTestCreateDeleteAddress(resourceManager.getDefaultAddressSpace(), new AddressBuilder()
                .withNewMetadata()
                .withNamespace(resourceManager.getDefaultAddressSpace().getMetadata().getNamespace())
                .withName(AddressUtils.generateAddressMetadataName(resourceManager.getDefaultAddressSpace(), "test-anycast"))
                .endMetadata()
                .withNewSpec()
                .withType("anycast")
                .withAddress("test-anycast")
                .withPlan(DestinationPlan.STANDARD_SMALL_ANYCAST)
                .endSpec()
                .build());
    }

    @Test
    void testCreateDeleteMulticast() throws Exception {
        doTestCreateDeleteAddress(resourceManager.getDefaultAddressSpace(), new AddressBuilder()
                .withNewMetadata()
                .withNamespace(resourceManager.getDefaultAddressSpace().getMetadata().getNamespace())
                .withName(AddressUtils.generateAddressMetadataName(resourceManager.getDefaultAddressSpace(), "test-multicast"))
                .endMetadata()
                .withNewSpec()
                .withType("multicast")
                .withAddress("test-multicast")
                .withPlan(DestinationPlan.STANDARD_SMALL_MULTICAST)
                .endSpec()
                .build());
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
        doTestAddressStatus(resourceManager.getDefaultAddressSpace(), new AddressBuilder()
                .withNewMetadata()
                .withNamespace(resourceManager.getDefaultAddressSpace().getMetadata().getNamespace())
                .withName(AddressUtils.generateAddressMetadataName(resourceManager.getDefaultAddressSpace(), "test-anycast"))
                .endMetadata()
                .withNewSpec()
                .withType("anycast")
                .withAddress("test-anycast")
                .withPlan(DestinationPlan.STANDARD_SMALL_ANYCAST)
                .endSpec()
                .build());
        doTestAddressStatus(resourceManager.getDefaultAddressSpace(), new AddressBuilder()
                .withNewMetadata()
                .withNamespace(resourceManager.getDefaultAddressSpace().getMetadata().getNamespace())
                .withName(AddressUtils.generateAddressMetadataName(resourceManager.getDefaultAddressSpace(), "test-multicast"))
                .endMetadata()
                .withNewSpec()
                .withType("multicast")
                .withAddress("test-multicast")
                .withPlan(DestinationPlan.STANDARD_SMALL_MULTICAST)
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
    void testFilterAddressesByStatus() throws Exception {
        doTestFilterAddressesByStatus(resourceManager.getDefaultAddressSpace());
    }

    @Test
    @ExternalClients
    void testPurgeAddress() throws Exception {
        doTestPurgeMessages(resourceManager.getDefaultAddressSpace());
    }

    @Test
    void testEditAddress() throws Exception {
        doTestEditAddress(resourceManager.getDefaultAddressSpace(), new AddressBuilder()
                .withNewMetadata()
                .withNamespace(resourceManager.getDefaultAddressSpace().getMetadata().getNamespace())
                .withName(AddressUtils.generateAddressMetadataName(resourceManager.getDefaultAddressSpace(), "test-queue-edit"))
                .endMetadata()
                .withNewSpec()
                .withType("queue")
                .withAddress("test-queue-edit")
                .withPlan(resourceManager.getDefaultAddressPlan(AddressType.QUEUE))
                .endSpec()
                .build(),
                DestinationPlan.STANDARD_XLARGE_QUEUE);
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
    void testSortAddressesBySenders() throws Exception {
        doTestSortAddressesBySenders(resourceManager.getDefaultAddressSpace());
    }

    @Test
    @ExternalClients
    void testSortAddressesByReceivers() throws Exception {
        doTestSortAddressesByReceivers(resourceManager.getDefaultAddressSpace());
    }

    @Test
    @ExternalClients
    void testSortConnectionsBySenders() throws Exception {
        doTestSortConnectionsBySenders(resourceManager.getDefaultAddressSpace());
    }

    @Test
    @ExternalClients
    void testSortConnectionsByReceivers() throws Exception {
        doTestSortConnectionsByReceivers(resourceManager.getDefaultAddressSpace());
    }

    @Test
    @ExternalClients
    void testAddressLinks() throws Exception {
        doTestAddressLinks(resourceManager.getDefaultAddressSpace(), DestinationPlan.STANDARD_SMALL_QUEUE);
    }

    @Test
    @ExternalClients
    void testFilterConnectionsByContainerId() throws Exception {
        doTestFilterConnectionsByContainerId(resourceManager.getDefaultAddressSpace());
    }

    @Disabled("Filtering based on ID is not supported now.") // Todo: Enable after filtering by Id will be supported
    @Test
    @ExternalClients
    void testFilterLinksByContainerId() throws Exception {
        doTestFilterClientsByContainerId(resourceManager.getDefaultAddressSpace());
    }

    @Test
    @ExternalClients
    void testFilterLinksByName() throws Exception {
        doTestFilterClientsByName(resourceManager.getDefaultAddressSpace());
    }


    @Test
    @ExternalClients
    void testSortConnectionsByContainerId() throws Exception {
        doTestSortConnectionsByContainerId(resourceManager.getDefaultAddressSpace());
    }

    @Test
    void testMessagesStoredMetrics() throws Exception {
        doTestMessagesStoredMetrics(resourceManager.getDefaultAddressSpace());
    }

    @Test
    @ExternalClients
    void testClientsMetrics() throws Exception {
        doTestClientsMetrics(resourceManager.getDefaultAddressSpace());
    }

    @Test
    @ExternalClients
    void testEmptyLinkPage() throws Exception {
        doTestEmptyLinkPage(resourceManager.getDefaultAddressSpace(), TestInfo.getInstance().getActualTest());
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
    void testVerylongAddressName() throws Exception {
        doTestWithStrangeAddressNames(resourceManager.getDefaultAddressSpace(), false, true,
                AddressType.QUEUE, AddressType.ANYCAST, AddressType.MULTICAST, AddressType.TOPIC, AddressType.SUBSCRIPTION
        );
    }

    @Test
    void testValidAddressNames() throws Exception {
        doTestValidAddressNames(resourceManager.getDefaultAddressSpace());
    }

    @Test
    void testErrorDialog() throws Exception {
        doTestErrorDialog(resourceManager.getDefaultAddressSpace());
    }

}
