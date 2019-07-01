/*
 * Copyright 2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.systemtest.standard.web;

import io.enmasse.address.model.AddressBuilder;
import io.enmasse.systemtest.AddressType;
import io.enmasse.systemtest.DestinationPlan;
import io.enmasse.systemtest.UserCredentials;
import io.enmasse.systemtest.ability.ITestBaseStandard;
import io.enmasse.systemtest.bases.web.WebConsoleTest;
import io.enmasse.systemtest.messagingclients.ExternalClients;
import io.enmasse.systemtest.selenium.ISeleniumProviderFirefox;
import io.enmasse.systemtest.utils.AddressUtils;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertThrows;

public class FirefoxWebConsoleTest extends WebConsoleTest implements ITestBaseStandard, ISeleniumProviderFirefox {

    @Test
    void testCreateDeleteQueue() throws Exception {
        doTestCreateDeleteAddress(
                new AddressBuilder()
                        .withNewMetadata()
                        .withNamespace(sharedAddressSpace.getMetadata().getNamespace())
                        .withName(AddressUtils.generateAddressMetadataName(sharedAddressSpace, "test-queue"))
                        .endMetadata()
                        .withNewSpec()
                        .withType("queue")
                        .withAddress("test-queue")
                        .withPlan(DestinationPlan.STANDARD_LARGE_QUEUE)
                        .endSpec()
                        .build(),
                new AddressBuilder()
                        .withNewMetadata()
                        .withNamespace(sharedAddressSpace.getMetadata().getNamespace())
                        .withName(AddressUtils.generateAddressMetadataName(sharedAddressSpace, "test-queue2"))
                        .endMetadata()
                        .withNewSpec()
                        .withType("queue")
                        .withAddress("test-queue2")
                        .withPlan(DestinationPlan.STANDARD_SMALL_QUEUE)
                        .endSpec()
                        .build());
    }

    @Test
    void testCreateDeleteTopic() throws Exception {
        doTestCreateDeleteAddress(
                new AddressBuilder()
                        .withNewMetadata()
                        .withNamespace(sharedAddressSpace.getMetadata().getNamespace())
                        .withName(AddressUtils.generateAddressMetadataName(sharedAddressSpace, "test-topic"))
                        .endMetadata()
                        .withNewSpec()
                        .withType("topic")
                        .withAddress("test-topic")
                        .withPlan(DestinationPlan.STANDARD_LARGE_TOPIC)
                        .endSpec()
                        .build(),
                new AddressBuilder()
                        .withNewMetadata()
                        .withNamespace(sharedAddressSpace.getMetadata().getNamespace())
                        .withName(AddressUtils.generateAddressMetadataName(sharedAddressSpace, "test-topic2"))
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
        doTestCreateDeleteDurableSubscription(
                new AddressBuilder()
                        .withNewMetadata()
                        .withNamespace(sharedAddressSpace.getMetadata().getNamespace())
                        .withName(AddressUtils.generateAddressMetadataName(sharedAddressSpace, "test-topic"))
                        .endMetadata()
                        .withNewSpec()
                        .withType("topic")
                        .withAddress("test-topic")
                        .withPlan(DestinationPlan.STANDARD_LARGE_TOPIC)
                        .endSpec()
                        .build(),
                new AddressBuilder()
                        .withNewMetadata()
                        .withNamespace(sharedAddressSpace.getMetadata().getNamespace())
                        .withName(AddressUtils.generateAddressMetadataName(sharedAddressSpace, "test-topic2"))
                        .endMetadata()
                        .withNewSpec()
                        .withType("topic")
                        .withAddress("test-topic2")
                        .withPlan(DestinationPlan.STANDARD_SMALL_TOPIC)
                        .endSpec()
                        .build());
    }

    @Test
    void testCreateDeleteAnycast() throws Exception {
        doTestCreateDeleteAddress(new AddressBuilder()
                .withNewMetadata()
                .withNamespace(sharedAddressSpace.getMetadata().getNamespace())
                .withName(AddressUtils.generateAddressMetadataName(sharedAddressSpace, "test-anycast"))
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
        doTestCreateDeleteAddress(new AddressBuilder()
                .withNewMetadata()
                .withNamespace(sharedAddressSpace.getMetadata().getNamespace())
                .withName(AddressUtils.generateAddressMetadataName(sharedAddressSpace, "test-multicast"))
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
        doTestFilterAddressesByType();
    }

    @Test
    void testFilterAddressesByName() throws Exception {
        doTestFilterAddressesByName();
    }

    @Test
    void testDeleteFilteredAddress() throws Exception {
        doTestDeleteFilteredAddress();
    }

    @Test
    void testFilterAddressWithRegexSymbols() throws Exception {
        doTestFilterAddressWithRegexSymbols();
    }

    @Test
    void testRegexAlertBehavesConsistently() throws Exception {
        doTestRegexAlertBehavesConsistently();
    }

    @Test
    void testSortAddressesByName() throws Exception {
        doTestSortAddressesByName();
    }

    @Test
    @ExternalClients
    void testSortAddressesByClients() throws Exception {
        doTestSortAddressesByClients();
    }

    @Test
    @ExternalClients
    void testSortConnectionsBySenders() throws Exception {
        doTestSortConnectionsBySenders();
    }

    @Test
    @ExternalClients
    void testSortConnectionsByReceivers() throws Exception {
        doTestSortConnectionsByReceivers();
    }

    @Test
    @ExternalClients
    void testFilterConnectionsByEncrypted() throws Exception {
        doTestFilterConnectionsByEncrypted();
    }

    @Test
    @ExternalClients
    @Disabled("related issue: #667")
    void testFilterConnectionsByUser() throws Exception {
        doTestFilterConnectionsByUser();
    }

    @Test
    @ExternalClients
    void testFilterConnectionsByHostname() throws Exception {
        doTestFilterConnectionsByHostname();
    }

    @Test
    @ExternalClients
    void testSortConnectionsByHostname() throws Exception {
        doTestSortConnectionsByHostname();
    }

    @Test
    @ExternalClients
    void testFilterConnectionsByContainerId() throws Exception {
        doTestFilterConnectionsByContainerId();
    }

    @Test
    @ExternalClients
    void testSortConnectionsByContainerId() throws Exception {
        doTestSortConnectionsByContainerId();
    }

    @Test
    void testMessagesMetrics() throws Exception {
        doTestMessagesMetrics();
    }

    @Test
    @ExternalClients
    void testClientsMetrics() throws Exception {
        doTestClientsMetrics();
    }

    @Test()
    void testCannotOpenConsolePage() {
        assertThrows(IllegalAccessException.class,
                () -> doTestCanOpenConsolePage(new UserCredentials("noexistuser", "pepaPa555")));
    }

    @Test
    void testCanOpenConsolePage() throws Exception {
        doTestCanOpenConsolePage(clusterUser);
    }

    @Test
    void testAddressStatus() throws Exception {
        doTestAddressStatus(new AddressBuilder()
                .withNewMetadata()
                .withNamespace(sharedAddressSpace.getMetadata().getNamespace())
                .withName(AddressUtils.generateAddressMetadataName(sharedAddressSpace, "test-queue"))
                .endMetadata()
                .withNewSpec()
                .withType("queue")
                .withAddress("test-queue")
                .withPlan(getDefaultPlan(AddressType.QUEUE))
                .endSpec()
                .build());
        doTestAddressStatus(new AddressBuilder()
                .withNewMetadata()
                .withNamespace(sharedAddressSpace.getMetadata().getNamespace())
                .withName(AddressUtils.generateAddressMetadataName(sharedAddressSpace, "test-topic"))
                .endMetadata()
                .withNewSpec()
                .withType("queue")
                .withAddress("test-topic")
                .withPlan(getDefaultPlan(AddressType.QUEUE))
                .endSpec()
                .build());
        doTestAddressStatus(new AddressBuilder()
                .withNewMetadata()
                .withNamespace(sharedAddressSpace.getMetadata().getNamespace())
                .withName(AddressUtils.generateAddressMetadataName(sharedAddressSpace, "test-anycast"))
                .endMetadata()
                .withNewSpec()
                .withType("anycast")
                .withAddress("test-anycast")
                .withPlan(DestinationPlan.STANDARD_SMALL_ANYCAST)
                .endSpec()
                .build());
        doTestAddressStatus(new AddressBuilder()
                .withNewMetadata()
                .withNamespace(sharedAddressSpace.getMetadata().getNamespace())
                .withName(AddressUtils.generateAddressMetadataName(sharedAddressSpace, "test-multicast"))
                .endMetadata()
                .withNewSpec()
                .withType("multicast")
                .withAddress("test-multicast")
                .withPlan(DestinationPlan.STANDARD_SMALL_MULTICAST)
                .endSpec()
                .build());
    }

    @Test
    @Disabled("disabled due to #1601")
    void testAddressNameWithHyphens() throws Exception {
        doTestWithStrangeAddressNames(true, false,
                AddressType.QUEUE, AddressType.ANYCAST, AddressType.MULTICAST, AddressType.TOPIC, AddressType.SUBSCRIPTION
        );
    }

    @Test
    void testVerylongAddressName() throws Exception {
        doTestWithStrangeAddressNames(false, true,
                AddressType.QUEUE, AddressType.ANYCAST, AddressType.MULTICAST, AddressType.TOPIC, AddressType.SUBSCRIPTION
        );
    }

    @Test
    void testCreateAddressWithSpecialCharsShowsErrorMessage() throws Exception {
        doTestCreateAddressWithSpecialCharsShowsErrorMessage();
    }

    @Test
    void testAddressWithValidPlanOnly() throws Exception {
        doTestAddressWithValidPlanOnly();
    }

    @Test
    void testOpenConsoleCustomRoute() throws Exception {
        doTestOpenConsoleCustomRoute();
    }
}
