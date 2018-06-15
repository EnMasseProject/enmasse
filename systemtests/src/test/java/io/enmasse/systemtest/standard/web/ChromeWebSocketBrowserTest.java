/*
 * Copyright 2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.systemtest.standard.web;

import io.enmasse.systemtest.*;
import io.enmasse.systemtest.ability.ITestBaseStandard;
import io.enmasse.systemtest.bases.web.WebSocketBrowserTest;
import io.enmasse.systemtest.selenium.ISeleniumProviderChrome;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static io.enmasse.systemtest.TestTag.nonPR;

@Disabled("Issue #403")
@Tag(nonPR)
class ChromeWebSocketBrowserTest extends WebSocketBrowserTest implements ITestBaseStandard, ISeleniumProviderChrome {

    @Test
    void testWebSocketSendReceiveQueue() throws Exception {
        doWebSocketSendReceive(Destination.queue("websocket-queue", getDefaultPlan(AddressType.QUEUE)));
    }

    @Test
    void testWebSocketSendReceiveTopic() throws Exception {
        doWebSocketSendReceive(Destination.topic("websocket-topic", getDefaultPlan(AddressType.TOPIC)));
    }
}
