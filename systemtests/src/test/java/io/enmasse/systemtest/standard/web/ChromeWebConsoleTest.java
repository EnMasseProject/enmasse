/*
 * Copyright 2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.systemtest.standard.web;

import io.enmasse.systemtest.AddressType;
import io.enmasse.systemtest.Destination;
import org.openqa.selenium.WebDriver;

import static org.junit.jupiter.api.Assertions.assertThrows;

public class ChromeWebConsoleTest extends StandardWebConsoleTest {

    //@Test
    public void testCreateDeleteQueue() throws Exception {
        doTestCreateDeleteAddress(Destination.queue("test-queue", getDefaultPlan(AddressType.QUEUE)));
    }

    //@Test
    public void testCreateDeleteTopic() throws Exception {
        doTestCreateDeleteAddress(Destination.topic("test-topic", getDefaultPlan(AddressType.TOPIC)));
    }

    //@Test
    public void testCreateDeleteAnycast() throws Exception {
        doTestCreateDeleteAddress(Destination.anycast("test-anycast-firefox"));
    }

    //@Test
    public void testCreateDeleteMulticast() throws Exception {
        doTestCreateDeleteAddress(Destination.multicast("test-multicast-firefox"));
    }

    //@Test
    public void testFilterAddressesByType() throws Exception {
        doTestFilterAddressesByType();
    }

    //@Test
    public void testFilterAddressesByName() throws Exception {
        doTestFilterAddressesByName();
    }

    //@Test
    public void testSortAddressesByName() throws Exception {
        doTestSortAddressesByName();
    }

    //@Test
    public void testSortConnectionsBySenders() throws Exception {
        doTestSortConnectionsBySenders();
    }

    //@Test
    public void testSortConnectionsByReceivers() throws Exception {
        doTestSortConnectionsByReceivers();
    }

    //@Test
    public void testFilterConnectionsByEncrypted() throws Exception {
        doTestFilterConnectionsByEncrypted();
    }

    //@Test
    public void testFilterConnectionsByUser() throws Exception {
        doTestFilterConnectionsByUser();
    }

    //@Test
    public void testFilterConnectionsByHostname() throws Exception {
        doTestFilterConnectionsByHostname();
    }

    //@Test
    public void testSortConnectionsByHostname() throws Exception {
        doTestSortConnectionsByHostname();
    }

    //@Test
    public void testFilterConnectionsByContainerId() throws Exception {
        doTestFilterConnectionsByContainerId();
    }

    //@Test
    public void testSortConnectionsByContainerId() throws Exception {
        doTestSortConnectionsByContainerId();
    }

    //@Test
    public void testMessagesMetrics() throws Exception {
        doTestMessagesMetrics();
    }

    //@Test
    public void testClientsMetrics() throws Exception {
        doTestClientsMetrics();
    }

    //@Test
    public void testCannotCreateAddresses() throws Exception {
        doTestCannotCreateAddresses();
    }

    //@Test
    public void testCannotDeleteAddresses() throws Exception {
        doTestCannotDeleteAddresses();
    }

    //@Test
    public void testViewAddresses() throws Exception {
        doTestViewAddresses();
    }

    //@Test
    public void testViewConnections() throws Exception {
        doTestViewConnections();
    }

    //@Test
    public void testViewAddressesWildcards() throws Exception {
        doTestViewAddressesWildcards();
    }

    //@Test()
    public void testCannotOpenConsolePage() throws Exception {
        assertThrows(IllegalAccessException.class, () -> doTestCanOpenConsolePage("pepa", "pepaPa555"));
    }

    //@Test
    public void testCanOpenConsolePage() throws Exception {
        doTestCanOpenConsolePage(username, password);
    }

    @Override
    public WebDriver buildDriver() {
        return getChromeDriver();
    }
}
