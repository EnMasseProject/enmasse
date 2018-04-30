/*
 * Copyright 2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.systemtest.standard.web;

import io.enmasse.systemtest.AddressType;
import io.enmasse.systemtest.Destination;
import io.enmasse.systemtest.KeycloakCredentials;
import io.enmasse.systemtest.ability.ITestBaseStandard;
import io.enmasse.systemtest.bases.web.WebConsoleTest;
import io.enmasse.systemtest.selenium.ISeleniumProviderFirefox;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertThrows;

public class FirefoxWebConsoleTest extends WebConsoleTest implements ITestBaseStandard, ISeleniumProviderFirefox {

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
    @Disabled("related issue: #1074")
    void testCreateDeleteAnycast() throws Exception {
        doTestCreateDeleteAddress(Destination.anycast("test-anycast-firefox"));
    }

    @Test
    @Disabled("related issue: #1074")
    void testCreateDeleteMulticast() throws Exception {
        doTestCreateDeleteAddress(Destination.multicast("test-multicast-firefox"));
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
    void testFilterConnectionsByEncrypted() throws Exception {
        doTestFilterConnectionsByEncrypted();
    }

    @Test
    @Disabled("related issue: #667")
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
    void testFilterConnectionsByContainerId() throws Exception {
        doTestFilterConnectionsByContainerId();
    }

    @Test
    void testSortConnectionsByContainerId() throws Exception {
        doTestSortConnectionsByContainerId();
    }

    @Test
    void testMessagesMetrics() throws Exception {
        doTestMessagesMetrics();
    }

    @Test
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
    @Disabled("related issue: #818")
    void testViewConnections() throws Exception {
        doTestViewConnections();
    }

    @Test
    @Disabled("related issue: #819")
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
        doTestAddressStatus(Destination.anycast("test-anycast"));
        doTestAddressStatus(Destination.multicast("test-multicast"));
    }

    @Override
    public boolean skipDummyAddress() {
        return true;
    }
}
