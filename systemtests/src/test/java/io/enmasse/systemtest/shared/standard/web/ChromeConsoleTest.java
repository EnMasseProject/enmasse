/*
 * Copyright 2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.systemtest.shared.standard.web;

import io.enmasse.address.model.AddressBuilder;
import io.enmasse.systemtest.UserCredentials;
import io.enmasse.systemtest.bases.shared.ITestSharedStandard;
import io.enmasse.systemtest.bases.web.ConsoleTest;
import io.enmasse.systemtest.messagingclients.ExternalClients;
import io.enmasse.systemtest.model.address.AddressType;
import io.enmasse.systemtest.model.addressplan.DestinationPlan;
import io.enmasse.systemtest.selenium.SeleniumChrome;
import io.enmasse.systemtest.utils.AddressUtils;

import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Disabled;

import static io.enmasse.systemtest.TestTag.NON_PR;

/**
*
* We are only adding a few tests for chrome because, the rest of the functionality is covered by firefox.
* Among other reasons, for not making the test runs too long.
*
*/
@Disabled
@Tag(NON_PR)
@SeleniumChrome
public class ChromeConsoleTest extends ConsoleTest implements ITestSharedStandard {

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
    void testDeleteFilteredAddress() throws Exception {
        doTestDeleteFilteredAddress(getSharedAddressSpace());
    }

    @Test
    void testSortAddressesByName() throws Exception {
        doTestSortAddressesByName(getSharedAddressSpace());
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
                AddressType.QUEUE, AddressType.ANYCAST, AddressType.MULTICAST, AddressType.TOPIC, AddressType.SUBSCRIPTION);
    }

    @Test
    void testVerylongAddressName() throws Exception {
        doTestWithStrangeAddressNames(getSharedAddressSpace(), false, true,
                AddressType.QUEUE, AddressType.ANYCAST, AddressType.MULTICAST, AddressType.TOPIC, AddressType.SUBSCRIPTION);
    }

    @Test
    void testCreateAddressWithSpecialCharsShowsErrorMessage() throws Exception {
        doTestCreateAddressWithSpecialCharsShowsErrorMessage(getSharedAddressSpace());
    }

}
