/*
 * Copyright 2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.systemtest.bases.web;


import io.enmasse.address.model.Address;
import io.enmasse.address.model.AddressSpace;
import io.enmasse.systemtest.AddressSpaceType;
import io.enmasse.systemtest.Endpoint;
import io.enmasse.systemtest.bases.TestBaseWithShared;
import io.enmasse.systemtest.selenium.ISeleniumProvider;
import io.enmasse.systemtest.selenium.page.RheaWebPage;
import io.enmasse.systemtest.utils.AddressSpaceUtils;
import io.enmasse.systemtest.utils.TestUtils;
import org.junit.jupiter.api.BeforeEach;
import static org.junit.jupiter.api.Assertions.assertTrue;

public abstract class WebSocketBrowserTest extends TestBaseWithShared implements ISeleniumProvider {

    private RheaWebPage rheaWebPage;

    @BeforeEach
    public void setUpWebConsoleTests() throws Exception {
        if (selenium.getDriver() == null)
            selenium.setupDriver(buildDriver());
        else
            selenium.clearScreenShots();
        rheaWebPage = new RheaWebPage(selenium);
        deleteAddresses(sharedAddressSpace);
    }

    @Override
    protected Endpoint getMessagingRoute(AddressSpace addressSpace) throws Exception {
        if (addressSpace.getSpec().getType().equals(AddressSpaceType.STANDARD.toString())) {
            Endpoint messagingEndpoint = AddressSpaceUtils.getEndpointByName(addressSpace, "messaging-wss");
            if (TestUtils.resolvable(messagingEndpoint)) {
                return messagingEndpoint;
            } else {
                return kubernetes.getEndpoint("messaging-" + AddressSpaceUtils.getAddressSpaceInfraUuid(addressSpace), "https");
            }
        } else {
            return super.getMessagingRoute(addressSpace);
        }
    }

    //============================================================================================
    //============================ do test methods ===============================================
    //============================================================================================

    protected void doWebSocketSendReceive(Address destination) throws Exception {
        setAddresses(destination);
        int count = 10;

        rheaWebPage.sendReceiveMessages(getMessagingRoute(sharedAddressSpace).toString(), destination.getSpec().getAddress(),
                count, defaultCredentials, AddressSpaceType.valueOf(sharedAddressSpace.getSpec().getType().toUpperCase()));
        assertTrue(rheaWebPage.checkCountMessage(count * 2), "Browser client didn't sent and received all messages");
    }
}
