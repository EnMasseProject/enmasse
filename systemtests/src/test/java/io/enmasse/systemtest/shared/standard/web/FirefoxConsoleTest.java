/*
 * Copyright 2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.systemtest.shared.standard.web;

import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import io.enmasse.address.model.AddressBuilder;
import io.enmasse.systemtest.UserCredentials;
import io.enmasse.systemtest.bases.shared.ITestSharedStandard;
import io.enmasse.systemtest.bases.web.ConsoleTest;
import io.enmasse.systemtest.messagingclients.ExternalClients;
import io.enmasse.systemtest.model.address.AddressType;
import io.enmasse.systemtest.model.addressplan.DestinationPlan;
import io.enmasse.systemtest.selenium.SeleniumFirefox;
import io.enmasse.systemtest.utils.AddressUtils;

@SeleniumFirefox
@Disabled
public class FirefoxConsoleTest extends ConsoleTest implements ITestSharedStandard {

    @Test
    void testCreateDeleteQueue() throws Exception {
        doTestCreateDeleteAddress(getSharedAddressSpace(),
                new AddressBuilder()
                        .withNewMetadata()
                        .withNamespace(getSharedAddressSpace().getMetadata().getNamespace())
                        .withName(AddressUtils.generateAddressMetadataName(getSharedAddressSpace(), "test-queue2"))
                        .endMetadata()
                        .withNewSpec()
                        .withType("queue")
                        .withAddress("test-queue2")
                        .withPlan(DestinationPlan.STANDARD_SMALL_QUEUE)
                        .endSpec()
                        .build(),
                new AddressBuilder()
                        .withNewMetadata()
                        .withNamespace(getSharedAddressSpace().getMetadata().getNamespace())
                        .withName(AddressUtils.generateAddressMetadataName(getSharedAddressSpace(), "test-queue"))
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
        doTestCreateDeleteAddress(getSharedAddressSpace(),
                new AddressBuilder()
                        .withNewMetadata()
                        .withNamespace(getSharedAddressSpace().getMetadata().getNamespace())
                        .withName(AddressUtils.generateAddressMetadataName(getSharedAddressSpace(), "test-topic"))
                        .endMetadata()
                        .withNewSpec()
                        .withType("topic")
                        .withAddress("test-topic")
                        .withPlan(DestinationPlan.STANDARD_LARGE_TOPIC)
                        .endSpec()
                        .build(),
                new AddressBuilder()
                        .withNewMetadata()
                        .withNamespace(getSharedAddressSpace().getMetadata().getNamespace())
                        .withName(AddressUtils.generateAddressMetadataName(getSharedAddressSpace(), "test-topic2"))
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
     doTestCreateDeleteAddress(getSharedAddressSpace(),
                new AddressBuilder()
                        .withNewMetadata()
                        .withNamespace(getSharedAddressSpace().getMetadata().getNamespace())
                        .withName(AddressUtils.generateAddressMetadataName(getSharedAddressSpace(), "test-topic"))
                        .endMetadata()
                        .withNewSpec()
                        .withType("topic")
                        .withAddress("test-topic")
                        .withPlan(DestinationPlan.STANDARD_LARGE_TOPIC)
                        .endSpec()
                        .build(),
                new AddressBuilder()
                        .withNewMetadata()
                        .withNamespace(getSharedAddressSpace().getMetadata().getNamespace())
                        .withName(AddressUtils.generateAddressMetadataName(getSharedAddressSpace(), "test-sub"))
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
        doTestCreateDeleteAddress(getSharedAddressSpace(), new AddressBuilder()
                .withNewMetadata()
                .withNamespace(getSharedAddressSpace().getMetadata().getNamespace())
                .withName(AddressUtils.generateAddressMetadataName(getSharedAddressSpace(), "test-anycast"))
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
        doTestCreateDeleteAddress(getSharedAddressSpace(), new AddressBuilder()
                .withNewMetadata()
                .withNamespace(getSharedAddressSpace().getMetadata().getNamespace())
                .withName(AddressUtils.generateAddressMetadataName(getSharedAddressSpace(), "test-multicast"))
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
        doTestAddressStatus(getSharedAddressSpace(), new AddressBuilder()
                .withNewMetadata()
                .withNamespace(getSharedAddressSpace().getMetadata().getNamespace())
                .withName(AddressUtils.generateAddressMetadataName(getSharedAddressSpace(), "test-anycast"))
                .endMetadata()
                .withNewSpec()
                .withType("anycast")
                .withAddress("test-anycast")
                .withPlan(DestinationPlan.STANDARD_SMALL_ANYCAST)
                .endSpec()
                .build());
        doTestAddressStatus(getSharedAddressSpace(), new AddressBuilder()
                .withNewMetadata()
                .withNamespace(getSharedAddressSpace().getMetadata().getNamespace())
                .withName(AddressUtils.generateAddressMetadataName(getSharedAddressSpace(), "test-multicast"))
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
        doTestFilterAddressesByType(getSharedAddressSpace());
    }

    @Test
    void testFilterAddressesByName() throws Exception {
        doTestFilterAddressesByName(getSharedAddressSpace());
    }

    @Test
    void testFilterAddressesByStatus() throws Exception {
        doTestFilterAddressesByStatus(getSharedAddressSpace());
    }

    @Test
    @ExternalClients
    void testPurgeAddress() throws Exception {
        doTestPurgeMessages(getSharedAddressSpace());
    }

    @Test
    void testEditAddress() throws Exception {
        doTestEditAddress(getSharedAddressSpace(), new AddressBuilder()
                .withNewMetadata()
                .withNamespace(getSharedAddressSpace().getMetadata().getNamespace())
                .withName(AddressUtils.generateAddressMetadataName(getSharedAddressSpace(), "test-queue-edit"))
                .endMetadata()
                .withNewSpec()
                .withType("queue")
                .withAddress("test-queue-edit")
                .withPlan(getDefaultPlan(AddressType.QUEUE))
                .endSpec()
                .build(),
                DestinationPlan.STANDARD_XLARGE_QUEUE);
    }

    @Test
    void testDeleteFilteredAddress() throws Exception {
        doTestDeleteFilteredAddress(getSharedAddressSpace());
    }

//    @Test
//    void testFilterAddressWithRegexSymbols() throws Exception {
//        doTestFilterAddressWithRegexSymbols();
//    }
//
//    @Test
//    void testRegexAlertBehavesConsistently() throws Exception {
//        doTestRegexAlertBehavesConsistently();
//    }

    @Test
    void testSortAddressesByName() throws Exception {
        doTestSortAddressesByName(getSharedAddressSpace());
    }

//    @Test
//    @ExternalClients
//    void testSortAddressesByClients() throws Exception {
//        doTestSortAddressesByClients(getSharedAddressSpace());
//    }

    @Test
    @ExternalClients
    void testSortAddressesBySenders() throws Exception {
        doTestSortAddressesBySenders(getSharedAddressSpace());
    }

    @Test
    @ExternalClients
    void testSortAddressesByReceivers() throws Exception {
        doTestSortAddressesByReceivers(getSharedAddressSpace());
    }

    @Test
    @ExternalClients
    void testSortConnectionsBySenders() throws Exception {
        doTestSortConnectionsBySenders(getSharedAddressSpace());
    }

    @Test
    @ExternalClients
    void testSortConnectionsByReceivers() throws Exception {
        doTestSortConnectionsByReceivers(getSharedAddressSpace());
    }

//    @Test
//    @ExternalClients
//    void testFilterConnectionsByEncrypted() throws Exception {
//        doTestFilterConnectionsByEncrypted();
//    }
//
//    @Test
//    @ExternalClients
//    @Disabled("related issue: #667")
//    void testFilterConnectionsByUser() throws Exception {
//        doTestFilterConnectionsByUser();
//    }

    //hostname tests doesn't make much sense
//    @Test
//    @ExternalClients
//    void testFilterConnectionsByHostname() throws Exception {
//        doTestFilterConnectionsByHostname(getSharedAddressSpace());
//    }
//
//    @Test
//    @ExternalClients
//    void testSortConnectionsByHostname() throws Exception {
//        doTestSortConnectionsByHostname(getSharedAddressSpace());
//    }

    @Test
    @ExternalClients
    void testFilterConnectionsByContainerId() throws Exception {
        doTestFilterConnectionsByContainerId(getSharedAddressSpace());
    }

    @Test
    @ExternalClients
    void testSortConnectionsByContainerId() throws Exception {
        doTestSortConnectionsByContainerId(getSharedAddressSpace());
    }

    @Test
    void testMessagesStoredMetrics() throws Exception {
        doTestMessagesStoredMetrics(getSharedAddressSpace());
    }

    @Test
    @ExternalClients
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
    void testAddressNameWithHyphens() throws Exception {
        doTestWithStrangeAddressNames(getSharedAddressSpace(), true, false,
                AddressType.QUEUE, AddressType.ANYCAST, AddressType.MULTICAST, AddressType.TOPIC, AddressType.SUBSCRIPTION
        );
    }

    @Test
    void testVerylongAddressName() throws Exception {
        doTestWithStrangeAddressNames(getSharedAddressSpace(), false, true,
                AddressType.QUEUE, AddressType.ANYCAST, AddressType.MULTICAST, AddressType.TOPIC, AddressType.SUBSCRIPTION
        );
    }

    @Test
    void testCreateAddressWithSpecialCharsShowsErrorMessage() throws Exception {
        doTestCreateAddressWithSpecialCharsShowsErrorMessage(getSharedAddressSpace());
    }

}
