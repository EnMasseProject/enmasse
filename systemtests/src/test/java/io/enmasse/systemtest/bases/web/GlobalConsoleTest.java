/*
 * Copyright 2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.systemtest.bases.web;


import io.enmasse.address.model.AddressSpace;
import io.enmasse.admin.model.v1.AuthenticationService;
import io.enmasse.systemtest.AddressSpacePlans;
import io.enmasse.systemtest.AddressSpaceType;
import io.enmasse.systemtest.AdminResourcesManager;
import io.enmasse.systemtest.bases.TestBase;
import io.enmasse.systemtest.selenium.ISeleniumProvider;
import io.enmasse.systemtest.selenium.page.ConsoleWebPage;
import io.enmasse.systemtest.selenium.page.GlobalConsolePage;
import io.enmasse.systemtest.selenium.resources.AddressSpaceWebItem;
import io.enmasse.systemtest.utils.AddressSpaceUtils;
import io.enmasse.systemtest.utils.AuthServiceUtils;
import io.enmasse.systemtest.utils.TestUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public abstract class GlobalConsoleTest extends TestBase implements ISeleniumProvider {

    private GlobalConsolePage globalConsolePage;
    private static final AdminResourcesManager adminManager = new AdminResourcesManager(kubernetes);

    @BeforeEach
    public void setUpWebConsoleTests() throws Exception {
        adminManager.setUp();
        if (selenium.getDriver() == null)
            selenium.setupDriver(environment, kubernetes, buildDriver());
        else
            selenium.clearScreenShots();
    }

    @AfterEach
    public void tearDown() throws Exception {
        adminManager.tearDown();
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
        addToAddressSpacess(addressSpace);
        globalConsolePage = new GlobalConsolePage(selenium, TestUtils.getGlobalConsoleRoute(), clusterUser);
        globalConsolePage.openGlobalConsolePage();
        globalConsolePage.createAddressSpace(addressSpace);
        assertEquals("Active", ((AddressSpaceWebItem) selenium.waitUntilItemPresent(30, ()
                -> globalConsolePage.getAddressSpaceItem(addressSpace))).getStatus());
        globalConsolePage.deleteAddressSpace(addressSpace);
    }

    protected void doTestConnectToAddressSpaceConsole(AddressSpace addressSpace) throws Exception {
        addToAddressSpacess(addressSpace);
        globalConsolePage = new GlobalConsolePage(selenium, TestUtils.getGlobalConsoleRoute(), clusterUser);
        globalConsolePage.openGlobalConsolePage();
        globalConsolePage.createAddressSpace(addressSpace);
        ConsoleWebPage console = globalConsolePage.openAddressSpaceConsolePage(addressSpace);
        console.logout();
        assertTrue(((AddressSpaceWebItem) selenium.waitUntilItemPresent(30, ()
                -> globalConsolePage.getAddressSpaceItem(addressSpace))).getStatus().contains("Active"));
    }

    protected void doTestCreateAddrSpaceWithCustomAuthService() throws Exception {
        AuthenticationService standardAuth = AuthServiceUtils.createStandardAuthServiceObject("test-standard-authservice", true);
        adminManager.createAuthService(standardAuth);

        AddressSpace addressSpace = AddressSpaceUtils.createAddressSpaceObject("test-addr-space",
                kubernetes.getNamespace(),
                standardAuth.getMetadata().getName(),
                AddressSpaceType.BROKERED,
                AddressSpacePlans.BROKERED);
        addToAddressSpacess(addressSpace);

        globalConsolePage = new GlobalConsolePage(selenium, TestUtils.getGlobalConsoleRoute(), clusterUser);
        globalConsolePage.openGlobalConsolePage();
        globalConsolePage.createAddressSpace(addressSpace);
        assertEquals("Active", ((AddressSpaceWebItem) selenium.waitUntilItemPresent(30, ()
                -> globalConsolePage.getAddressSpaceItem(addressSpace))).getStatus());
    }

    protected void doTestViewAddressSpace() throws Exception {
        AddressSpace addressSpace = AddressSpaceUtils.createAddressSpaceObject("test-addr-space-api",
                kubernetes.getNamespace(),
                AddressSpaceType.BROKERED,
                AddressSpacePlans.BROKERED);

        createAddressSpace(addressSpace);

        globalConsolePage = new GlobalConsolePage(selenium, TestUtils.getGlobalConsoleRoute(), clusterUser);
        globalConsolePage.openGlobalConsolePage();
        assertEquals("Active", ((AddressSpaceWebItem) selenium.waitUntilItemPresent(30, ()
                -> globalConsolePage.getAddressSpaceItem(addressSpace))).getStatus());
        globalConsolePage.deleteAddressSpace(addressSpace);
    }
}
