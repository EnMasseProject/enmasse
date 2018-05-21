/*
 * Copyright 2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.systemtest.brokered.web;

import io.enmasse.systemtest.AddressType;
import io.enmasse.systemtest.Destination;
import io.enmasse.systemtest.ability.ITestBaseBrokered;
import io.enmasse.systemtest.bases.web.WebSocketBrowserTest;
import io.enmasse.systemtest.selenium.ISeleniumProviderChrome;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

class ChromeWebSocketBrowserTest extends WebSocketBrowserTest implements ITestBaseBrokered, ISeleniumProviderChrome {


    @Test
    void testWebSocketSendReceiveQueue() throws Exception {
        doWebSocketSendReceive(Destination.queue("websocket-queue", getDefaultPlan(AddressType.QUEUE)));
    }

    @Test
    void testWebSocketSendReceiveTopic() throws Exception {
        doWebSocketSendReceive(Destination.topic("websocket-topic", getDefaultPlan(AddressType.TOPIC)));
    }
}
