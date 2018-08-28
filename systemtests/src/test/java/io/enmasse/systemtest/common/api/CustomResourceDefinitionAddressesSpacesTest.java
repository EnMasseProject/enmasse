/*
 * Copyright 2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package io.enmasse.systemtest.common.api;

import io.enmasse.systemtest.*;
import io.enmasse.systemtest.apiclients.AddressApiClient;
import io.enmasse.systemtest.bases.TestBase;
import io.enmasse.systemtest.cmdclients.CRDCmdClient;
import io.enmasse.systemtest.executor.ExecutionResultData;
import io.vertx.core.json.JsonObject;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.util.concurrent.TimeUnit;

import static io.enmasse.systemtest.TestTag.isolated;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Tag(isolated)
public class CustomResourceDefinitionAddressesSpacesTest extends TestBase {

    @Test
    void testAddressSpaceCreateViaCmdRemoveViaApi() throws Exception {
        AddressSpace brokered = new AddressSpace("crd-addressspaces-test-foo", AddressSpaceType.BROKERED, AuthService.NONE);
        JsonObject addressSpacePayloadJson = brokered.toJson(addressApiClient.getApiVersion());
        CRDCmdClient.createCR(addressSpacePayloadJson.toString());
        waitForAddressSpaceReady(brokered);

        deleteAddressSpace(brokered);
        TestUtils.waitForNamespaceDeleted(kubernetes, brokered.getName());
        TestUtils.waitUntilCondition(() -> {
            ExecutionResultData allAddresses = CRDCmdClient.getAddressSpace(environment.namespace(), "-a");
            return allAddresses.getStdOut() + allAddresses.getStdErr();
        }, "No resources found.", new TimeoutBudget(30, TimeUnit.SECONDS));
    }

    @Test
    void testAddressSpaceCreateViaApiRemoveViaCmd() throws Exception {
        AddressSpace brokered = new AddressSpace("crd-addressspaces-test-bar", AddressSpaceType.BROKERED, AuthService.NONE);
        createAddressSpace(brokered);

        ExecutionResultData addressSpaces = CRDCmdClient.getAddressSpace(environment.namespace(), brokered.getName());
        String output = addressSpaces.getStdOut();
        assertTrue(output.contains(brokered.getName()),
                String.format("Get all addressspaces should contains '%s'; but contains only: %s",
                        brokered.getName(), output));

        CRDCmdClient.deleteAddressSpace(environment.namespace(), brokered.getName());
        TestUtils.waitUntilCondition(() -> {
            ExecutionResultData allAddresses = CRDCmdClient.getAddressSpace(environment.namespace(), "-a");
            return allAddresses.getStdErr();
        }, "No resources found.", new TimeoutBudget(30, TimeUnit.SECONDS));
    }

    @Test
    void testCreateAddressSpaceViaCmdNonAdminUser() throws Exception {
        String namespace = "pepik";
        KeycloakCredentials user = new KeycloakCredentials("pepik", "pepik");
        try {
            AddressApiClient apiClient = new AddressApiClient(kubernetes, namespace);
            AddressSpace brokered = new AddressSpace("crd-addressspaces-test-baz", AddressSpaceType.BROKERED, AuthService.NONE);
            JsonObject addressSpacePayloadJson = brokered.toJson(addressApiClient.getApiVersion());

            CRDCmdClient.loginUser(user.getUsername(), user.getPassword());
            CRDCmdClient.createNamespace(namespace);
            CRDCmdClient.createCR(namespace, addressSpacePayloadJson.toString());
            waitForAddressSpaceReady(brokered, apiClient);

            deleteAddressSpace(brokered, apiClient);
            TestUtils.waitForNamespaceDeleted(kubernetes, brokered.getName());
            TestUtils.waitUntilCondition(() -> {
                ExecutionResultData allAddresses = CRDCmdClient.getAddressSpace(namespace, "-a");
                return allAddresses.getStdOut() + allAddresses.getStdErr();
            }, "No resources found.", new TimeoutBudget(30, TimeUnit.SECONDS));
        } finally {
            CRDCmdClient.loginUser("developer", "developer");
            CRDCmdClient.switchProject(environment.namespace());
            kubernetes.deleteNamespace(namespace);
        }
    }
}
