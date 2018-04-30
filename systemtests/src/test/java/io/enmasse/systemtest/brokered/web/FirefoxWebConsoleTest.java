/*
 * Copyright 2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.systemtest.brokered.web;

import io.enmasse.systemtest.AddressType;
import io.enmasse.systemtest.Destination;
import io.enmasse.systemtest.KeycloakCredentials;
import io.enmasse.systemtest.ability.ITestBaseBrokered;
import io.enmasse.systemtest.bases.web.WebConsoleTest;
import io.enmasse.systemtest.selenium.ISeleniumProviderFirefox;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertThrows;

class FirefoxWebConsoleTest extends WebConsoleTest implements ITestBaseBrokered, ISeleniumProviderFirefox {

    @Test
    @Disabled("related issue: #1074")
    void testCreateDeleteQueue() throws Exception {
        doTestCreateDeleteAddress(Destination.queue("test-queue", getDefaultPlan(AddressType.QUEUE)));
    }

    @Test
    @Disabled("related issue: #1074")
    void testCreateDeleteTopic() throws Exception {
        doTestCreateDeleteAddress(Destination.topic("test-topic", getDefaultPlan(AddressType.TOPIC)));
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
    void testSortAddressesByName() throws Exception {
        doTestSortAddressesByName();
    }

    @Test
    void testSortAddressesByClients() throws Exception {
        doTestSortAddressesByClients();
    }

    @Test
    void testSortConnectionsBySenders() throws Exception {
        doTestSortConnectionsBySenders();
    }

    @Test
    void testSortConnectionsByReceivers() throws Exception {
        doTestSortConnectionsByReceivers();
    }

    @Test
    @Disabled("disabled due to #669")
    void testFilterConnectionsByEncrypted() throws Exception {
        doTestFilterConnectionsByEncrypted();
    }

    @Test
    void testFilterConnectionsByUser() throws Exception {
        doTestFilterConnectionsByUser();
    }

    @Test
    void testFilterConnectionsByHostname() throws Exception {
        doTestFilterConnectionsByHostname();
    }

    @Test
    void testSortConnectionsByHostname() throws Exception {
        doTestSortConnectionsByHostname();
    }

    @Test
    @Disabled("disabled due to https://github.com/EnMasseProject/enmasse/issues/634")
    void testFilterConnectionsByContainerId() throws Exception {
        doTestFilterConnectionsByContainerId();
    }

    @Test
    @Disabled("disabled due to https://github.com/EnMasseProject/enmasse/issues/634")
    void testSortConnectionsByContainerId() throws Exception {
        doTestSortConnectionsByContainerId();
    }

    @Test
    void testMessagesMetrics() throws Exception {
        doTestMessagesMetrics();
    }

    @Test
    @Disabled("disabled due to #649")
    void testClientsMetrics() throws Exception {
        doTestClientsMetrics();
    }

    @Test
    void testCannotCreateAddresses() throws Exception {
        doTestCannotCreateAddresses();
    }

    @Test
    void testCannotDeleteAddresses() throws Exception {
        doTestCannotDeleteAddresses();
    }

    @Test
    void testViewAddresses() throws Exception {
        doTestViewAddresses();
    }

    @Test
    void testViewConnections() throws Exception {
        doTestViewConnections();
    }

    @Test
    @Disabled("not implemented yet")
    void testViewAddressesWildcards() throws Exception {
        doTestViewAddressesWildcards();
    }

    @Test()
    void testCannotOpenConsolePage() {
        assertThrows(IllegalAccessException.class,
                () -> doTestCanOpenConsolePage(new KeycloakCredentials("pepa", "pepaPa555")));
    }

    @Test
    void testCanOpenConsolePage() throws Exception {
        doTestCanOpenConsolePage(defaultCredentials);
    }

    @Test
    void testAddressStatus() throws Exception {
        doTestAddressStatus(Destination.queue("test-queue", getDefaultPlan(AddressType.QUEUE)));
        doTestAddressStatus(Destination.topic("test-topic", getDefaultPlan(AddressType.TOPIC)));
    }
}
