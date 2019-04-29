/*
 * Copyright 2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package io.enmasse.systemtest.common.api;

import io.enmasse.address.model.Address;
import io.enmasse.address.model.AddressSpace;
import io.enmasse.address.model.AddressSpaceBuilder;
import io.enmasse.systemtest.*;
import io.enmasse.systemtest.bases.TestBase;
import io.enmasse.systemtest.cmdclients.KubeCMDClient;
import io.enmasse.systemtest.executor.ExecutionResultData;
import io.enmasse.systemtest.selenium.ISeleniumProviderChrome;
import io.enmasse.systemtest.selenium.page.ConsoleWebPage;
import io.enmasse.systemtest.utils.AddressUtils;
import io.enmasse.systemtest.utils.TestUtils;
import io.vertx.core.json.JsonObject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

import static io.enmasse.systemtest.TestTag.isolated;
import static org.junit.jupiter.api.Assertions.*;

@Tag(isolated)
public class CustomResourceDefinitionAddressesTest extends TestBase implements ISeleniumProviderChrome {
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
                .withType(AddressSpaceType.BROKERED.toString().toLowerCase())
                .withPlan(AddressSpacePlans.BROKERED)
                .withNewAuthenticationService()
                .withName("standard-authservice")
                .endAuthenticationService()
                .endSpec()
                .build();
        createAddressSpace(brokered);
        userCredentials = new UserCredentials("test", "test");
        createUser(brokered, userCredentials);
        if (selenium.getDriver() == null) {
            selenium.setupDriver(environment, kubernetes, TestUtils.getChromeDriver());
        } else {
            selenium.clearScreenShots();
        }
    }

    @Test
    void testAddressCreateViaAgentApiRemoveViaCmd() throws Exception {
        Address dest1 = AddressUtils.createTopicAddressObject("mytopic-agent", DestinationPlan.BROKERED_TOPIC);
        Address dest2 = AddressUtils.createTopicAddressObject("mytopic-api", DestinationPlan.BROKERED_TOPIC);

        ConsoleWebPage consoleWeb = new ConsoleWebPage(selenium, getConsoleRoute(brokered), brokered, clusterUser);
        consoleWeb.openWebConsolePage();
        consoleWeb.openAddressesPageWebConsole();
        consoleWeb.createAddressWebConsole(dest1, false);

        appendAddresses(brokered, false, dest2);
        waitForDestinationsReady(brokered, dest1, dest2);

        ExecutionResultData addresses = KubeCMDClient.getAddress(environment.namespace(), "-a");
        String output = addresses.getStdOut().trim();


        assertAll(() -> {
            assertTrue(output.contains(Objects.requireNonNull(AddressUtils.generateAddressMetadataName(brokered.getMetadata().getName(), dest1))),
                    String.format("Get all addresses should contains '%s'; but contains only: %s",
                            AddressUtils.generateAddressMetadataName(brokered.getMetadata().getName(), dest1), output));
            assertTrue(output.contains(Objects.requireNonNull(AddressUtils.generateAddressMetadataName(brokered.getMetadata().getName(), dest2))),
                    String.format("Get all addresses should contains '%s'; but contains only: %s",
                            AddressUtils.generateAddressMetadataName(brokered.getMetadata().getName(), dest2), output));
        });


        Address addressesObjects = kubernetes.getAddressClient(brokered.getMetadata().getNamespace()).withName(dest1.getMetadata().getName()).get();
        assertNotNull(addressesObjects, "Didn't receive address from api server");

        // Patch new label
        assertTrue(KubeCMDClient.patchCR(Address.KIND.toLowerCase(), dest1.getMetadata().getName(), "{\"metadata\":{\"annotations\":{\"mylabel\":\"myvalue\"}}}").getRetCode());

        KubeCMDClient.deleteAddress(environment.namespace(), dest1.getMetadata().getName());
        KubeCMDClient.deleteAddress(environment.namespace(), AddressUtils.generateAddressMetadataName(brokered.getMetadata().getName(), dest2));

        TestUtils.waitUntilCondition(() -> {
            ExecutionResultData allAddresses = KubeCMDClient.getAddress(environment.namespace(), "-a");
            return allAddresses.getStdErr() + allAddresses.getStdOut();
        }, "No resources found.", new TimeoutBudget(30, TimeUnit.SECONDS));
    }

    @Test
    void testAddressCreateViaCmdRemoveViaAgentApi() throws Exception {
        Address dest1 = AddressUtils.createAddressObject("myqueue1", null, brokered.getMetadata().getName(), "myqueue1",
                AddressType.QUEUE.toString(), DestinationPlan.BROKERED_QUEUE);
        Address dest2 = AddressUtils.createAddressObject("myqueue2", null, brokered.getMetadata().getName(), "myqueue2",
                AddressType.QUEUE.toString(), DestinationPlan.BROKERED_QUEUE);

        JsonObject address1 = AddressUtils.addressToJson(brokered.getMetadata().getName(), dest1);
        String address2 = AddressUtils.addressToYaml(brokered.getMetadata().getName(), dest2);

        ExecutionResultData result = KubeCMDClient.createCR(address1.toString());
        String output = result.getStdOut().trim();

        String addressString = "%s \"%s.%s\" created";
        String address2String = "%s/%s.%s created";
        List<String> dest1Expected = Arrays.asList(
                String.format(addressString, "address", brokered.getMetadata().getName(), dest1.getMetadata().getName()),
                String.format(addressString, "address.enmasse.io", brokered.getMetadata().getName(), dest1.getMetadata().getName()),
                String.format(address2String, "address.enmasse.io", brokered.getMetadata().getName(), dest1.getMetadata().getName()));
        assertTrue(dest1Expected.contains(output),
                String.format("Unexpected response on create custom resource '%s': %s", address1.toString(), output));
        assertTrue(result.getRetCode(), String.format("Expected return code 0 on create custom resource '%s'", address1.toString()));

        result = KubeCMDClient.createCR(address2);
        output = result.getStdOut().trim();

        List<String> dest2Expected = Arrays.asList(
                String.format(addressString, "address", brokered.getMetadata().getName(), dest2.getMetadata().getName()),
                String.format(addressString, "address.enmasse.io", brokered.getMetadata().getName(), dest2.getMetadata().getName()),
                String.format(address2String, "address.enmasse.io", brokered.getMetadata().getName(), dest2.getMetadata().getName()));
        assertTrue(dest2Expected.contains(output),
                String.format("Unexpected response on create custom resource '%s': %s", address2, output));
        assertTrue(result.getRetCode(), String.format("Expected return code 0 on create custom resource '%s'", address2));

        waitForDestinationsReady(brokered, dest1, dest2);

        result = KubeCMDClient.getAddress(environment.namespace(), "-a");
        output = result.getStdOut().trim();

        assertTrue(output.contains(Objects.requireNonNull(AddressUtils.generateAddressMetadataName(brokered.getMetadata().getName(), dest1))),
                String.format("Get all addresses should contains '%s'; but contains only: %s",
                        AddressUtils.generateAddressMetadataName(brokered.getMetadata().getName(), dest1), output));
        assertTrue(output.contains(Objects.requireNonNull(AddressUtils.generateAddressMetadataName(brokered.getMetadata().getName(), dest2))),
                String.format("Get all addresses should contains '%s'; but contains only: %s",
                        AddressUtils.generateAddressMetadataName(brokered.getMetadata().getName(), dest2), output));

        ConsoleWebPage consoleWeb = new ConsoleWebPage(selenium, getConsoleRoute(brokered), brokered, clusterUser);
        consoleWeb.openWebConsolePage();
        consoleWeb.openAddressesPageWebConsole();
        consoleWeb.deleteAddressWebConsole(dest1);
        deleteAddresses(brokered, dest2);

        TestUtils.waitUntilCondition(() -> {
            ExecutionResultData addresses = KubeCMDClient.getAddress(environment.namespace(), "-a");
            return addresses.getStdOut() + addresses.getStdErr();
        }, "No resources found.", new TimeoutBudget(30, TimeUnit.SECONDS));
    }
}
