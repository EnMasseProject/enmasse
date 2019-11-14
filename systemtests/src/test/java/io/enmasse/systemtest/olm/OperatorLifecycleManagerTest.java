/*
 * Copyright 2019, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.systemtest.olm;

import io.enmasse.address.model.Address;
import io.enmasse.address.model.AddressSpace;
import io.enmasse.systemtest.UserCredentials;
import io.enmasse.systemtest.bases.TestBase;
import io.enmasse.systemtest.bases.isolated.ITestIsolatedStandard;
import io.enmasse.systemtest.condition.Kubernetes;
import io.enmasse.systemtest.condition.OpenShift;
import io.enmasse.systemtest.executor.Exec;
import io.enmasse.systemtest.executor.ExecutionResultData;
import io.enmasse.systemtest.logs.CustomLogger;
import io.enmasse.systemtest.operator.OperatorManager;
import io.enmasse.systemtest.platform.KubeCMDClient;
import io.enmasse.systemtest.time.TimeoutBudget;
import io.enmasse.systemtest.utils.TestUtils;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.slf4j.Logger;

import java.io.File;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import static io.enmasse.systemtest.TestTag.OLM;

@Tag(OLM)
class OperatorLifecycleManagerTest extends TestBase implements ITestIsolatedStandard {
    private static Logger log = CustomLogger.getLogger();
    private final String infraNamespace = kubernetes.getOlmNamespace();

    private static final int CREATE_CR_TIMEOUT_MILLIS = 30000;

    @AfterEach
    void collectLogs(ExtensionContext context) {
        if (context.getExecutionException().isPresent()) { //test failed
            logCollector.collectLogsOfPodsInNamespace(infraNamespace);
            logCollector.collectEvents(infraNamespace);
        }
    }

    @Test
    void testCreateExampleResources() throws Exception {
        Predicate<String> isInfraCR = kind -> kind.equals("StandardInfraConfig") || kind.equals("BrokeredInfraConfig") || kind.equals("AddressPlan") || kind.equals("AddressSpacePlan") || kind.equals("AuthenticationService");
        ExecutionResultData result = KubeCMDClient.runOnCluster("get", "csv", "-n", infraNamespace, "-o", "json", "-l", "app=enmasse");
        JsonObject csvList = new JsonObject(result.getStdOut());
        JsonObject csv = csvList.getJsonArray("items").getJsonObject(0);
        String almExamples = csv.getJsonObject("metadata").getJsonObject("annotations").getString("alm-examples");
        JsonArray examples = new JsonArray(almExamples);
        List<JsonObject> exampleResources = examples.stream().map(o->(JsonObject)o).collect(Collectors.toList());
        for(JsonObject example : exampleResources) {
            String kind = example.getString("kind");
            if(isInfraCR.test(kind)) {
                log.info("Creating {}", example.toString());
                ExecutionResultData res = KubeCMDClient.createCR(infraNamespace, example.toString(), CREATE_CR_TIMEOUT_MILLIS);
                if(!res.getRetCode()) {
                    Assertions.fail(res.getStdErr());
                }
            }
        }
        TestUtils.waitUntilDeployed(infraNamespace);
        TestUtils.waitForSchemaInSync("standard-small");

        var addressSpacePlanClient = kubernetes.getAddressSpacePlanClient(infraNamespace);
        TestUtils.waitUntilCondition("AddressSpacePlan standard-small visible",
                phase -> addressSpacePlanClient.withName("standard-small").get() != null,
                new TimeoutBudget(2, TimeUnit.MINUTES));
        for(JsonObject example : exampleResources) {
            String kind = example.getString("kind");
            if(kind.equals("AddressSpace")) {
                log.info("Creating {}", kind);
                ExecutionResultData res = KubeCMDClient.createCR(infraNamespace, example.toString(), CREATE_CR_TIMEOUT_MILLIS);
                if(!res.getRetCode()) {
                    Assertions.fail(res.getStdErr());
                }
            }
        }
        var client = kubernetes.getAddressSpaceClient(infraNamespace);
        TestUtils.waitUntilCondition("Address space visible",
                phase -> client.withName("myspace").get() != null,
                new TimeoutBudget(30, TimeUnit.SECONDS));
        resourcesManager.waitForAddressSpaceReady(client.withName("myspace").get());
        for(JsonObject example : exampleResources) {
            String kind = example.getString("kind");
            if(kind.equals("Address") || kind.equals("MessagingUser")) {
                log.info("Creating {}", kind);
                ExecutionResultData res = KubeCMDClient.createCR(infraNamespace, example.toString(), CREATE_CR_TIMEOUT_MILLIS);
                if(!res.getRetCode()) {
                    Assertions.fail(res.getStdErr());
                }
            }
        }
        Thread.sleep(10_000);
        TestUtils.waitUntilDeployed(infraNamespace);
        var addressClient = kubernetes.getAddressClient(infraNamespace);
        TestUtils.waitUntilCondition("Address visible",
                phase -> addressClient.withName("myspace.myqueue").get() != null,
                new TimeoutBudget(30, TimeUnit.SECONDS));
        waitForDestinationsReady(addressClient.withName("myspace.myqueue").get());

        // Test basic messages
        AddressSpace exampleSpace = kubernetes.getAddressSpaceClient(infraNamespace).withName("myspace").get();
        Address exampleAddress = kubernetes.getAddressClient(infraNamespace).withName("myspace.myqueue").get();
        getClientUtils().assertCanConnect(exampleSpace, new UserCredentials("user", "enmasse"), Collections.singletonList(exampleAddress), resourcesManager);
    }
}