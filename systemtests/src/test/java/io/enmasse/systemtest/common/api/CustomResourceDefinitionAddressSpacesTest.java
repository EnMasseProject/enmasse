/*
 * Copyright 2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package io.enmasse.systemtest.common.api;

import io.enmasse.address.model.Address;
import io.enmasse.address.model.AddressSpace;
import io.enmasse.address.model.AuthenticationServiceType;
import io.enmasse.address.model.DoneableAddressSpace;
import io.enmasse.systemtest.*;
import io.enmasse.systemtest.apiclients.AddressApiClient;
import io.enmasse.systemtest.bases.TestBase;
import io.enmasse.systemtest.cmdclients.KubeCMDClient;
import io.enmasse.systemtest.common.Credentials;
import io.enmasse.systemtest.executor.ExecutionResultData;
import io.enmasse.systemtest.resources.CliOutputData;
import io.enmasse.systemtest.utils.AddressSpaceUtils;
import io.enmasse.systemtest.utils.AddressUtils;
import io.enmasse.systemtest.utils.TestUtils;
import io.enmasse.systemtest.utils.UserUtils;
import io.enmasse.user.model.v1.Operation;
import io.enmasse.user.model.v1.User;
import io.enmasse.user.model.v1.UserAuthorizationBuilder;
import io.vertx.core.json.JsonObject;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledIfEnvironmentVariable;

import java.util.Collections;
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
        AddressSpace brokered = AddressSpaceUtils.createAddressSpaceObject("crd-addressspaces-test-foo", AddressSpaceType.BROKERED, AuthenticationServiceType.STANDARD);
        JsonObject addressSpacePayloadJson = AddressSpaceUtils.addressSpaceToJson(brokered);
        createCR(addressSpacePayloadJson.toString());
        waitForAddressSpaceReady(brokered);

        deleteAddressSpace(brokered);
        TestUtils.waitForNamespaceDeleted(kubernetes, brokered.getMetadata().getName());
        TestUtils.waitUntilCondition(() -> {
            ExecutionResultData allAddresses = KubeCMDClient.getAddressSpace(environment.namespace(), "-a");
            return allAddresses.getStdOut() + allAddresses.getStdErr();
        }, "No resources found.", new TimeoutBudget(30, TimeUnit.SECONDS));
    }

    @Test
    void testReplaceAddressSpace() throws Exception {
        AddressSpace standard = AddressSpaceUtils.createAddressSpaceObject("crd-addressspaces-test-foobar", AddressSpaceType.STANDARD, AddressSpacePlans.STANDARD_SMALL, AuthenticationServiceType.STANDARD);
        standard = new DoneableAddressSpace(standard).editSpec().withPlan(AddressSpacePlans.STANDARD_SMALL).endSpec().done();
        createCR(AddressSpaceUtils.addressSpaceToJson(standard).toString());
        addToAddressSpacess(standard);
        waitForAddressSpaceReady(standard);
        waitForAddressSpacePlanApplied(standard);

        standard = new DoneableAddressSpace(standard).editSpec().withPlan(AddressSpacePlans.STANDARD_UNLIMITED).endSpec().done();
        updateCR(AddressSpaceUtils.addressSpaceToJson(standard).toString());
        waitForAddressSpaceReady(standard);

        assertThat(getAddressSpace(standard.getMetadata().getName()).getSpec().getPlan(), is(AddressSpacePlans.STANDARD_UNLIMITED));
    }

    @Test
    void testAddressSpaceCreateViaApiRemoveViaCmd() throws Exception {
        AddressSpace brokered = AddressSpaceUtils.createAddressSpaceObject("crd-addressspaces-test-bar", AddressSpaceType.BROKERED, AuthenticationServiceType.STANDARD);
        createAddressSpace(brokered);

        ExecutionResultData addressSpaces = KubeCMDClient.getAddressSpace(environment.namespace(), brokered.getMetadata().getName());
        String output = addressSpaces.getStdOut();
        assertTrue(output.contains(brokered.getMetadata().getName()),
                String.format("Get all addressspaces should contains '%s'; but contains only: %s",
                        brokered.getMetadata().getName(), output));

        KubeCMDClient.deleteAddressSpace(environment.namespace(), brokered.getMetadata().getName());
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
            AddressSpace brokered = AddressSpaceUtils.createAddressSpaceObject("crd-addressspaces-test-baz", AddressSpaceType.BROKERED, AuthenticationServiceType.STANDARD);
            JsonObject addressSpacePayloadJson = AddressSpaceUtils.addressSpaceToJson(brokered);

            KubeCMDClient.loginUser(user.getUsername(), user.getPassword());
            KubeCMDClient.createNamespace(namespace);
            createCR(namespace, addressSpacePayloadJson.toString());
            waitForAddressSpaceReady(brokered, apiClient);

            deleteAddressSpace(brokered, apiClient);
            TestUtils.waitForNamespaceDeleted(kubernetes, brokered.getMetadata().getName());
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
            AddressSpace brokered = AddressSpaceUtils.createAddressSpaceObject("crd-brokered", AddressSpaceType.BROKERED, AuthenticationServiceType.STANDARD);
            AddressSpace standard = AddressSpaceUtils.createAddressSpaceObject("crd-standard", AddressSpaceType.STANDARD, AddressSpacePlans.STANDARD_SMALL, AuthenticationServiceType.STANDARD);

            KubeCMDClient.loginUser(user.getUsername(), user.getPassword());
            KubeCMDClient.createNamespace(namespace);
            createCR(namespace, AddressSpaceUtils.addressSpaceToJson(brokered).toString());
            createCR(namespace, AddressSpaceUtils.addressSpaceToJson(standard).toString());

            ExecutionResultData result = KubeCMDClient.getAddressSpace(namespace, Optional.of("wide"));
            assertTrue(result.getStdOut().contains(brokered.getMetadata().getName()));
            assertTrue(result.getStdOut().contains(standard.getMetadata().getName()));

            waitForAddressSpaceReady(brokered, apiClient);

            CliOutputData data = new CliOutputData(KubeCMDClient.getAddressSpace(namespace, Optional.of("wide")).getStdOut(),
                    CliOutputData.CliOutputDataType.ADDRESS_SPACE);
            assertTrue(((CliOutputData.AddressSpaceRow) data.getData(brokered.getMetadata().getName())).isReady());
            if (((CliOutputData.AddressSpaceRow) data.getData(brokered.getMetadata().getName())).isReady()) {
                assertThat(((CliOutputData.AddressSpaceRow) data.getData(standard.getMetadata().getName())).getStatus(),
                        containsString(""));
            } else {
                assertThat(((CliOutputData.AddressSpaceRow) data.getData(standard.getMetadata().getName())).getStatus(),
                        containsString("Following deployments and statefulsets are not ready"));
            }

            waitForAddressSpaceReady(standard, apiClient);

            data = new CliOutputData(KubeCMDClient.getAddressSpace(namespace, Optional.of("wide")).getStdOut(),
                    CliOutputData.CliOutputDataType.ADDRESS_SPACE);
            assertTrue(((CliOutputData.AddressSpaceRow) data.getData(brokered.getMetadata().getName())).isReady());
            assertTrue(((CliOutputData.AddressSpaceRow) data.getData(standard.getMetadata().getName())).isReady());
            assertEquals("Active", ((CliOutputData.AddressSpaceRow) data.getData(standard.getMetadata().getName())).getPhase());
            assertTrue(((CliOutputData.AddressSpaceRow) data.getData(standard.getMetadata().getName())).getStatus().isEmpty());


            //===========================
            // User part
            //===========================

            UserCredentials cred = new UserCredentials("pepanatestovani", "pepaNaTestovani");
            User testUser = UserUtils.createUserObject(cred, Collections.singletonList(
                    new UserAuthorizationBuilder()
                            .withAddresses("*")
                            .withOperations(Operation.send, Operation.recv).build()));

            //create user
            assertThat(KubeCMDClient.createCR(namespace, UserUtils.userToJson(brokered.getMetadata().getName(), testUser).toString()).getRetCode(), is(true));
            assertThat(KubeCMDClient.createCR(namespace, UserUtils.userToJson(standard.getMetadata().getName(), testUser).toString()).getRetCode(), is(true));

            data = new CliOutputData(KubeCMDClient.getUser(namespace).getStdOut(),
                    CliOutputData.CliOutputDataType.USER);
            assertEquals(((CliOutputData.UserRow) data.getData(String.format("%s.%s", brokered.getMetadata().getName(),
                    cred.getUsername()))).getUsername(), cred.getUsername());
            assertEquals(data.getData(String.format("%s.%s", standard.getMetadata().getName(),
                    cred.getUsername())).getType(), "password");
            assertEquals(data.getData(String.format("%s.%s", standard.getMetadata().getName(),
                    user.getUsername())).getType(), "federated");

            //===========================
            // Address part
            //===========================

            Address queue = AddressUtils.createAddressObject("queue", null, brokered.getMetadata().getName(), "queue",
                    AddressType.QUEUE.toString(), DestinationPlan.BROKERED_QUEUE);
            Address topicBrokered = AddressUtils.createAddressObject("topic", null, brokered.getMetadata().getName(), "topic",
                    AddressType.TOPIC.toString(), DestinationPlan.BROKERED_TOPIC);
            Address topicStandard = AddressUtils.createAddressObject("topic", null, standard.getMetadata().getName(), "topic",
                    AddressType.TOPIC.toString(), DestinationPlan.STANDARD_LARGE_TOPIC);
            Address anycast = AddressUtils.createAddressObject("anycast", null, standard.getMetadata().getName(), "anycast",
                    AddressType.ANYCAST.toString(), DestinationPlan.STANDARD_SMALL_ANYCAST);

            assertTrue(KubeCMDClient.createCR(namespace, AddressUtils.addressToYaml(queue)).getRetCode());
            assertTrue(KubeCMDClient.createCR(namespace, AddressUtils.addressToYaml(topicBrokered)).getRetCode());
            assertTrue(KubeCMDClient.createCR(namespace, AddressUtils.addressToYaml(topicStandard)).getRetCode());
            assertTrue(KubeCMDClient.createCR(namespace, AddressUtils.addressToYaml(anycast)).getRetCode());

            data = new CliOutputData(KubeCMDClient.getAddress(namespace).getStdOut(),
                    CliOutputData.CliOutputDataType.ADDRESS);

            assertEquals(((CliOutputData.AddressRow) data.getData(String.format("%s.%s", standard.getMetadata().getName(), topicStandard.getSpec().getAddress()))).getPlan(),
                    DestinationPlan.STANDARD_LARGE_TOPIC);

            AddressUtils.waitForDestinationsReady(apiClient, brokered, new TimeoutBudget(5, TimeUnit.MINUTES), queue, topicBrokered);

            data = new CliOutputData(KubeCMDClient.getAddress(namespace).getStdOut(),
                    CliOutputData.CliOutputDataType.ADDRESS);

            assertTrue(((CliOutputData.AddressRow) data.getData(String.format("%s.%s", brokered.getMetadata().getName(), queue.getSpec().getAddress()))).isReady());
            assertEquals(((CliOutputData.AddressRow) data.getData(String.format("%s.%s", standard.getMetadata().getName(), topicStandard.getSpec().getAddress()))).getPlan(),
                    DestinationPlan.STANDARD_LARGE_TOPIC);

            AddressUtils.waitForDestinationsReady(apiClient, standard, new TimeoutBudget(5, TimeUnit.MINUTES), anycast, topicStandard);

            data = new CliOutputData(KubeCMDClient.getAddress(namespace).getStdOut(),
                    CliOutputData.CliOutputDataType.ADDRESS);

            assertTrue(((CliOutputData.AddressRow) data.getData(String.format("%s.%s", brokered.getMetadata().getName(), queue.getSpec().getAddress()))).isReady());
            assertEquals(((CliOutputData.AddressRow) data.getData(String.format("%s.%s", standard.getMetadata().getName(), topicStandard.getSpec().getAddress()))).getPlan(),
                    DestinationPlan.STANDARD_LARGE_TOPIC);
            assertEquals(((CliOutputData.AddressRow) data.getData(String.format("%s.%s", standard.getMetadata().getName(), anycast.getSpec().getAddress()))).getPhase(),
                    "Active");
            assertTrue(((CliOutputData.AddressRow) data.getData(String.format("%s.%s", standard.getMetadata().getName(), topicStandard.getSpec().getAddress()))).getStatus().isEmpty());

            //===========================
            // Clean part
            //===========================

            KubeCMDClient.deleteAddressSpace(namespace, brokered.getMetadata().getName());
            KubeCMDClient.deleteAddressSpace(namespace, standard.getMetadata().getName());

            TestUtils.waitForNamespaceDeleted(kubernetes, brokered.getMetadata().getName());
            TestUtils.waitForNamespaceDeleted(kubernetes, standard.getMetadata().getName());
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
            AddressSpace brokered = AddressSpaceUtils.createAddressSpaceObject("crd-addressspaces-test-barr", AddressSpaceType.BROKERED, AuthenticationServiceType.STANDARD);
            JsonObject addressSpacePayloadJson = AddressSpaceUtils.addressSpaceToJson(brokered);

            KubeCMDClient.loginUser(user.getUsername(), user.getPassword());
            assertThat(KubeCMDClient.createCR(addressSpacePayloadJson.toString()).getRetCode(), is(false));
        } finally {
            KubeCMDClient.loginUser(environment.getApiToken());
            KubeCMDClient.switchProject(environment.namespace());
        }
    }
}
