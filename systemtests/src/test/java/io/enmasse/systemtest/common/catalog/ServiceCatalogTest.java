/*
 * Copyright 2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.systemtest.common.catalog;

import io.enmasse.systemtest.*;
import io.enmasse.systemtest.bases.TestBase;
import io.enmasse.systemtest.selenium.ISeleniumProviderFirefox;
import io.enmasse.systemtest.selenium.page.OpenshiftWebPage;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;

import static io.enmasse.systemtest.TestTag.isolated;

@Tag(isolated)
class ServiceCatalogTest extends TestBase implements ISeleniumProviderFirefox {

    private static Logger log = CustomLogger.getLogger();
    String projectName = "user";

    @BeforeEach
    void setUpDrivers() throws Exception {
        selenium.setupDriver(environment, kubernetes, buildDriver());
    }

    @AfterEach
    void tearDownWebConsoleTests() throws Exception {
        selenium.tearDownDrivers();
        getAddressSpaceList().forEach(addressSpace -> {
            try {
                deleteAddressSpaceCreatedBySC(projectName, addressSpace);
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    @Test
    void testCreateAddressSpaceBrokered() throws Exception {
        AddressSpace brokered = new AddressSpace("brokered", AddressSpaceType.BROKERED, AuthService.STANDARD);
        addToAddressSpaceList(brokered);
        OpenshiftWebPage ocPage = new OpenshiftWebPage(selenium, addressApiClient, getOCConsoleRoute(),
                new KeycloakCredentials("developer", "developer"));
        ocPage.openOpenshiftPage();
        ocPage.createAddressSpace(brokered, projectName);
    }

    @Test
    void testCreateAddressSpaceStandard() throws Exception {
        AddressSpace standard = new AddressSpace("standard", AddressSpaceType.STANDARD, AuthService.STANDARD);
        addToAddressSpaceList(standard);
        OpenshiftWebPage ocPage = new OpenshiftWebPage(selenium, addressApiClient, getOCConsoleRoute(),
                new KeycloakCredentials("developer", "developer"));
        ocPage.openOpenshiftPage();
        ocPage.createAddressSpace(standard, projectName);
    }

}
