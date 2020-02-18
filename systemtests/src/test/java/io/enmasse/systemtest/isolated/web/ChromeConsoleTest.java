/*
 * Copyright 2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.systemtest.isolated.web;

import io.enmasse.address.model.AddressSpaceBuilder;
import io.enmasse.systemtest.bases.isolated.ITestIsolatedStandard;
import io.enmasse.systemtest.bases.web.ConsoleTest;
import io.enmasse.systemtest.model.addressspace.AddressSpacePlans;
import io.enmasse.systemtest.model.addressspace.AddressSpaceType;
import io.enmasse.systemtest.selenium.SeleniumChrome;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static io.enmasse.systemtest.TestTag.NON_PR;

@Tag(NON_PR)
@Disabled
@SeleniumChrome
class ChromeConsoleTest extends ConsoleTest implements ITestIsolatedStandard {

    @Test
    void testLoginLogout() throws Exception {
        doTestOpen();
    }

    @Test
    void testCreateDeleteAddressSpace() throws Exception {
        doTestCreateDeleteAddressSpace(new AddressSpaceBuilder()
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
        doTestCreateDeleteAddressSpace(new AddressSpaceBuilder()
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
    void testCreateAddrSpaceWithCustomAuthService() throws Exception {
        doTestCreateAddrSpaceWithCustomAuthService();
    }

    @Test
    void testViewAddressSpaceCreatedByApi() throws Exception {
        doTestViewAddressSpace();
    }

    @Test
    void testCreateAddrSpaceNonClusterAdmin() throws Exception {
        doTestCreateAddrSpaceNonClusterAdmin();
    }

    @Test
    void testEditAddressSpace() throws Exception {
        doEditAddressSpace();
    }

    @Test
    void testFilterAddressSpace() throws Exception {
        doTestFilterAddrSpace();
    }
}

