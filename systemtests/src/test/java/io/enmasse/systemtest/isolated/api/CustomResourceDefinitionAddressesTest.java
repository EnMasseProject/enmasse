/*
 * Copyright 2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package io.enmasse.systemtest.isolated.api;

import io.enmasse.address.model.Address;
import io.enmasse.address.model.AddressBuilder;
import io.enmasse.address.model.AddressSpace;
import io.enmasse.address.model.AddressSpaceBuilder;
import io.enmasse.systemtest.UserCredentials;
import io.enmasse.systemtest.bases.TestBase;
import io.enmasse.systemtest.bases.isolated.ITestIsolatedStandard;
import io.enmasse.systemtest.platform.KubeCMDClient;
import io.enmasse.systemtest.executor.ExecutionResultData;
import io.enmasse.systemtest.model.addressplan.DestinationPlan;
import io.enmasse.systemtest.model.addressspace.AddressSpacePlans;
import io.enmasse.systemtest.model.addressspace.AddressSpaceType;
import io.enmasse.systemtest.selenium.SeleniumFirefox;
import io.enmasse.systemtest.selenium.SeleniumProvider;
import io.enmasse.systemtest.selenium.page.ConsoleWebPage;
import io.enmasse.systemtest.time.TimeoutBudget;
import io.enmasse.systemtest.utils.AddressUtils;
import io.enmasse.systemtest.utils.TestUtils;
import io.vertx.core.json.JsonObject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SeleniumFirefox
public class CustomResourceDefinitionAddressesTest extends TestBase implements ITestIsolatedStandard {
    SeleniumProvider selenium = SeleniumProvider.getInstance();
    private AddressSpace brokered;
    private UserCredentials userCredentials;

    @BeforeEach
    void setUpSelenium() throws Exception {
        brokered = new AddressSpaceBuilder()
                .withNewMetadata()
                .withName("crd-address-space")
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
        resourcesManager.createAddressSpace(brokered);
        userCredentials = new UserCredentials("test", "test");
        resourcesManager.createOrUpdateUser(brokered, userCredentials);
    }

    @Test
    void testAddressCreateViaAgentApiRemoveViaCmd() throws Exception {
        Address dest1 = new AddressBuilder()
                .withNewMetadata()
                .withNamespace(brokered.getMetadata().getNamespace())
                .withName(AddressUtils.generateAddressMetadataName(brokered, "mytopic-agent"))
                .endMetadata()
                .withNewSpec()
                .withType("topic")
                .withAddress("mytopic-agent")
                .withPlan(DestinationPlan.BROKERED_TOPIC)
                .endSpec()
                .build();
        Address dest2 = new AddressBuilder()
                .withNewMetadata()
                .withNamespace(brokered.getMetadata().getNamespace())
                .withName(AddressUtils.generateAddressMetadataName(brokered, "mytopic-api"))
                .endMetadata()
                .withNewSpec()
                .withType("topic")
                .withAddress("mytopic-api")
                .withPlan(DestinationPlan.BROKERED_TOPIC)
                .endSpec()
                .build();

        ConsoleWebPage consoleWeb = new ConsoleWebPage(selenium, kubernetes.getConsoleRoute(brokered), brokered, clusterUser);
        consoleWeb.openWebConsolePage();
        consoleWeb.openAddressesPageWebConsole();
        consoleWeb.createAddressWebConsole(dest1, false);

        resourcesManager.appendAddresses(false, dest2);
        AddressUtils.waitForDestinationsReady(dest1, dest2);

        Address addressFromConsole = kubernetes.getAddressClient(brokered.getMetadata().getNamespace()).list().getItems()
                .stream().filter(address -> address.getSpec().getAddress().equals(dest1.getSpec().getAddress())).findFirst().orElse(null);
        assertNotNull(addressFromConsole, "Didn't receive address from api server");

        // Patch new label
        assertTrue(KubeCMDClient.patchCR(Address.KIND.toLowerCase(), addressFromConsole.getMetadata().getName(), "{\"metadata\":{\"annotations\":{\"mylabel\":\"myvalue\"}}}").getRetCode());

        KubeCMDClient.deleteAddress(environment.namespace(), addressFromConsole.getMetadata().getName());
        KubeCMDClient.deleteAddress(environment.namespace(), dest2.getMetadata().getName());

        TestUtils.waitUntilCondition(() -> {
            ExecutionResultData allAddresses = KubeCMDClient.getAddress(environment.namespace(), "-a");
            return allAddresses.getStdErr() + allAddresses.getStdOut();
        }, "No resources found.", new TimeoutBudget(30, TimeUnit.SECONDS));
    }

    @Test
    void testAddressCreateViaCmdRemoveViaAgentApi() throws Exception {
        Address dest1 = new AddressBuilder()
                .withNewMetadata()
                .withNamespace(brokered.getMetadata().getNamespace())
                .withName(AddressUtils.generateAddressMetadataName(brokered, "myqueue"))
                .endMetadata()
                .withNewSpec()
                .withType("queue")
                .withAddress("myqueue")
                .withPlan(DestinationPlan.BROKERED_QUEUE)
                .endSpec()
                .build();

        Address dest2 = new AddressBuilder()
                .withNewMetadata()
                .withNamespace(brokered.getMetadata().getNamespace())
                .withName(AddressUtils.generateAddressMetadataName(brokered, "myqueue2"))
                .endMetadata()
                .withNewSpec()
                .withType("queue")
                .withAddress("myqueue2")
                .withPlan(DestinationPlan.BROKERED_QUEUE)
                .endSpec()
                .build();

        JsonObject address1 = AddressUtils.addressToJson(dest1);
        String address2 = AddressUtils.addressToYaml(dest2);

        ExecutionResultData result = KubeCMDClient.createCR(address1.toString());
        String output = result.getStdOut().trim();

        String addressString = "%s/%s created";
        List<String> dest1Expected = Arrays.asList(
                String.format(addressString, "address", dest1.getMetadata().getName()),
                String.format(addressString, "address.enmasse.io", dest1.getMetadata().getName()));
        assertTrue(dest1Expected.contains(output),
                String.format("Unexpected response on create custom resource '%s': %s", address1.toString(), output));
        assertTrue(result.getRetCode(), String.format("Expected return code 0 on create custom resource '%s'", address1.toString()));

        result = KubeCMDClient.createCR(address2);
        output = result.getStdOut().trim();

        List<String> dest2Expected = Arrays.asList(
                String.format(addressString, "address", dest2.getMetadata().getName()),
                String.format(addressString, "address.enmasse.io", dest2.getMetadata().getName()));
        assertTrue(dest2Expected.contains(output),
                String.format("Unexpected response on create custom resource '%s': %s", address2, output));
        assertTrue(result.getRetCode(), String.format("Expected return code 0 on create custom resource '%s'", address2));

        AddressUtils.waitForDestinationsReady(dest1, dest2);

        result = KubeCMDClient.getAddress(environment.namespace(), "-a");
        output = result.getStdOut().trim();

        assertTrue(output.contains(Objects.requireNonNull(dest1.getMetadata().getName())),
                String.format("Get all addresses should contains '%s'; but contains only: %s",
                        dest1.getMetadata().getName(), output));
        assertTrue(output.contains(Objects.requireNonNull(dest2.getMetadata().getName())),
                String.format("Get all addresses should contains '%s'; but contains only: %s",
                        dest2.getMetadata().getName(), output));

        ConsoleWebPage consoleWeb = new ConsoleWebPage(selenium, kubernetes.getConsoleRoute(brokered), brokered, clusterUser);
        consoleWeb.openWebConsolePage();
        consoleWeb.openAddressesPageWebConsole();
        consoleWeb.deleteAddressWebConsole(dest1);
        resourcesManager.deleteAddresses(dest2);

        TestUtils.waitUntilCondition(() -> {
            ExecutionResultData addresses = KubeCMDClient.getAddress(environment.namespace(), "-a");
            return addresses.getStdOut() + addresses.getStdErr();
        }, "No resources found.", new TimeoutBudget(30, TimeUnit.SECONDS));
    }
}
