/*
 * Copyright 2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.systemtest.brokered.web;

import io.enmasse.address.model.AddressBuilder;
import io.enmasse.systemtest.AddressType;
import io.enmasse.systemtest.UserCredentials;
import io.enmasse.systemtest.ability.ITestBaseBrokered;
import io.enmasse.systemtest.bases.web.WebConsoleTest;
import io.enmasse.systemtest.messagingclients.ExternalClients;
import io.enmasse.systemtest.selenium.ISeleniumProviderFirefox;
import io.enmasse.systemtest.utils.AddressUtils;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertThrows;

class FirefoxWebConsoleTest extends WebConsoleTest implements ITestBaseBrokered, ISeleniumProviderFirefox {

    @Test
    void testCreateDeleteQueue() throws Exception {
        doTestCreateDeleteAddress(new AddressBuilder()
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
    }

    @Test
    void testCreateDeleteTopic() throws Exception {
        doTestCreateDeleteAddress(new AddressBuilder()
                .withNewMetadata()
                .withNamespace(sharedAddressSpace.getMetadata().getNamespace())
                .withName(AddressUtils.generateAddressMetadataName(sharedAddressSpace, "test-topic"))
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
    @Disabled("disabled due to #669")
    void testFilterConnectionsByEncrypted() throws Exception {
        doTestFilterConnectionsByEncrypted();
    }

    @Test
    @ExternalClients
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
    @Disabled("disabled due to https://github.com/EnMasseProject/enmasse/issues/634")
    void testFilterConnectionsByContainerId() throws Exception {
        doTestFilterConnectionsByContainerId();
    }

    @Test
    @ExternalClients
    @Disabled("disabled due to https://github.com/EnMasseProject/enmasse/issues/634")
    void testSortConnectionsByContainerId() throws Exception {
        doTestSortConnectionsByContainerId();
    }

    @Test
    void testMessagesMetrics() throws Exception {
        doTestMessagesMetrics();
    }

    @Test
    @ExternalClients
    @Disabled("disabled due to #649")
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
    }

    @Test
    void testCreateAddressWithSpecialCharsShowsErrorMessage() throws Exception {
        doTestCreateAddressWithSpecialCharsShowsErrorMessage();
    }

    @Test
    @Disabled("disabled while sdavey changes it with changes to regex in addr names")
        //TODO(sdavey)
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
    void testAddressWithValidPlanOnly() throws Exception {
        doTestAddressWithValidPlanOnly();
    }
}
