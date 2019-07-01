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
import io.enmasse.systemtest.selenium.ISeleniumProviderChrome;
import io.enmasse.systemtest.utils.AddressUtils;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static io.enmasse.systemtest.TestTag.nonPR;
import static org.junit.jupiter.api.Assertions.assertThrows;

@Tag(nonPR)
public class ChromeWebConsoleTest extends WebConsoleTest implements ITestBaseStandard, ISeleniumProviderChrome {

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
    @Disabled("Only few chrome tests are enabled, rest functionality is covered by firefox")
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
    @Disabled("Only few chrome tests are enabled, rest functionality is covered by firefox")
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
    @Disabled("Only few chrome tests are enabled, rest functionality is covered by firefox")
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
    @Disabled("Only few chrome tests are enabled, rest functionality is covered by firefox")
    void testFilterAddressesByName() throws Exception {
        doTestFilterAddressesByName();
    }

    @Test
    @Disabled("Only few chrome tests are enabled, rest functionality is covered by firefox")
    void testDeleteFilteredAddress() throws Exception {
        doTestDeleteFilteredAddress();
    }

    @Test
    @Disabled("Only few chrome tests are enabled, rest functionality is covered by firefox")
    void testFilterAddressWithRegexSymbols() throws Exception {
        doTestFilterAddressWithRegexSymbols();
    }

    @Test
    @Disabled("Only few chrome tests are enabled, rest functionality is covered by firefox")
    void testRegexAlertBehavesConsistently() throws Exception {
        doTestRegexAlertBehavesConsistently();
    }

    @Test
    void testSortAddressesByName() throws Exception {
        doTestSortAddressesByName();
    }

    @Test
    @ExternalClients
    @Disabled("Only few chrome tests are enabled, rest functionality is covered by firefox")
    void testSortConnectionsBySenders() throws Exception {
        doTestSortConnectionsBySenders();
    }

    @Test
    @ExternalClients
    @Disabled("Only few chrome tests are enabled, rest functionality is covered by firefox")
    void testSortConnectionsByReceivers() throws Exception {
        doTestSortConnectionsByReceivers();
    }

    @Test
    @ExternalClients
    @Disabled("Only few chrome tests are enabled, rest functionality is covered by firefox")
    void testFilterConnectionsByEncrypted() throws Exception {
        doTestFilterConnectionsByEncrypted();
    }

    @Test
    @ExternalClients
    @Disabled("Only few chrome tests are enabled, rest functionality is covered by firefox")
    void testFilterConnectionsByUser() throws Exception {
        doTestFilterConnectionsByUser();
    }

    @Test
    @ExternalClients
    @Disabled("Only few chrome tests are enabled, rest functionality is covered by firefox")
    void testFilterConnectionsByHostname() throws Exception {
        doTestFilterConnectionsByHostname();
    }

    @Test
    @ExternalClients
    @Disabled("Only few chrome tests are enabled, rest functionality is covered by firefox")
    void testSortConnectionsByHostname() throws Exception {
        doTestSortConnectionsByHostname();
    }

    @Test
    @ExternalClients
    @Disabled("Only few chrome tests are enabled, rest functionality is covered by firefox")
    void testFilterConnectionsByContainerId() throws Exception {
        doTestFilterConnectionsByContainerId();
    }

    @Test
    @ExternalClients
    @Disabled("Only few chrome tests are enabled, rest functionality is covered by firefox")
    void testSortConnectionsByContainerId() throws Exception {
        doTestSortConnectionsByContainerId();
    }

    @Test
    @Disabled("Only few chrome tests are enabled, rest functionality is covered by firefox")
    void testMessagesMetrics() throws Exception {
        doTestMessagesMetrics();
    }

    @Test
    @ExternalClients
    @Disabled("Only few chrome tests are enabled, rest functionality is covered by firefox")
    void testClientsMetrics() throws Exception {
        doTestClientsMetrics();
    }

    @Test()
    void testCannotOpenConsolePage() {
        assertThrows(IllegalAccessException.class, () -> doTestCanOpenConsolePage(new UserCredentials("noexistuser", "pepaPa555")));
    }

    @Test
    @Disabled("Only few chrome tests are enabled, rest functionality is covered by firefox")
    void testCanOpenConsolePage() throws Exception {
        doTestCanOpenConsolePage(clusterUser);
    }

    @Test
    @Disabled("Only few chrome tests are enabled, rest functionality is covered by firefox")
    void testCreateAddressWithSpecialCharsShowsErrorMessage() throws Exception {
        doTestCreateAddressWithSpecialCharsShowsErrorMessage();
    }

    @Test
    @Disabled("Only a few chrome tests are enabled, rest of functionality is covered by firefox")
    void testCreateAddressWithSymbolsAt61stCharIndex() throws Exception {
        doTestCreateAddressWithSymbolsAt61stCharIndex(
                new AddressBuilder()
                        .withNewMetadata()
                        .withNamespace(sharedAddressSpace.getMetadata().getNamespace())
                        .withName(AddressUtils.generateAddressMetadataName(sharedAddressSpace, "queue10charhere-10charhere-10charhere-10charhere-10charhere-1"))
                        .endMetadata()
                        .withNewSpec()
                        .withType("queue")
                        .withAddress("test-queue1")
                        .withPlan(getDefaultPlan(AddressType.QUEUE))
                        .endSpec()
                        .build(),
                new AddressBuilder()
                        .withNewMetadata()
                        .withNamespace(sharedAddressSpace.getMetadata().getNamespace())
                        .withName(AddressUtils.generateAddressMetadataName(sharedAddressSpace, "queue10charhere-10charhere-10charhere-10charhere-10charhere.1"))
                        .endMetadata()
                        .withNewSpec()
                        .withType("queue")
                        .withAddress("test-queue2")
                        .withPlan(getDefaultPlan(AddressType.QUEUE))
                        .endSpec()
                        .build());
    }

    @Test
    @Disabled("Only a few chrome tests are enabled, rest of functionality is covered by firefox")
    void testAddressWithValidPlanOnly() throws Exception {
        doTestAddressWithValidPlanOnly();
    }
}
