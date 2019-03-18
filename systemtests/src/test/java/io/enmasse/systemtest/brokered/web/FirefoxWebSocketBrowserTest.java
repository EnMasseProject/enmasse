/*
 * Copyright 2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.systemtest.brokered.web;

import io.enmasse.systemtest.AddressType;
import io.enmasse.systemtest.ability.ITestBaseBrokered;
import io.enmasse.systemtest.bases.web.WebSocketBrowserTest;
import io.enmasse.systemtest.selenium.ISeleniumProviderFirefox;
import io.enmasse.systemtest.utils.AddressUtils;
import org.junit.jupiter.api.Test;

class FirefoxWebSocketBrowserTest extends WebSocketBrowserTest implements ITestBaseBrokered, ISeleniumProviderFirefox {


    @Test
    void testWebSocketSendReceiveQueue() throws Exception {
        doWebSocketSendReceive(AddressUtils.createQueueAddressObject("websocket-queue", getDefaultPlan(AddressType.QUEUE)));
    }

    @Test
    void testWebSocketSendReceiveTopic() throws Exception {
        doWebSocketSendReceive(AddressUtils.createTopicAddressObject("websocket-topic", getDefaultPlan(AddressType.TOPIC)));
    }
}
