/*
 * Copyright 2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package io.enmasse.systemtest.common.api;

import io.enmasse.address.model.Address;
import io.enmasse.systemtest.*;
import io.enmasse.systemtest.apiclients.AddressApiClient;
import io.enmasse.systemtest.bases.TestBase;
import io.enmasse.systemtest.cmdclients.KubeCMDClient;
import io.enmasse.systemtest.common.Credentials;
import io.enmasse.systemtest.executor.ExecutionResultData;
import io.enmasse.systemtest.utils.AddressUtils;
import io.enmasse.systemtest.resources.CliOutputData;
import io.enmasse.systemtest.utils.TestUtils;
import io.vertx.core.json.JsonObject;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledIfEnvironmentVariable;

import java.util.Optional;
import java.util.concurrent.TimeUnit;

import static io.enmasse.systemtest.Environment.ocpVersionEnv;
import static io.enmasse.systemtest.TestTag.isolated;
import static io.enmasse.systemtest.cmdclients.KubeCMDClient.createCR;
import static io.enmasse.systemtest.cmdclients.KubeCMDClient.updateCR;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Tag(isolated)
class CustomResourceDefinitionAddressSpacesTest extends TestBase {

    @Test
    void testAddressSpaceCreateViaCmdRemoveViaApi() throws Exception {
        AddressSpace brokered = new AddressSpace("crd-addressspaces-test-foo", AddressSpaceType.BROKERED, AuthService.STANDARD);
        JsonObject addressSpacePayloadJson = brokered.toJson(addressApiClient.getApiVersion());
        createCR(addressSpacePayloadJson.toString());
        waitForAddressSpaceReady(brokered);

        deleteAddressSpace(brokered);
        TestUtils.waitForNamespaceDeleted(kubernetes, brokered.getName());
        TestUtils.waitUntilCondition(() -> {
            ExecutionResultData allAddresses = KubeCMDClient.getAddressSpace(environment.namespace(), "-a");
            return allAddresses.getStdOut() + allAddresses.getStdErr();
        }, "No resources found.", new TimeoutBudget(30, TimeUnit.SECONDS));
    }

    @Test
    void testReplaceAddressSpace() throws Exception {
        AddressSpace standard = new AddressSpace("crd-addressspaces-test-foobar", AddressSpaceType.STANDARD, AuthService.STANDARD);
        standard.setPlan(AddressSpacePlan.STANDARD_SMALL);
        createCR(standard.toJson(addressApiClient.getApiVersion()).toString());
        addToAddressSpacess(standard);
        waitForAddressSpaceReady(standard);
        waitForAddressSpacePlanApplied(standard);

        standard.setPlan(AddressSpacePlan.STANDARD_UNLIMITED);
        updateCR(standard.toJson(addressApiClient.getApiVersion()).toString());
        waitForAddressSpaceReady(standard);

        assertThat(getAddressSpace(standard.getName()).getPlan(), is(AddressSpacePlan.STANDARD_UNLIMITED));
    }

    @Test
    void testAddressSpaceCreateViaApiRemoveViaCmd() throws Exception {
        AddressSpace brokered = new AddressSpace("crd-addressspaces-test-bar", AddressSpaceType.BROKERED, AuthService.STANDARD);
        createAddressSpace(brokered);

        ExecutionResultData addressSpaces = KubeCMDClient.getAddressSpace(environment.namespace(), brokered.getName());
        String output = addressSpaces.getStdOut();
        assertTrue(output.contains(brokered.getName()),
                String.format("Get all addressspaces should contains '%s'; but contains only: %s",
                        brokered.getName(), output));

        KubeCMDClient.deleteAddressSpace(environment.namespace(), brokered.getName());
        TestUtils.waitUntilCondition(() -> {
            ExecutionResultData allAddresses = KubeCMDClient.getAddressSpace(environment.namespace(), "-a");
            return allAddresses.getStdErr();
        }, "No resources found.", new TimeoutBudget(30, TimeUnit.SECONDS));
    }

    @Test
    void testCreateAddressSpaceViaCmdNonAdminUser() throws Exception {
        String namespace = Credentials.namespace();
        UserCredentials user = Credentials.userCredentials();
        try {
            AddressApiClient apiClient = new AddressApiClient(kubernetes, namespace);
            AddressSpace brokered = new AddressSpace("crd-addressspaces-test-baz", AddressSpaceType.BROKERED, AuthService.STANDARD);
            JsonObject addressSpacePayloadJson = brokered.toJson(addressApiClient.getApiVersion());

            KubeCMDClient.loginUser(user.getUsername(), user.getPassword());
            KubeCMDClient.createNamespace(namespace);
            createCR(namespace, addressSpacePayloadJson.toString());
            waitForAddressSpaceReady(brokered, apiClient);

            deleteAddressSpace(brokered, apiClient);
            TestUtils.waitForNamespaceDeleted(kubernetes, brokered.getName());
            TestUtils.waitUntilCondition(() -> {
                ExecutionResultData allAddresses = KubeCMDClient.getAddressSpace(namespace, Optional.empty());
                return allAddresses.getStdOut() + allAddresses.getStdErr();
            }, "No resources found.", new TimeoutBudget(30, TimeUnit.SECONDS));
        } finally {
            KubeCMDClient.loginUser(environment.getApiToken());
            KubeCMDClient.switchProject(environment.namespace());
            kubernetes.deleteNamespace(namespace);
        }
    }

    @Test
    @DisabledIfEnvironmentVariable(named = ocpVersionEnv, matches = "3.10")
    void testCliOutput() throws Exception {
        String namespace = "cli-output";
        UserCredentials user = new UserCredentials("pepan", "pepan");
        try {
            //===========================
            // AddressSpace part
            //===========================
            AddressApiClient apiClient = new AddressApiClient(kubernetes, namespace);
            AddressSpace brokered = new AddressSpace("crd-brokered", AddressSpaceType.BROKERED, AuthService.STANDARD);
            AddressSpace standard = new AddressSpace("crd-standard", AddressSpaceType.STANDARD, AuthService.STANDARD);

            KubeCMDClient.loginUser(user.getUsername(), user.getPassword());
            KubeCMDClient.createNamespace(namespace);
            createCR(namespace, brokered.toJson(apiClient.getApiVersion()).toString());
            createCR(namespace, standard.toJson(apiClient.getApiVersion()).toString());

            ExecutionResultData result = KubeCMDClient.getAddressSpace(namespace, Optional.of("wide"));
            assertTrue(result.getStdOut().contains(brokered.getName()));
            assertTrue(result.getStdOut().contains(standard.getName()));

            waitForAddressSpaceReady(brokered, apiClient);

            CliOutputData data = new CliOutputData(KubeCMDClient.getAddressSpace(namespace, Optional.of("wide")).getStdOut(),
                    CliOutputData.CliOutputDataType.ADDRESS_SPACE);
            assertTrue(((CliOutputData.AddressSpaceRow) data.getData(brokered.getName())).isReady());
            if (((CliOutputData.AddressSpaceRow) data.getData(brokered.getName())).isReady()) {
                assertThat(((CliOutputData.AddressSpaceRow) data.getData(standard.getName())).getStatus(),
                        containsString(""));
            } else {
                assertThat(((CliOutputData.AddressSpaceRow) data.getData(standard.getName())).getStatus(),
                        containsString("Following deployments and statefulsets are not ready"));
            }

            waitForAddressSpaceReady(standard, apiClient);

            data = new CliOutputData(KubeCMDClient.getAddressSpace(namespace, Optional.of("wide")).getStdOut(),
                    CliOutputData.CliOutputDataType.ADDRESS_SPACE);
            assertTrue(((CliOutputData.AddressSpaceRow) data.getData(brokered.getName())).isReady());
            assertTrue(((CliOutputData.AddressSpaceRow) data.getData(standard.getName())).isReady());
            assertTrue(((CliOutputData.AddressSpaceRow) data.getData(standard.getName()))
                    .getStatus().isEmpty());


            //===========================
            // User part
            //===========================

            UserCredentials cred = new UserCredentials("pepanatestovani", "pepaNaTestovani");
            User testUser = new User().setUserCredentials(cred).addAuthorization(
                    new User.AuthorizationRule()
                            .addAddress("*")
                            .addOperation(User.Operation.SEND)
                            .addOperation(User.Operation.RECEIVE));

            //create user
            assertThat(KubeCMDClient.createCR(namespace, testUser.toCRDJson(brokered.getName()).toString()).getRetCode(), is(true));
            assertThat(KubeCMDClient.createCR(namespace, testUser.toCRDJson(standard.getName()).toString()).getRetCode(), is(true));

            data = new CliOutputData(KubeCMDClient.getUser(namespace).getStdOut(),
                    CliOutputData.CliOutputDataType.USER);
            assertEquals(((CliOutputData.UserRow) data.getData(String.format("%s.%s", brokered.getName(),
                    cred.getUsername()))).getUsername(), cred.getUsername());
            assertEquals(data.getData(String.format("%s.%s", standard.getName(),
                    cred.getUsername())).getType(), "password");
            assertEquals(data.getData(String.format("%s.%s", standard.getName(),
                    user.getUsername())).getType(), "federated");

            //===========================
            // Address part
            //===========================

            Address queue = AddressUtils.createAddress("queue", null, brokered.getName(), "queue",
                    AddressType.QUEUE.toString(), DestinationPlan.BROKERED_QUEUE);
            Address topicBrokered = AddressUtils.createAddress("topic", null, brokered.getName(), "topic",
                    AddressType.TOPIC.toString(), DestinationPlan.BROKERED_TOPIC);
            Address topicStandard = AddressUtils.createAddress("topic", null, standard.getName(), "topic",
                    AddressType.TOPIC.toString(), DestinationPlan.STANDARD_LARGE_TOPIC);
            Address anycast = AddressUtils.createAddress("anycast", null, standard.getName(), "anycast",
                    AddressType.ANYCAST.toString(), DestinationPlan.STANDARD_SMALL_ANYCAST);

            assertTrue(KubeCMDClient.createCR(namespace, AddressUtils.addressToYaml(queue)).getRetCode());
            assertTrue(KubeCMDClient.createCR(namespace, AddressUtils.addressToYaml(topicBrokered)).getRetCode());
            assertTrue(KubeCMDClient.createCR(namespace, AddressUtils.addressToYaml(topicStandard)).getRetCode());
            assertTrue(KubeCMDClient.createCR(namespace, AddressUtils.addressToYaml(anycast)).getRetCode());

            data = new CliOutputData(KubeCMDClient.getAddress(namespace).getStdOut(),
                    CliOutputData.CliOutputDataType.ADDRESS);

            assertEquals(((CliOutputData.AddressRow) data.getData(String.format("%s.%s", standard.getName(), topicStandard.getSpec().getAddress()))).getPlan(),
                    DestinationPlan.STANDARD_LARGE_TOPIC);

            TestUtils.waitForDestinationsReady(apiClient, brokered, new TimeoutBudget(5, TimeUnit.MINUTES), queue, topicBrokered);

            data = new CliOutputData(KubeCMDClient.getAddress(namespace).getStdOut(),
                    CliOutputData.CliOutputDataType.ADDRESS);

            assertTrue(((CliOutputData.AddressRow) data.getData(String.format("%s.%s", brokered.getName(), queue.getSpec().getAddress()))).isReady());
            assertEquals(((CliOutputData.AddressRow) data.getData(String.format("%s.%s", standard.getName(), topicStandard.getSpec().getAddress()))).getPlan(),
                    DestinationPlan.STANDARD_LARGE_TOPIC);

            TestUtils.waitForDestinationsReady(apiClient, standard, new TimeoutBudget(5, TimeUnit.MINUTES), anycast, topicStandard);

            data = new CliOutputData(KubeCMDClient.getAddress(namespace).getStdOut(),
                    CliOutputData.CliOutputDataType.ADDRESS);

            assertTrue(((CliOutputData.AddressRow) data.getData(String.format("%s.%s", brokered.getName(), queue.getSpec().getAddress()))).isReady());
            assertEquals(((CliOutputData.AddressRow) data.getData(String.format("%s.%s", standard.getName(), topicStandard.getSpec().getAddress()))).getPlan(),
                    DestinationPlan.STANDARD_LARGE_TOPIC);
            assertEquals(((CliOutputData.AddressRow) data.getData(String.format("%s.%s", standard.getName(), anycast.getSpec().getAddress()))).getPhase(),
                    "Active");
            assertTrue(((CliOutputData.AddressRow) data.getData(String.format("%s.%s", standard.getName(), topicStandard.getSpec().getAddress()))).getStatus().isEmpty());

            //===========================
            // Clean part
            //===========================

            KubeCMDClient.deleteAddressSpace(namespace, brokered.getName());
            KubeCMDClient.deleteAddressSpace(namespace, standard.getName());

            TestUtils.waitForNamespaceDeleted(kubernetes, brokered.getName());
            TestUtils.waitForNamespaceDeleted(kubernetes, standard.getName());
            TestUtils.waitUntilCondition(() -> {
                ExecutionResultData allAddresses = KubeCMDClient.getAddressSpace(namespace, Optional.empty());
                return allAddresses.getStdOut() + allAddresses.getStdErr();
            }, "No resources found.", new TimeoutBudget(30, TimeUnit.SECONDS));
        } finally {
            KubeCMDClient.loginUser(environment.getApiToken());
            KubeCMDClient.switchProject(environment.namespace());
            kubernetes.deleteNamespace(namespace);
        }
    }

    @Test
    void testCannotCreateAddressSpaceViaCmdNonAdminUser() throws Exception {
        UserCredentials user = Credentials.userCredentials();
        try {
            AddressSpace brokered = new AddressSpace("crd-addressspaces-test-barr", AddressSpaceType.BROKERED, AuthService.STANDARD);
            JsonObject addressSpacePayloadJson = brokered.toJson(addressApiClient.getApiVersion());

            KubeCMDClient.loginUser(user.getUsername(), user.getPassword());
            assertThat(KubeCMDClient.createCR(addressSpacePayloadJson.toString()).getRetCode(), is(false));
        } finally {
            KubeCMDClient.loginUser(environment.getApiToken());
            KubeCMDClient.switchProject(environment.namespace());
        }
    }
}
