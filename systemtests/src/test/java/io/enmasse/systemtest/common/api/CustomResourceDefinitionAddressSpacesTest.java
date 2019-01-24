/*
 * Copyright 2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package io.enmasse.systemtest.common.api;

import io.enmasse.systemtest.*;
import io.enmasse.systemtest.apiclients.AddressApiClient;
import io.enmasse.systemtest.bases.TestBase;
import io.enmasse.systemtest.cmdclients.KubeCMDClient;
import io.enmasse.systemtest.executor.ExecutionResultData;
import io.enmasse.systemtest.resources.CliOutputData;
import io.vertx.core.json.JsonObject;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledIfEnvironmentVariable;
import org.slf4j.Logger;

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
    private static Logger log = CustomLogger.getLogger();

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
        standard.setPlan("standard-small");
        createCR(standard.toJson(addressApiClient.getApiVersion()).toString());
        addToAddressSpacess(standard);
        waitForAddressSpaceReady(standard);
        waitForAddressSpacePlanApplied(standard);

        standard.setPlan("standard-unlimited");
        updateCR(standard.toJson(addressApiClient.getApiVersion()).toString());
        waitForAddressSpaceReady(standard);

        assertThat(getAddressSpace(standard.getName()).getPlan(), is("standard-unlimited"));
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
        String namespace = "pepik";
        UserCredentials user = new UserCredentials("pepik", "pepik");
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

            Destination queue = new Destination("queue", null, brokered.getName(), "queue",
                    Destination.QUEUE, DestinationPlan.BROKERED_QUEUE.plan());
            Destination topicBrokered = new Destination("topic", null, brokered.getName(), "topic",
                    Destination.TOPIC, DestinationPlan.BROKERED_TOPIC.plan());
            Destination topicStandard = new Destination("topic", null, standard.getName(), "topic",
                    Destination.TOPIC, DestinationPlan.STANDARD_LARGE_TOPIC.plan());
            Destination anycast = new Destination("anycast", null, standard.getName(), "anycast",
                    Destination.ANYCAST, DestinationPlan.STANDARD_SMALL_ANYCAST.plan());

            assertTrue(KubeCMDClient.createCR(namespace, queue.toYaml(addressApiClient.getApiVersion())).getRetCode());
            assertTrue(KubeCMDClient.createCR(namespace, topicBrokered.toYaml(addressApiClient.getApiVersion())).getRetCode());
            assertTrue(KubeCMDClient.createCR(namespace, topicStandard.toYaml(addressApiClient.getApiVersion())).getRetCode());
            assertTrue(KubeCMDClient.createCR(namespace, anycast.toYaml(addressApiClient.getApiVersion())).getRetCode());

            data = new CliOutputData(KubeCMDClient.getAddress(namespace).getStdOut(),
                    CliOutputData.CliOutputDataType.ADDRESS);

            assertEquals(((CliOutputData.AddressRow) data.getData(String.format("%s.%s", standard.getName(), topicStandard.getAddress()))).getPlan(),
                    DestinationPlan.STANDARD_LARGE_TOPIC.plan());

            TestUtils.waitForDestinationsReady(apiClient, brokered, new TimeoutBudget(5, TimeUnit.MINUTES), queue, topicBrokered);

            data = new CliOutputData(KubeCMDClient.getAddress(namespace).getStdOut(),
                    CliOutputData.CliOutputDataType.ADDRESS);

            assertTrue(((CliOutputData.AddressRow) data.getData(String.format("%s.%s", brokered.getName(), queue.getAddress()))).isReady());
            assertEquals(((CliOutputData.AddressRow) data.getData(String.format("%s.%s", standard.getName(), topicStandard.getAddress()))).getPlan(),
                    DestinationPlan.STANDARD_LARGE_TOPIC.plan());

            TestUtils.waitForDestinationsReady(apiClient, standard, new TimeoutBudget(5, TimeUnit.MINUTES), anycast, topicStandard);

            data = new CliOutputData(KubeCMDClient.getAddress(namespace).getStdOut(),
                    CliOutputData.CliOutputDataType.ADDRESS);

            assertTrue(((CliOutputData.AddressRow) data.getData(String.format("%s.%s", brokered.getName(), queue.getAddress()))).isReady());
            assertEquals(((CliOutputData.AddressRow) data.getData(String.format("%s.%s", standard.getName(), topicStandard.getAddress()))).getPlan(),
                    DestinationPlan.STANDARD_LARGE_TOPIC.plan());
            assertEquals(((CliOutputData.AddressRow) data.getData(String.format("%s.%s", standard.getName(), anycast.getAddress()))).getPhase(),
                    "Active");
            assertTrue(((CliOutputData.AddressRow) data.getData(String.format("%s.%s", standard.getName(), topicStandard.getAddress()))).getStatus().isEmpty());

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
        UserCredentials user = new UserCredentials("pepik", "pepik");
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
