/*
 * Copyright 2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.systemtest.bases.web;


import io.enmasse.systemtest.CustomLogger;
import io.enmasse.systemtest.Destination;
import io.enmasse.systemtest.bases.TestBaseWithShared;
import io.enmasse.systemtest.selenium.ISeleniumProvider;
import io.enmasse.systemtest.selenium.RheaWebPage;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.slf4j.Logger;

import static org.junit.jupiter.api.Assertions.assertEquals;

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

    @AfterAll
    public static void TearDownDrivers() {
        selenium.tearDownDrivers();
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

        rheaWebPage.sendReceiveMessages(getMessagingRoute(sharedAddressSpace).toString(), destination.getAddress(),
                10, defaultCredentials);
        Thread.sleep(5000);
        log.info("Check if client sent and received right count of messages");
        assertEquals(20, rheaWebPage.readMessageCount(), "Browser client didn't sent and received all messages");
    }
}
