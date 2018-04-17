/*
 * Copyright 2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.systemtest.standard.web;

import io.enmasse.systemtest.AddressType;
import io.enmasse.systemtest.Destination;
import io.enmasse.systemtest.ability.ITestBaseStandard;
import io.enmasse.systemtest.bases.web.WebSocketBrowserTest;
import io.enmasse.systemtest.selenium.ISeleniumProviderFirefox;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.WebDriver;

@Disabled("disabled until ws in standard address space will be implemented")
public class FirefoxWebSocketBrowserTest extends WebSocketBrowserTest implements ITestBaseStandard, ISeleniumProviderFirefox {


    @Test
    public void testWebSocketSendReceiveQueue() throws Exception {
        doWebSocketSendReceive(Destination.queue("websocket-queue", getDefaultPlan(AddressType.QUEUE)));
    }

    @Test
    public void testWebSocketSendReceiveTopic() throws Exception {
        doWebSocketSendReceive(Destination.topic("websocket-topic", getDefaultPlan(AddressType.TOPIC)));
    }
}
