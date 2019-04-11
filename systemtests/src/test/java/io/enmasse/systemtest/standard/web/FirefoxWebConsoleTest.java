/*
 * Copyright 2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.systemtest.standard.web;

import io.enmasse.systemtest.AddressType;
import io.enmasse.systemtest.DestinationPlan;
import io.enmasse.systemtest.UserCredentials;
import io.enmasse.systemtest.ability.ITestBaseStandard;
import io.enmasse.systemtest.bases.web.WebConsoleTest;
import io.enmasse.systemtest.selenium.ISeleniumProviderFirefox;
import io.enmasse.systemtest.utils.AddressUtils;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertThrows;

public class FirefoxWebConsoleTest extends WebConsoleTest implements ITestBaseStandard, ISeleniumProviderFirefox {

    @Test
    void testCreateDeleteQueue() throws Exception {
        doTestCreateDeleteAddress(AddressUtils.createQueueAddressObject("test-queue1", DestinationPlan.STANDARD_SMALL_QUEUE),
                AddressUtils.createQueueAddressObject("test-queue2", DestinationPlan.STANDARD_LARGE_QUEUE));
    }

    @Test
    void testCreateDeleteDurableSubscription() throws Exception {
        doTestCreateDeleteDurableSubscription(AddressUtils.createTopicAddressObject("test-topic1", DestinationPlan.STANDARD_SMALL_TOPIC),
                AddressUtils.createTopicAddressObject("test-topic2", DestinationPlan.STANDARD_XLARGE_TOPIC));
    }

    @Test
    void testCreateDeleteTopic() throws Exception {
        doTestCreateDeleteAddress(AddressUtils.createTopicAddressObject("test-topic1", DestinationPlan.STANDARD_SMALL_TOPIC),
                AddressUtils.createTopicAddressObject("test-topic2", DestinationPlan.STANDARD_LARGE_TOPIC));
    }

    @Test
    void testCreateDeleteAnycast() throws Exception {
        doTestCreateDeleteAddress(AddressUtils.createAnycastAddressObject("test-anycast-firefox"));
    }

    @Test
    void testCreateDeleteMulticast() throws Exception {
        doTestCreateDeleteAddress(AddressUtils.createMulticastAddressObject("test-multicast-firefox"));
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

    @Test()
    void testCannotOpenConsolePage() {
        assertThrows(IllegalAccessException.class,
                () -> doTestCanOpenConsolePage(new UserCredentials("pepa", "pepaPa555")));
    }

    @Test
    void testCanOpenConsolePage() throws Exception {
        doTestCanOpenConsolePage(clusterUser);
    }

    @Test
    void testAddressStatus() throws Exception {
        doTestAddressStatus(AddressUtils.createQueueAddressObject("test-queue", getDefaultPlan(AddressType.QUEUE)));
        doTestAddressStatus(AddressUtils.createTopicAddressObject("test-topic", getDefaultPlan(AddressType.TOPIC)));
        doTestAddressStatus(AddressUtils.createAnycastAddressObject("test-anycast"));
        doTestAddressStatus(AddressUtils.createMulticastAddressObject("test-multicast"));
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
    @Disabled("disabled while sdavey changes it with changes to regex in addr names")
        //TODO(sdavey)
    void testCreateAddressWithSymbolsAt61stCharIndex() throws Exception {
        doTestCreateAddressWithSymbolsAt61stCharIndex(
                AddressUtils.createQueueAddressObject("queue10charHere-10charHere-10charHere-10charHere-10charHere-1",
                        getDefaultPlan(AddressType.QUEUE)),
                AddressUtils.createQueueAddressObject("queue10charHere-10charHere-10charHere-10charHere-10charHere.1",
                        getDefaultPlan(AddressType.QUEUE)));
    }

    @Test
    void testAddressWithValidPlanOnly() throws Exception {
        doTestAddressWithValidPlanOnly();
    }

    @Override
    public boolean skipDummyAddress() {
        return true;
    }
}
