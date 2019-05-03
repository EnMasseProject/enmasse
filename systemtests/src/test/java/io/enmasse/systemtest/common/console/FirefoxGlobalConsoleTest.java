/*
 * Copyright 2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.systemtest.common.console;

import io.enmasse.address.model.AddressSpaceBuilder;
import io.enmasse.systemtest.AddressSpacePlans;
import io.enmasse.systemtest.AddressSpaceType;
import io.enmasse.systemtest.bases.web.GlobalConsoleTest;
import io.enmasse.systemtest.selenium.ISeleniumProviderFirefox;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static io.enmasse.systemtest.TestTag.isolated;

@Tag(isolated)
class FirefoxGlobalConsoleTest extends GlobalConsoleTest implements ISeleniumProviderFirefox {

    @Test
    void testLoginLogout() throws Exception {
        doTestOpen();
    }

    @Test
    void testCreateDeleteAddressSpace() throws Exception {
        doTestCreateAddressSpace(new AddressSpaceBuilder()
                .withNewMetadata()
                .withName("test-addr-space-brokered")
                .withNamespace(kubernetes.getInfraNamespace())
                .endMetadata()
                .withNewSpec()
                .withType(AddressSpaceType.BROKERED.toString())
                .withPlan(AddressSpacePlans.BROKERED)
                .withNewAuthenticationService()
                .withName("standard-authservice")
                .endAuthenticationService()
                .endSpec()
                .build());
        doTestCreateAddressSpace(new AddressSpaceBuilder()
                .withNewMetadata()
                .withName("test-addr-space-standard")
                .withNamespace(kubernetes.getInfraNamespace())
                .endMetadata()
                .withNewSpec()
                .withType(AddressSpaceType.STANDARD.toString())
                .withPlan(AddressSpacePlans.STANDARD_SMALL)
                .withNewAuthenticationService()
                .withName("standard-authservice")
                .endAuthenticationService()
                .endSpec()
                .build());
    }

    @Test
    void testConnectToAddressSpaceConsole() throws Exception {
        doTestConnectToAddressSpaceConsole(new AddressSpaceBuilder()
                .withNewMetadata()
                .withName("test-addr-space-console")
                .withNamespace(kubernetes.getInfraNamespace())
                .endMetadata()
                .withNewSpec()
                .withType(AddressSpaceType.BROKERED.toString())
                .withPlan(AddressSpacePlans.BROKERED)
                .withNewAuthenticationService()
                .withName("standard-authservice")
                .endAuthenticationService()
                .endSpec()
                .build());
    }

    @Test
    void testCreateAddrSpaceWithCustomAuthService() throws Exception {
        doTestCreateAddrSpaceWithCustomAuthService();
    }

    @Test
    void testViewAddressSpaceCreatedByApi() throws Exception {
        doTestViewAddressSpace();
    }
}

