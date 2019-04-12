/*
 * Copyright 2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.systemtest.bases.web;


import io.enmasse.address.model.AddressSpace;
import io.enmasse.systemtest.CustomLogger;
import io.enmasse.systemtest.bases.TestBase;
import io.enmasse.systemtest.selenium.ISeleniumProvider;
import io.enmasse.systemtest.selenium.page.ConsoleWebPage;
import io.enmasse.systemtest.selenium.page.GlobalConsolePage;
import io.enmasse.systemtest.selenium.resources.AddressSpaceWebItem;
import io.enmasse.systemtest.utils.TestUtils;
import org.junit.jupiter.api.BeforeEach;
import org.slf4j.Logger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public abstract class GlobalConsoleTest extends TestBase implements ISeleniumProvider {

    private static Logger log = CustomLogger.getLogger();
    private GlobalConsolePage globalConsolePage;

    @BeforeEach
    public void setUpWebConsoleTests() throws Exception {
        if (selenium.getDriver() == null)
            selenium.setupDriver(environment, kubernetes, buildDriver());
        else
            selenium.clearScreenShots();
    }

    //============================================================================================
    //============================ do test methods ===============================================
    //============================================================================================

    protected void doTestOpen() throws Exception {
        globalConsolePage = new GlobalConsolePage(selenium, TestUtils.getGlobalConsoleRoute(), clusterUser);
        globalConsolePage.openGlobalConsolePage();
        globalConsolePage.logout();
    }

    protected void doTestCreateAddressSpace(AddressSpace addressSpace) throws Exception {
        globalConsolePage = new GlobalConsolePage(selenium, TestUtils.getGlobalConsoleRoute(), clusterUser);
        globalConsolePage.openGlobalConsolePage();
        globalConsolePage.createAddressSpace(addressSpace);
        assertEquals("Ready", ((AddressSpaceWebItem) selenium.waitUntilItemPresent(30, ()
                -> globalConsolePage.getAddressSpaceItem(addressSpace))).getStatus());
        globalConsolePage.deleteAddressSpace(addressSpace);
    }

    protected void doTestConnectToAddressSpaceConsole(AddressSpace addressSpace) throws Exception {
        globalConsolePage = new GlobalConsolePage(selenium, TestUtils.getGlobalConsoleRoute(), clusterUser);
        globalConsolePage.openGlobalConsolePage();
        globalConsolePage.createAddressSpace(addressSpace);
        ConsoleWebPage console = globalConsolePage.openAddressSpaceConsolePage(addressSpace);
        console.logout();
        assertTrue(((AddressSpaceWebItem) selenium.waitUntilItemPresent(30, ()
                -> globalConsolePage.getAddressSpaceItem(addressSpace))).getStatus().contains("Ready"));
    }
}
