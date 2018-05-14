/*
 * Copyright 2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.systemtest.common.catalog;

import io.enmasse.systemtest.AddressSpace;
import io.enmasse.systemtest.AddressSpaceType;
import io.enmasse.systemtest.CustomLogger;
import io.enmasse.systemtest.KeycloakCredentials;
import io.enmasse.systemtest.bases.TestBase;
import io.enmasse.systemtest.selenium.ISeleniumProviderFirefox;
import io.enmasse.systemtest.selenium.page.ConsoleWebPage;
import io.enmasse.systemtest.selenium.page.OpenshiftWebPage;
import io.enmasse.systemtest.selenium.resources.BindingSecretData;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.DisabledIfEnvironmentVariable;
import org.slf4j.Logger;

import java.util.HashMap;
import java.util.Map;

import static io.enmasse.systemtest.Environment.useMinikubeEnv;
import static io.enmasse.systemtest.TestTag.isolated;
import static org.junit.jupiter.api.Assertions.fail;

@Tag(isolated)
class ServiceCatalogWebTest extends TestBase implements ISeleniumProviderFirefox {

    private static Logger log = CustomLogger.getLogger();
    private Map<String, AddressSpace> provisionedServices = new HashMap<>();
    private KeycloakCredentials developer = new KeycloakCredentials("developer", "developer");

    private String getUserProjectName(AddressSpace addressSpace) {
        return String.format("%s-%s", "service", addressSpace.getNamespace());
    }

    @BeforeEach
    void setUpDrivers() throws Exception {
        if (selenium.getDriver() == null) {
            selenium.setupDriver(environment, kubernetes, buildDriver());
        } else {
            selenium.clearScreenShots();
        }
    }

    @AfterEach
    void tearDownWebConsoleTests() {
        if (!environment.skipCleanup()) {
            provisionedServices.forEach((project, addressSpace) -> {
                try {
                    deleteAddressSpaceCreatedBySC(project, addressSpace);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });
            provisionedServices.clear();
        } else {
            log.warn("Remove address spaces in tear down - SKIPPED!");
        }
    }

    @Test
    @DisabledIfEnvironmentVariable(named = useMinikubeEnv, matches = "true")
    void testProvisionAddressSpaceBrokered() throws Exception {
        AddressSpace brokered = new AddressSpace("addr-space-brokered", AddressSpaceType.BROKERED);
        provisionedServices.put(getUserProjectName(brokered), brokered);
        OpenshiftWebPage ocPage = new OpenshiftWebPage(selenium, addressApiClient, getOCConsoleRoute(),
                new KeycloakCredentials("developer", "developer"));
        ocPage.openOpenshiftPage();
        ocPage.provisionAddressSpaceViaSC(brokered, getUserProjectName(brokered));
        ocPage.deprovisionAddressSpace(getUserProjectName(brokered));
    }

    @Test
    @DisabledIfEnvironmentVariable(named = useMinikubeEnv, matches = "true")
    void testProvisionAddressSpaceStandard() throws Exception {
        AddressSpace standard = new AddressSpace("addr-space-standard", AddressSpaceType.STANDARD);
        provisionedServices.put(getUserProjectName(standard), standard);
        OpenshiftWebPage ocPage = new OpenshiftWebPage(selenium, addressApiClient, getOCConsoleRoute(),
                new KeycloakCredentials("developer", "developer"));
        ocPage.openOpenshiftPage();
        ocPage.provisionAddressSpaceViaSC(standard, getUserProjectName(standard));
        ocPage.deprovisionAddressSpace(getUserProjectName(standard));
    }

    @Test
    @DisabledIfEnvironmentVariable(named = useMinikubeEnv, matches = "true")
    void testCreateDeleteBindings() throws Exception {
        AddressSpace brokered = new AddressSpace("test-binding-space", AddressSpaceType.BROKERED);
        provisionedServices.put(getUserProjectName(brokered), brokered);
        OpenshiftWebPage ocPage = new OpenshiftWebPage(selenium, addressApiClient, getOCConsoleRoute(),
                new KeycloakCredentials("developer", "developer"));
        ocPage.openOpenshiftPage();
        ocPage.provisionAddressSpaceViaSC(brokered, getUserProjectName(brokered));
        String external = ocPage.createBinding(getUserProjectName(brokered),
                false, false, true, null, null);
        String consoleAdmin = ocPage.createBinding(getUserProjectName(brokered),
                false, true, false, null, null);
        String consoleAccess = ocPage.createBinding(getUserProjectName(brokered),
                true, false, false, null, null);
        ocPage.removeBinding(getUserProjectName(brokered), external);
        ocPage.removeBinding(getUserProjectName(brokered), consoleAccess);
        ocPage.removeBinding(getUserProjectName(brokered), consoleAdmin);
    }

    @Test
    @DisabledIfEnvironmentVariable(named = useMinikubeEnv, matches = "true")
    void testCreateBindingCreateAddressSendReceive() throws Exception {
        AddressSpace brokered = new AddressSpace("test-external-messaging-space", AddressSpaceType.BROKERED);
        provisionedServices.put(getUserProjectName(brokered), brokered);
        OpenshiftWebPage ocPage = new OpenshiftWebPage(selenium, addressApiClient, getOCConsoleRoute(),
                new KeycloakCredentials("developer", "developer"));
        ocPage.openOpenshiftPage();
        ocPage.provisionAddressSpaceViaSC(brokered, getUserProjectName(brokered));
        String bindingID = ocPage.createBinding(getUserProjectName(brokered),
                false, true, true, null, null);
        BindingSecretData credentials = ocPage.viewSecretOfBinding(getUserProjectName(brokered), bindingID);

    }

    @Test
    @Disabled("IN PROGRESS")
    void testLoginViaOpensShiftCredentials() throws Exception {
        AddressSpace brokeredSpace = new AddressSpace("login-via-oc-brokered", AddressSpaceType.BROKERED);
        provisionedServices.put(getUserProjectName(brokeredSpace), brokeredSpace);
        OpenshiftWebPage ocPage = new OpenshiftWebPage(selenium, addressApiClient, getOCConsoleRoute(), developer);
        ocPage.openOpenshiftPage();
        ocPage.provisionAddressSpaceViaSC(brokeredSpace, getUserProjectName(brokeredSpace));
//        ConsoleWebPage consoleWebPage = ocPage.clickOnDashboard();
//        consoleWebPage
        waitForAddressSpaceReady(brokeredSpace);
        reloadAddressSpaceEndpoints(brokeredSpace);
        //this is important due to update of endpoint which are usually updated

        ConsoleWebPage consolePage = new ConsoleWebPage(selenium, getConsoleRoute(brokeredSpace),
                addressApiClient, brokeredSpace, developer);
        consolePage.openWebConsolePage(true);
        fail("korny za to nemuze");

    }

}
