/*
 * Copyright 2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.systemtest.bases.web;


import io.enmasse.systemtest.*;
import io.enmasse.systemtest.bases.TestBaseWithShared;
import io.enmasse.systemtest.selenium.ISeleniumProvider;
import io.enmasse.systemtest.selenium.page.RheaWebPage;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.slf4j.Logger;

import static org.junit.jupiter.api.Assertions.assertTrue;

public abstract class WebSocketBrowserTest extends TestBaseWithShared implements ISeleniumProvider {

    private static Logger log = CustomLogger.getLogger();
    private RheaWebPage rheaWebPage;

    @BeforeEach
    public void setUpWebConsoleTests() throws Exception {
        if (selenium.getDriver() == null)
            selenium.setupDriver(environment, kubernetes, buildDriver());
        else
            selenium.clearScreenShots();
        rheaWebPage = new RheaWebPage(selenium);
        super.setAddresses(sharedAddressSpace);
    }

    @Override
    protected Endpoint getMessagingRoute(AddressSpace addressSpace) {
        if (addressSpace.getType().equals(AddressSpaceType.STANDARD)) {
            Endpoint messagingEndpoint = addressSpace.getEndpoint("amqp-wss");
            if (TestUtils.resolvable(messagingEndpoint)) {
                return messagingEndpoint;
            } else {
                return kubernetes.getEndpoint(addressSpace.getNamespace(), "messaging", "https");
            }
        } else {
            return super.getMessagingRoute(addressSpace);
        }
    }

    @Override
    public boolean skipDummyAddress() {
        return true;
    }

    //============================================================================================
    //============================ do test methods ===============================================
    //============================================================================================

    protected void doWebSocketSendReceive(Destination destination) throws Exception {
        setAddresses(destination);
        int count = 10;

        rheaWebPage.sendReceiveMessages(getMessagingRoute(sharedAddressSpace).toString(), destination.getAddress(),
                count, defaultCredentials, sharedAddressSpace.getType());
        assertTrue(rheaWebPage.checkCountMessage(count * 2), "Browser client didn't sent and received all messages");
    }
}
