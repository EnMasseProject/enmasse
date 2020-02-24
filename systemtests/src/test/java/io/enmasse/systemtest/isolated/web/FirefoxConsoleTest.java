/*
 * Copyright 2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.systemtest.isolated.web;

import io.enmasse.address.model.AddressSpaceBuilder;
import io.enmasse.systemtest.bases.isolated.ITestIsolatedStandard;
import io.enmasse.systemtest.bases.web.ConsoleTest;
import io.enmasse.systemtest.messagingclients.ExternalClients;
import io.enmasse.systemtest.model.addressplan.DestinationPlan;
import io.enmasse.systemtest.model.addressspace.AddressSpacePlans;
import io.enmasse.systemtest.model.addressspace.AddressSpaceType;
import io.enmasse.systemtest.selenium.SeleniumFirefox;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static io.enmasse.systemtest.TestTag.ACCEPTANCE;

@SeleniumFirefox
class FirefoxConsoleTest extends ConsoleTest implements ITestIsolatedStandard {

    @Test
    void testLoginLogout() throws Exception {
        doTestOpen();
    }

    @Test
    void testAddressSpaceSnippetStandard() throws Exception {
        doTestAddressSpaceSnippet(AddressSpaceType.STANDARD);
    }

    @Test
    void testAddressSnippetStandard() throws Exception {
        doTestAddressSnippet(AddressSpaceType.STANDARD, DestinationPlan.STANDARD_SMALL_QUEUE);
    }

    @Test
    void testAddressSpaceSnippetBrokered() throws Exception {
        doTestAddressSpaceSnippet(AddressSpaceType.BROKERED);
    }

    @Test
    void testAddressSnippetBrokered() throws Exception {
        doTestAddressSnippet(AddressSpaceType.BROKERED, DestinationPlan.BROKERED_QUEUE);
    }

    @Test
    @Tag(ACCEPTANCE)
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
    @ExternalClients
    void testCreateAddrSpaceNonClusterAdmin() throws Exception {
        doTestCreateAddrSpaceNonClusterAdmin();
    }

    @Test
    void testViewAddressSpacesAsBasicUser() throws Exception {
        doTestRestrictAddressSpaceView();
    }

    @Test
    void testEditAddressSpace() throws Exception {
        doEditAddressSpace();
    }

    @Test
    void testFilterAddressSpace() throws Exception {
        doTestFilterAddrSpace();
    }

    @Test
    void testHelpLink() throws Exception {
        doTestHelpLink();
    }
}

