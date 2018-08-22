/*
 * Copyright 2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package io.enmasse.systemtest.common.api;

import io.enmasse.systemtest.*;
import io.enmasse.systemtest.bases.TestBase;
import io.enmasse.systemtest.cmdclients.CRDCmdClient;
import io.enmasse.systemtest.executor.ExecutionResultData;
import io.enmasse.systemtest.selenium.ISeleniumProviderChrome;
import io.enmasse.systemtest.selenium.page.ConsoleWebPage;
import io.vertx.core.json.JsonObject;
import org.junit.jupiter.api.*;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static io.enmasse.systemtest.TestTag.isolated;
import static org.junit.jupiter.api.Assertions.*;

@Tag(isolated)
public class CustomResourceDefinitionAddressesTest extends TestBase implements ISeleniumProviderChrome {
    private AddressSpace brokered;

    protected AddressSpace getSharedAddressSpace() {
        return brokered;
    }

    @AfterAll
    void tearDownAddressSpace() throws Exception {
        deleteAddressSpace(brokered);
    }

    @BeforeEach
    void setUpSelenium() throws Exception {
        if (brokered == null) {
            brokered = new AddressSpace("crd-address-test-shared", AddressSpaceType.BROKERED, AuthService.NONE);
            createAddressSpace(brokered);
        }
        if (selenium.getDriver() == null) {
            selenium.setupDriver(environment, kubernetes, TestUtils.getChromeDriver());
        } else {
            selenium.clearScreenShots();
        }
    }

    @AfterEach
    void tearDownAddresses() throws Exception {
        if (brokered != null) {
            setAddresses(brokered);
        }
    }

    @Test
    void testAddressCreateViaAgentApiRemoveViaCmd() throws Exception {
        Destination dest1 = Destination.topic("mytopic-agent", "brokered-topic");
        Destination dest2 = Destination.topic("mytopic-api", "brokered-topic");

        ConsoleWebPage consoleWeb = new ConsoleWebPage(selenium, getConsoleRoute(brokered), addressApiClient, brokered, new KeycloakCredentials(null, null));
        consoleWeb.openWebConsolePage();
        consoleWeb.openAddressesPageWebConsole();
        consoleWeb.createAddressWebConsole(dest1, false);

        appendAddresses(brokered, false, dest2);
        waitForDestinationsReady(brokered, dest1, dest2);

        ExecutionResultData addresses = CRDCmdClient.getAddress(environment.namespace(), "-a");
        String output = addresses.getStdOut().trim();


        assertAll(() -> {
            assertTrue(output.contains(dest1.getAddressName(brokered.getName())),
                    String.format("Get all addresses should contains '%s'; but contains only: %s",
                            dest1.getAddressName(brokered.getName()), output));
            assertTrue(output.contains(dest2.getAddressName(brokered.getName())),
                    String.format("Get all addresses should contains '%s'; but contains only: %s",
                            dest2.getAddressName(brokered.getName()), output));
        });


        HashMap<String, String> queryParams = new HashMap<>();
        queryParams.put("address", dest1.getAddress());
        Future<List<Address>> addressesObjects = getAddressesObjects(brokered, Optional.empty(), Optional.of(queryParams));
        List<Address> dest1Response = addressesObjects.get(11, TimeUnit.SECONDS);
        assertEquals(1, dest1Response.size(), String.format("Received unexpected count of addresses! got following addresses %s",
                dest1Response.stream().map(address -> address.getName()).reduce("", String::concat)));

        CRDCmdClient.deleteAddress(environment.namespace(), dest1Response.get(0).getName());
        CRDCmdClient.deleteAddress(environment.namespace(), dest2.getAddressName(brokered.getName()));

        TestUtils.waitUntilCondition(() -> {
            ExecutionResultData allAddresses = CRDCmdClient.getAddress(environment.namespace(), "-a");
            return allAddresses.getStdErr() + allAddresses.getStdOut();
        }, "No resources found.", new TimeoutBudget(30, TimeUnit.SECONDS));
    }


    @Test
    void testAddressCreateViaCmdRemoveViaAgentApi() throws Exception {
        Destination dest1 = new Destination("myqueue1", null, brokered.getName(), "myqueue1",
                Destination.QUEUE, "brokered-queue");
        Destination dest2 = new Destination("myqueue2", null, brokered.getName(), "myqueue2",
                Destination.QUEUE, "brokered-queue");

        JsonObject address1 = dest1.toJson(addressApiClient.getApiVersion());
        String address2 = dest2.toYaml(addressApiClient.getApiVersion());

        ExecutionResultData result = CRDCmdClient.createCR(address1.toString());
        String output = result.getStdOut().trim();

        String addressString = "%s \"%s.%s\" created";
        List<String> dest1Expected = Arrays.asList(
                String.format(addressString, "address", brokered.getName(), dest1.getName()),
                String.format(addressString, "address.enmasse.io", brokered.getName(), dest1.getName()));
        assertTrue(dest1Expected.contains(output),
                String.format("Unexpected response on create custom resource '%s'", address1.toString()));
        assertTrue(result.getRetCode(), String.format("Expected return code 0 on create custom resource '%s'", address1.toString()));

        result = CRDCmdClient.createCR(address2);
        output = result.getStdOut().trim();

        List<String> dest2Expected = Arrays.asList(
                String.format(addressString, "address", brokered.getName(), dest2.getName()),
                String.format(addressString, "address.enmasse.io", brokered.getName(), dest2.getName()));
        assertTrue(dest2Expected.contains(output),
                String.format("Unexpected response on create custom resource '%s'", address2));
        assertTrue(result.getRetCode(), String.format("Expected return code 0 on create custom resource '%s'", address2));

        waitForDestinationsReady(brokered, dest1, dest2);

        result = CRDCmdClient.getAddress(environment.namespace(), "-a");
        output = result.getStdOut().trim();

        assertTrue(output.contains(dest1.getAddressName(brokered.getName())),
                String.format("Get all addresses should contains '%s'; but contains only: %s",
                        dest1.getAddressName(brokered.getName()), output));
        assertTrue(output.contains(dest2.getAddressName(brokered.getName())),
                String.format("Get all addresses should contains '%s'; but contains only: %s",
                        dest2.getAddressName(brokered.getName()), output));

        ConsoleWebPage consoleWeb = new ConsoleWebPage(selenium, getConsoleRoute(brokered), addressApiClient, brokered, new KeycloakCredentials(null, null));
        consoleWeb.openWebConsolePage();
        consoleWeb.openAddressesPageWebConsole();
        consoleWeb.deleteAddressWebConsole(dest1, false);
        deleteAddresses(brokered, dest2);

        TestUtils.waitUntilCondition(() -> {
            ExecutionResultData addresses = CRDCmdClient.getAddress(environment.namespace(), "-a");
            return addresses.getStdOut() + addresses.getStdErr();
        }, "No resources found.", new TimeoutBudget(30, TimeUnit.SECONDS));
    }
}
