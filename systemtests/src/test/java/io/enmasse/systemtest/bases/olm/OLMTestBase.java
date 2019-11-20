/*
 * Copyright 2019, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.systemtest.bases.olm;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import org.apache.qpid.proton.message.Message;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.slf4j.Logger;

import io.enmasse.address.model.Address;
import io.enmasse.address.model.AddressSpace;
import io.enmasse.admin.model.v1.AuthenticationService;
import io.enmasse.systemtest.amqp.AmqpClient;
import io.enmasse.systemtest.amqp.AmqpConnectOptions;
import io.enmasse.systemtest.amqp.QueueTerminusFactory;
import io.enmasse.systemtest.bases.TestBase;
import io.enmasse.systemtest.bases.isolated.ITestIsolatedStandard;
import io.enmasse.systemtest.executor.ExecutionResultData;
import io.enmasse.systemtest.logs.CustomLogger;
import io.enmasse.systemtest.platform.KubeCMDClient;
import io.enmasse.systemtest.platform.cluster.CRCCluster;
import io.enmasse.systemtest.time.TimeoutBudget;
import io.enmasse.systemtest.utils.AddressSpaceUtils;
import io.enmasse.systemtest.utils.AuthServiceUtils;
import io.enmasse.systemtest.utils.TestUtils;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.proton.ProtonQoS;

public abstract class OLMTestBase extends TestBase implements ITestIsolatedStandard {
    private static Logger log = CustomLogger.getLogger();

    private static final int CR_TIMEOUT_MILLIS = 30000;
    private List<JsonObject> exampleResources = new ArrayList<>();

    protected abstract String getInstallationNamespace();

    @AfterEach
    void collectLogs(ExtensionContext context) {
        if (context.getExecutionException().isPresent()) { //test failed
            logCollector.collectLogsOfPodsInNamespace(getInstallationNamespace());
            logCollector.collectEvents(getInstallationNamespace());
        }
    }

    @BeforeEach
    void setupExampleResources() {
        ExecutionResultData result = KubeCMDClient.runOnCluster("get", "csv", "-n", getInstallationNamespace(), "-o", "json", "-l", "app=enmasse");
        JsonObject csvList = new JsonObject(result.getStdOut());
        JsonObject csv = csvList.getJsonArray("items").getJsonObject(0);
        String almExamples = csv.getJsonObject("metadata").getJsonObject("annotations").getString("alm-examples");
        JsonArray examples = new JsonArray(almExamples);
        exampleResources = examples.stream().map(o -> (JsonObject) o).collect(Collectors.toList());
    }

    @AfterEach
    void teardownExampleResources() throws IOException {
        if (!environment.skipCleanup()) {
            for (JsonObject example : exampleResources) {
                log.info("Deleting {}", example.toString());
                KubeCMDClient.deleteCR(getInstallationNamespace(), example.toString(), CR_TIMEOUT_MILLIS);
            }
        }
    }

    protected void doTestCreateExampleResources() throws Exception {
        Set<String> infraKinds = Set.of("StandardInfraConfig", "BrokeredInfraConfig", "AddressPlan", "AddressSpacePlan", "AuthenticationService");

        for(JsonObject example : exampleResources) {
            LOGGER.info("Example: {}", example);
            String kind = example.getString("kind");
            if(kind.equals("AuthenticationService") && kubernetes.getCluster() instanceof CRCCluster) {
                log.info("Creating standard-authservice with no persistence because of CRC cluster");
                AuthenticationService authService = AuthServiceUtils.createStandardAuthServiceObject("standard-authservice", false);
                authService.getMetadata().setNamespace(getInstallationNamespace());
                kubernetes.getAuthenticationServiceClient(getInstallationNamespace()).create(authService);
            } else if(infraKinds.contains(kind)) {
                log.info("Creating {}", example.toString());
                ExecutionResultData res = KubeCMDClient.createCR(getInstallationNamespace(), example.toString(), CR_TIMEOUT_MILLIS);
                if(!res.getRetCode()) {
                    Assertions.fail(res.getStdErr());
                }
            }
        }
        TestUtils.waitUntilDeployed(getInstallationNamespace());
        TestUtils.waitForPodReady("standard-authservice", getInstallationNamespace());
        TestUtils.waitForSchemaInSync("standard-small");

        var addressSpacePlanClient = kubernetes.getAddressSpacePlanClient(getInstallationNamespace());
        TestUtils.waitUntilCondition("AddressSpacePlan standard-small visible",
                phase -> addressSpacePlanClient.withName("standard-small").get() != null,
                new TimeoutBudget(2, TimeUnit.MINUTES));
        for(JsonObject example : exampleResources) {
            String kind = example.getString("kind");
            if(kind.equals("AddressSpace")) {
                log.info("Creating {}", kind);
                ExecutionResultData res = KubeCMDClient.createCR(getInstallationNamespace(), example.toString(), CR_TIMEOUT_MILLIS);
                if(!res.getRetCode()) {
                    Assertions.fail(res.getStdErr());
                }
            }
        }
        var client = kubernetes.getAddressSpaceClient(getInstallationNamespace());
        TestUtils.waitUntilCondition("Address space visible",
                phase -> client.withName("myspace").get() != null,
                new TimeoutBudget(30, TimeUnit.SECONDS));
        resourcesManager.waitForAddressSpaceReady(client.withName("myspace").get());
        for(JsonObject example : exampleResources) {
            String kind = example.getString("kind");
            if(kind.equals("Address") || kind.equals("MessagingUser")) {
                log.info("Creating {}", kind);
                ExecutionResultData res = KubeCMDClient.createCR(getInstallationNamespace(), example.toString(), CR_TIMEOUT_MILLIS);
                if(!res.getRetCode()) {
                    Assertions.fail(res.getStdErr());
                }
            }
        }
        Thread.sleep(10_000);
        TestUtils.waitUntilDeployed(getInstallationNamespace());
        var addressClient = kubernetes.getAddressClient(getInstallationNamespace());
        TestUtils.waitUntilCondition("Address visible",
                phase -> addressClient.withName("myspace.myqueue").get() != null,
                new TimeoutBudget(30, TimeUnit.SECONDS));
        waitForDestinationsReady(addressClient.withName("myspace.myqueue").get());

        // Test basic messages
        AddressSpace exampleSpace = kubernetes.getAddressSpaceClient(getInstallationNamespace()).withName("myspace").get();
        Address exampleAddress = kubernetes.getAddressClient(getInstallationNamespace()).withName("myspace.myqueue").get();

        AmqpClient amqpClient = resourcesManager.getAmqpClientFactory().createClient(new AmqpConnectOptions()
                .setTerminusFactory(new QueueTerminusFactory())
                .setQos(ProtonQoS.AT_LEAST_ONCE)
                .setCert(new String(Base64.getDecoder().decode(exampleSpace.getStatus().getCaCert()), StandardCharsets.UTF_8))
                .setEndpoint(kubernetes.getExternalEndpoint("messaging-" + AddressSpaceUtils.getAddressSpaceInfraUuid(exampleSpace), getInstallationNamespace()))
                .setUsername("user")
                .setPassword("enmasse"));

        int messageCount = 10;
        Future<Integer> sent = amqpClient.sendMessages(exampleAddress.getSpec().getAddress(), TestUtils.generateMessages(messageCount));
        assertEquals(messageCount, sent.get(1, TimeUnit.MINUTES).intValue(), "Incorrect count of messages send");

        Future<List<Message>> received = amqpClient.recvMessages(exampleAddress.getSpec().getAddress(), messageCount);
        assertEquals(messageCount, received.get(1, TimeUnit.MINUTES).size(), "Incorrect count of messages received");
    }
}
