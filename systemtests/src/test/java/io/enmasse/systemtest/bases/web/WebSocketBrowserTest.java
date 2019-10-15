/*
 * Copyright 2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.systemtest.bases.web;


import io.enmasse.address.model.Address;
import io.enmasse.address.model.AddressSpace;
import io.enmasse.systemtest.Endpoint;
import io.enmasse.systemtest.bases.TestBase;
import io.enmasse.systemtest.bases.shared.ITestBaseShared;
import io.enmasse.systemtest.model.addressspace.AddressSpaceType;
import io.enmasse.systemtest.selenium.SeleniumProvider;
import io.enmasse.systemtest.selenium.page.RheaWebPage;
import io.enmasse.systemtest.utils.AddressSpaceUtils;
import io.enmasse.systemtest.utils.TestUtils;
import org.junit.jupiter.api.BeforeEach;

import static org.junit.jupiter.api.Assertions.assertTrue;

public abstract class WebSocketBrowserTest extends TestBase implements ITestBaseShared {

    private RheaWebPage rheaWebPage;

    @BeforeEach
    public void setUpWebConsoleTests() throws Exception {
        rheaWebPage = new RheaWebPage(SeleniumProvider.getInstance());
        resourcesManager.deleteAddresses(getSharedAddressSpace());
    }

    //============================================================================================
    //============================ do test methods ===============================================
    //============================================================================================

    protected void doWebSocketSendReceive(Address destination) throws Exception {
        resourcesManager.setAddresses(destination);
        int count = 10;

        rheaWebPage.sendReceiveMessages(kubernetes.getMessagingRouteWS(getSharedAddressSpace()).toString(), destination.getSpec().getAddress(),
                count, defaultCredentials, AddressSpaceType.valueOf(getSharedAddressSpace().getSpec().getType().toUpperCase()));
        assertTrue(rheaWebPage.checkCountMessage(count * 2), "Browser client didn't sent and received all messages");
    }
}
