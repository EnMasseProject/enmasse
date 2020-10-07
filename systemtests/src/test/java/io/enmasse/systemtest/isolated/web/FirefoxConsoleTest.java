/*
 * Copyright 2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.systemtest.isolated.web;

import io.enmasse.address.model.AddressSpaceBuilder;
import io.enmasse.systemtest.annotations.ExternalClients;
import io.enmasse.systemtest.annotations.SeleniumFirefox;
import io.enmasse.systemtest.bases.isolated.ITestIsolatedStandard;
import io.enmasse.systemtest.bases.web.ConsoleTest;
import io.enmasse.systemtest.model.address.AddressType;
import io.enmasse.systemtest.model.addressplan.DestinationPlan;
import io.enmasse.systemtest.model.addressspace.AddressSpacePlans;
import io.enmasse.systemtest.model.addressspace.AddressSpaceType;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static io.enmasse.systemtest.TestTag.ACCEPTANCE;

@SeleniumFirefox
class FirefoxConsoleTest extends ConsoleTest implements ITestIsolatedStandard {

    @Test
    void testLoginLogout() throws Exception {
        doTestOpen();
    }

    @Test
    void testOpenshiftClientInSnippet() throws Exception {
        doTestSnippetClient(AddressSpaceType.STANDARD);
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
    @Tag(ACCEPTANCE)
    void testCreateAddrSpaceWithCustomAuthService() throws Exception {
        doTestCreateAddrSpaceWithCustomAuthService();
    }

    @Test
    void testViewAddressSpaceCreatedByApi() throws Exception {
        doTestViewAddressSpace();
    }

    @Test
    @ExternalClients
    void testCreateAddrSpaceNonClusterAdminMinimal() throws Exception {
        doTestCreateAddrSpaceNonClusterAdminMinimal();
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
    @Tag(ACCEPTANCE)
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

    @Test
    void testGoneAwayPageAfterAddressSpaceDeletion() throws Exception {
        doTestGoneAwayPageAfterAddressSpaceDeletion();
    }

    @Test
    void testGoneAwayAfterAddressDeletion() throws Exception {
        doTestGoneAwayPageAfterAddressDeletion();
    }

    @Test
    void testViewCustomPlans() throws Exception {
        doTestViewCustomPlans();
    }

    @Test
    @Tag(ACCEPTANCE)
    void testFilterAddressSpaceStatus() throws Exception {
        doTestFilterAddressSpaceStatus();
    }

    @Test
    void testListEndpoints() throws Exception {
        doTestListEndpoints();
    }

    @Test
    void testAddressSpaceEndpointSelfsignedCert() throws Exception {
        doTestEndpointSystemProvided();
    }

    @Test
    void testAddressSpaceEndpointOpenshiftProvided() throws Exception {
        doTestEndpointOpenshiftProvided();
    }

    @Test
    void testEndpointCustomCertsProvided() throws Exception {
        doTestEndpointCustomCertsProvided();
    }

    @ParameterizedTest(name = "testAddressExpiry-{0}")
    @ValueSource(strings = {"standard", "brokered"})
    void testAddressExpiry(String type) throws Exception {
        doTestMessageRedelivery(AddressSpaceType.getEnum(type), AddressType.QUEUE, true);
    }

    @Test
    void testAddressExpiryTopic() throws Exception {
        doTestMessageRedelivery(AddressSpaceType.STANDARD, AddressType.TOPIC, true);
    }

    @ParameterizedTest(name = "testAddressSpecified-{0}-UI")
    @ValueSource(strings = {"standard", "brokered"})
    void testAddressSpecified(String type) throws Exception {
        doTestMessageRedelivery(AddressSpaceType.getEnum(type), AddressType.QUEUE, false);
    }

    @Test
    void testTopicAddressSpecifiedUI() throws Exception {
        doTestMessageRedelivery(AddressSpaceType.STANDARD, AddressType.TOPIC, false);
    }

}

