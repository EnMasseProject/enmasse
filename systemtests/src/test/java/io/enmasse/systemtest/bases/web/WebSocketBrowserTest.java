/*
 * Copyright 2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.systemtest.bases.web;


import io.enmasse.address.model.Address;
import io.enmasse.systemtest.bases.TestBase;
import io.enmasse.systemtest.model.addressspace.AddressSpaceType;
import io.enmasse.systemtest.selenium.SeleniumProvider;
import io.enmasse.systemtest.selenium.page.RheaWebPage;
import io.enmasse.systemtest.utils.AddressSpaceUtils;
import org.junit.jupiter.api.BeforeEach;

import static org.junit.jupiter.api.Assertions.assertTrue;

public abstract class WebSocketBrowserTest extends TestBase {

    private RheaWebPage rheaWebPage;

    @BeforeEach
    public void setUpWebConsoleTests() throws Exception {
        rheaWebPage = new RheaWebPage(SeleniumProvider.getInstance());
        resourceManager.deleteAddresses(resourceManager.getDefaultAddressSpace());
    }

    //============================================================================================
    //============================ do test methods ===============================================
    //============================================================================================

    protected void doWebSocketSendReceive(Address destination) throws Exception {
        resourceManager.setAddresses(destination);
        int count = 10;

        rheaWebPage.sendReceiveMessages(AddressSpaceUtils.getMessagingWssRoute(resourceManager.getDefaultAddressSpace()).toString(), destination.getSpec().getAddress(),
                count, defaultCredentials, AddressSpaceType.valueOf(resourceManager.getDefaultAddressSpace().getSpec().getType().toUpperCase()));
        assertTrue(rheaWebPage.checkCountMessage(count * 2), "Browser client didn't sent and received all messages");
    }
}
