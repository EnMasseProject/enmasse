/*
 * Copyright 2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.systemtest.bases.web;


import io.enmasse.address.model.AddressSpace;
import io.enmasse.address.model.AddressSpaceBuilder;
import io.enmasse.address.model.ExposeType;
import io.enmasse.address.model.TlsTermination;
import io.enmasse.admin.model.v1.AuthenticationService;
import io.enmasse.systemtest.UserCredentials;
import io.enmasse.systemtest.bases.TestBase;
import io.enmasse.systemtest.platform.KubeCMDClient;
import io.enmasse.systemtest.isolated.Credentials;
import io.enmasse.systemtest.logs.CustomLogger;
import io.enmasse.systemtest.manager.IsolatedResourcesManager;
import io.enmasse.systemtest.model.addressspace.AddressSpacePlans;
import io.enmasse.systemtest.model.addressspace.AddressSpaceType;
import io.enmasse.systemtest.selenium.SeleniumProvider;
import io.enmasse.systemtest.selenium.page.ConsoleWebPage;
import io.enmasse.systemtest.selenium.page.GlobalConsolePage;
import io.enmasse.systemtest.utils.AddressSpaceUtils;
import io.enmasse.systemtest.utils.AuthServiceUtils;
import io.enmasse.systemtest.utils.TestUtils;
import org.slf4j.Logger;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public abstract class GlobalConsoleTest extends TestBase {
    private static final IsolatedResourcesManager ISOLATED_RESOURCES_MANAGER = IsolatedResourcesManager.getInstance();
    private static Logger log = CustomLogger.getLogger();
    SeleniumProvider selenium = SeleniumProvider.getInstance();
    private GlobalConsolePage globalConsolePage;

    //============================================================================================
    //============================ do test methods ===============================================
    //============================================================================================

    protected void doTestOpen() throws Exception {
        globalConsolePage = new GlobalConsolePage(selenium, TestUtils.getGlobalConsoleRoute(), clusterUser);
        globalConsolePage.openGlobalConsolePage();
        globalConsolePage.logout();
    }

    protected void doTestCreateAddressSpace(AddressSpace addressSpace) throws Exception {
        ISOLATED_RESOURCES_MANAGER.addToAddressSpaces(addressSpace);
        globalConsolePage = new GlobalConsolePage(selenium, TestUtils.getGlobalConsoleRoute(), clusterUser);
        globalConsolePage.openGlobalConsolePage();
        globalConsolePage.createAddressSpace(addressSpace);
        waitUntilAddressSpaceActive(addressSpace);
        globalConsolePage.deleteAddressSpace(addressSpace);
    }

    protected void doTestConnectToAddressSpaceConsole(AddressSpace addressSpace) throws Exception {
        ISOLATED_RESOURCES_MANAGER.addToAddressSpaces(addressSpace);
        globalConsolePage = new GlobalConsolePage(selenium, TestUtils.getGlobalConsoleRoute(), clusterUser);
        globalConsolePage.openGlobalConsolePage();
        globalConsolePage.createAddressSpace(addressSpace);
        ConsoleWebPage console = globalConsolePage.openAddressSpaceConsolePage(addressSpace);
        console.logout();
        waitUntilAddressSpaceActive(addressSpace);
    }

    protected void doTestCreateAddrSpaceWithCustomAuthService() throws Exception {
        AuthenticationService standardAuth = AuthServiceUtils.createStandardAuthServiceObject("test-standard-authservice", true);
        ISOLATED_RESOURCES_MANAGER.replaceAuthService(standardAuth);

        AddressSpace addressSpace = new AddressSpaceBuilder()
                .withNewMetadata()
                .withName("test-addr-space-custom-auth")
                .withNamespace(kubernetes.getInfraNamespace())
                .endMetadata()
                .withNewSpec()
                .withType(AddressSpaceType.BROKERED.toString())
                .withPlan(AddressSpacePlans.BROKERED)
                .withNewAuthenticationService()
                .withName("standard-authservice")
                .endAuthenticationService()
                .endSpec()
                .build();
        ISOLATED_RESOURCES_MANAGER.addToAddressSpaces(addressSpace);

        globalConsolePage = new GlobalConsolePage(selenium, TestUtils.getGlobalConsoleRoute(), clusterUser);
        globalConsolePage.openGlobalConsolePage();
        globalConsolePage.createAddressSpace(addressSpace);
        waitUntilAddressSpaceActive(addressSpace);
    }

    protected void doTestViewAddressSpace() throws Exception {
        AddressSpace addressSpace = new AddressSpaceBuilder()
                .withNewMetadata()
                .withName("test-addr-space-view-console")
                .withNamespace(kubernetes.getInfraNamespace())
                .endMetadata()
                .withNewSpec()
                .withType(AddressSpaceType.BROKERED.toString())
                .withPlan(AddressSpacePlans.BROKERED)
                .withNewAuthenticationService()
                .withName("standard-authservice")
                .endAuthenticationService()
                .endSpec()
                .build();

        ISOLATED_RESOURCES_MANAGER.createAddressSpace(addressSpace);

        globalConsolePage = new GlobalConsolePage(selenium, TestUtils.getGlobalConsoleRoute(), clusterUser);
        globalConsolePage.openGlobalConsolePage();
        waitUntilAddressSpaceActive(addressSpace);
        globalConsolePage.deleteAddressSpace(addressSpace);
    }

    protected void doTestCreateAddrSpaceNonClusterAdmin() throws Exception {
        String namespace = "test-namespace";
        UserCredentials user = Credentials.userCredentials();
        try {
            KubeCMDClient.loginUser(user.getUsername(), user.getPassword());
            KubeCMDClient.createNamespace(namespace);

            AddressSpace addressSpace = new AddressSpaceBuilder()
                    .withNewMetadata()
                    .withName("test-addr-space-api")
                    .withNamespace(namespace)
                    .endMetadata()
                    .withNewSpec()
                    .withType(AddressSpaceType.BROKERED.toString())
                    .withPlan(AddressSpacePlans.BROKERED)
                    .withNewAuthenticationService()
                    .withName("standard-authservice")
                    .endAuthenticationService()
                    .endSpec()
                    .build();

            globalConsolePage = new GlobalConsolePage(selenium, TestUtils.getGlobalConsoleRoute(), user);
            globalConsolePage.openGlobalConsolePage();
            globalConsolePage.createAddressSpace(addressSpace);
            waitUntilAddressSpaceActive(addressSpace);
            globalConsolePage.deleteAddressSpace(addressSpace);

        } finally {
            KubeCMDClient.loginUser(environment.getApiToken());
            KubeCMDClient.switchProject(environment.namespace());
            kubernetes.deleteNamespace(namespace);
        }
    }

    protected void doTestSwitchAddressSpacePlan() throws Exception {
        AddressSpace addressSpace = new AddressSpaceBuilder()
                .withNewMetadata()
                .withName("test-addr-space-api")
                .withNamespace(kubernetes.getInfraNamespace())
                .endMetadata()
                .withNewSpec()
                .withType(AddressSpaceType.STANDARD.toString())
                .withPlan(AddressSpacePlans.STANDARD_MEDIUM)
                .withNewAuthenticationService()
                .withName("standard-authservice")
                .endAuthenticationService()
                .endSpec()
                .build();
        ISOLATED_RESOURCES_MANAGER.addToAddressSpaces(addressSpace);
        globalConsolePage = new GlobalConsolePage(selenium, TestUtils.getGlobalConsoleRoute(), clusterUser);
        globalConsolePage.openGlobalConsolePage();
        globalConsolePage.createAddressSpace(addressSpace);
        waitUntilAddressSpaceActive(addressSpace);
        assertEquals(AddressSpacePlans.STANDARD_MEDIUM,
                resourcesManager.getAddressSpace(addressSpace.getMetadata().getName()).getSpec().getPlan());
        globalConsolePage.switchAddressSpacePlan(addressSpace, AddressSpacePlans.STANDARD_UNLIMITED);
        AddressSpaceUtils.waitForAddressSpacePlanApplied(addressSpace);
        AddressSpaceUtils.waitForAddressSpaceReady(addressSpace);
        assertEquals(AddressSpacePlans.STANDARD_UNLIMITED,
                resourcesManager.getAddressSpace(addressSpace.getMetadata().getName()).getSpec().getPlan());
    }

    private void waitUntilAddressSpaceActive(AddressSpace addressSpace) throws Exception {
        String name = addressSpace.getMetadata().getName();
        ISOLATED_RESOURCES_MANAGER.waitForAddressSpaceReady(addressSpace);
        Boolean active = Optional.ofNullable(selenium.waitUntilItemPresent(60, () -> globalConsolePage.getAddressSpaceItem(addressSpace)))
                .map(webItem -> webItem.getStatus().contains("Active"))
                .orElseGet(() -> {
                    log.error("AddressSpaceWebItem {} not present", name);
                    return false;
                });
        assertTrue(active, String.format("Address space %s not marked active in UI within timeout", name));
    }

    protected void doTestOpenConsoleCustomRoute() throws Exception {
        String endpointPrefix = "test-endpoint-";

        AddressSpace addressSpace = new AddressSpaceBuilder()
                .withNewMetadata()
                .withName("standard")
                .withNamespace(kubernetes.getInfraNamespace())
                .endMetadata()
                .withNewSpec()
                .withType(AddressSpaceType.STANDARD.toString())
                .withPlan(AddressSpacePlans.STANDARD_SMALL)
                .withNewAuthenticationService()
                .withName("standard-authservice")
                .endAuthenticationService()

                .addNewEndpoint()
                .withName(endpointPrefix + "console")
                .withService("console")
                .editOrNewExpose()
                .withType(ExposeType.route)
                .withRouteTlsTermination(TlsTermination.reencrypt)
                .withRouteServicePort("https")
                .endExpose()
                .endEndpoint()

                .endSpec()
                .build();
        ISOLATED_RESOURCES_MANAGER.createAddressSpace(addressSpace);
        log.warn("Addressspace::     " + addressSpace);
        log.warn("Finding: " + endpointPrefix + "console-" + AddressSpaceUtils.getAddressSpaceInfraUuid(addressSpace));

        //try to get all external endpoints
        kubernetes.getExternalEndpoint(endpointPrefix + "console-" + AddressSpaceUtils.getAddressSpaceInfraUuid(addressSpace));

        ConsoleWebPage console = new ConsoleWebPage(
                selenium,
                kubernetes.getConsoleRoute(addressSpace),
                addressSpace,
                clusterUser);
        console.openWebConsolePage();
        console.openAddressesPageWebConsole();
    }
}
