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
import io.enmasse.systemtest.selenium.ISeleniumProviderChrome;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertThrows;

@Disabled("chromedriver does not wotk properly")
public class ChromeWebConsoleTest extends WebConsoleTest implements ITestBaseStandard, ISeleniumProviderChrome {

    @Test
    void testCreateDeleteQueue() throws Exception {
        doTestCreateDeleteAddress(Destination.queue("test-queue", getDefaultPlan(AddressType.QUEUE)));
    }

    @Test
    void testCreateDeleteTopic() throws Exception {
        doTestCreateDeleteAddress(Destination.topic("test-topic", getDefaultPlan(AddressType.TOPIC)));
    }

    @Test
    void testCreateDeleteAnycast() throws Exception {
        doTestCreateDeleteAddress(Destination.anycast("test-anycast-firefox"));
    }

    @Test
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
    void testViewConnections() throws Exception {
        doTestViewConnections();
    }

    @Test
    void testViewAddressesWildcards() throws Exception {
        doTestViewAddressesWildcards();
    }

    @Test()
    void testCannotOpenConsolePage() {
        assertThrows(IllegalAccessException.class, () -> doTestCanOpenConsolePage(new KeycloakCredentials("pepa", "pepaPa555")));
    }

    @Test
    void testCanOpenConsolePage() throws Exception {
        doTestCanOpenConsolePage(defaultCredentials);
    }

    @Override
    public boolean skipDummyAddress() {
        return true;
    }
}
