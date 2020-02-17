/*
 * Copyright 2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.systemtest.isolated.web;

import io.enmasse.address.model.Address;
import io.enmasse.address.model.AddressBuilder;
import io.enmasse.address.model.AddressSpace;
import io.enmasse.address.model.AddressSpaceBuilder;
import io.enmasse.systemtest.bases.isolated.ITestIsolatedStandard;
import io.enmasse.systemtest.bases.web.ConsoleTest;
import io.enmasse.systemtest.model.addressplan.DestinationPlan;
import io.enmasse.systemtest.model.addressspace.AddressSpacePlans;
import io.enmasse.systemtest.model.addressspace.AddressSpaceType;
import io.enmasse.systemtest.platform.Kubernetes;
import io.enmasse.systemtest.selenium.SeleniumChrome;
import io.enmasse.systemtest.utils.AddressSpaceUtils;
import io.enmasse.systemtest.utils.AddressUtils;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static io.enmasse.systemtest.TestTag.NON_PR;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Tag(NON_PR)
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
    void testAddressSpaceSnippetStandard() throws Exception {
        AddressSpace addressSpace = new AddressSpaceBuilder()
                .withNewMetadata()
                .withName("test-address-space")
                .withNamespace(Kubernetes.getInstance().getInfraNamespace())
                .endMetadata()
                .withNewSpec()
                .withType(AddressSpaceType.STANDARD.toString())
                .withPlan(AddressSpacePlans.STANDARD_MEDIUM)
                .withNewAuthenticationService()
                .withName("standard-authservice")
                .endAuthenticationService()
                .endSpec()
                .build();

        doTestAddressSpaceDeploymentSnippet(addressSpace);
        assertTrue(AddressSpaceUtils.addressSpaceExists(Kubernetes.getInstance().getInfraNamespace(),
                addressSpace.getMetadata().getName()));
    }

    @Test
    void testAddressSnippetStandard() throws Exception {
        AddressSpace addressSpace = new AddressSpaceBuilder()
                .withNewMetadata()
                .withName("test-address-space-standard")
                .withNamespace(Kubernetes.getInstance().getInfraNamespace())
                .endMetadata()
                .withNewSpec()
                .withType(AddressSpaceType.STANDARD.toString())
                .withPlan(AddressSpacePlans.STANDARD_MEDIUM)
                .withNewAuthenticationService()
                .withName("standard-authservice")
                .endAuthenticationService()
                .endSpec()
                .build();
        isolatedResourcesManager.createAddressSpace(addressSpace);

        Address address = new AddressBuilder()
                .withNewMetadata()
                .withNamespace(Kubernetes.getInstance().getInfraNamespace())
                .withName(AddressUtils.generateAddressMetadataName(addressSpace, "test-queue"))
                .endMetadata()
                .withNewSpec()
                .withAddress("test-queue")
                .withType("queue")
                .withPlan(DestinationPlan.STANDARD_SMALL_QUEUE)
                .endSpec()
                .build();
        doTestAddressDeploymentSnippet(addressSpace, address);
        AddressUtils.waitForDestinationsReady(address);
        AddressUtils.isAddressReady(address);
    }

    @Test
    void testAddressSpaceSnippetBrokered() throws Exception {
        AddressSpace addressSpace = new AddressSpaceBuilder()
                .withNewMetadata()
                .withName("test-address-space-brokered")
                .withNamespace(Kubernetes.getInstance().getInfraNamespace())
                .endMetadata()
                .withNewSpec()
                .withType(AddressSpaceType.BROKERED.toString())
                .withPlan(AddressSpacePlans.BROKERED)
                .withNewAuthenticationService()
                .withName("standard-authservice")
                .endAuthenticationService()
                .endSpec()
                .build();

        doTestAddressSpaceDeploymentSnippet(addressSpace);
        assertTrue(AddressSpaceUtils.addressSpaceExists(Kubernetes.getInstance().getInfraNamespace(),
                addressSpace.getMetadata().getName()));
    }

    @Test
    void testAddressSnippetBrokered() throws Exception {
        AddressSpace addressSpace = new AddressSpaceBuilder()
                .withNewMetadata()
                .withName("test-address-space-brokered")
                .withNamespace(Kubernetes.getInstance().getInfraNamespace())
                .endMetadata()
                .withNewSpec()
                .withType(AddressSpaceType.BROKERED.toString())
                .withPlan(AddressSpacePlans.BROKERED)
                .withNewAuthenticationService()
                .withName("standard-authservice")
                .endAuthenticationService()
                .endSpec()
                .build();
        isolatedResourcesManager.createAddressSpace(addressSpace);

        Address address = new AddressBuilder()
                .withNewMetadata()
                .withNamespace(Kubernetes.getInstance().getInfraNamespace())
                .withName(AddressUtils.generateAddressMetadataName(addressSpace, "test-queue"))
                .endMetadata()
                .withNewSpec()
                .withAddress("test-queue")
                .withType("queue")
                .withPlan(DestinationPlan.BROKERED_QUEUE)
                .endSpec()
                .build();
        doTestAddressDeploymentSnippet(addressSpace, address);
        AddressUtils.waitForDestinationsReady(address);
        AddressUtils.isAddressReady(address);
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

    @Test
    void testHelpLink() throws Exception {
        doTestHelpLink();
    }
}

