/*
 * Copyright 2019, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.systemtest.olm;

import io.enmasse.address.model.Address;
import io.enmasse.address.model.AddressSpace;
import io.enmasse.systemtest.Environment;
import io.enmasse.systemtest.UserCredentials;
import io.enmasse.systemtest.bases.TestBase;
import io.enmasse.systemtest.bases.isolated.ITestIsolatedStandard;
import io.enmasse.systemtest.condition.OpenShift;
import io.enmasse.systemtest.executor.Exec;
import io.enmasse.systemtest.executor.ExecutionResultData;
import io.enmasse.systemtest.logs.CustomLogger;
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

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@Tag(OLM)
@OpenShift(version = 4)
class OperatorLifecycleManagerTest extends TestBase implements ITestIsolatedStandard {
    private static Logger log = CustomLogger.getLogger();
    private final String infraNamespace = "openshift-operators";

    private static final int CREATE_CR_TIMEOUT_MILLIS = 30000;

    //amqonline.1.3.1
    private static String csvName;

    //amq-online-operator
    private static String operatorSource;
    //amq-online
    private static String operatorName;

    private static String subscriptionName = "systemtests-enmasse-operator";

    @BeforeAll
    void prepare() throws Exception {
        String productName = Environment.getInstance().isDownstream() ? "amq-online" : "enmasse";
        operatorName = productName;
        operatorSource = String.format("%s-operator", productName);
        ExecutionResultData res = KubeCMDClient.runOnCluster("get", "packagemanifests", "-n", "openshift-marketplace", "-l", String.format("catalog=%s", operatorSource), "-o", "json");
        if(!res.getRetCode()) {
            Assertions.fail(res.getStdErr());
        }
        JsonObject manifests = new JsonObject(res.getStdOut());
        JsonObject productManifest = manifests.getJsonArray("items")
                .stream()
                .map(o->(JsonObject)o)
                .filter(o->o.getJsonObject("metadata").getString("name").equals(productName))
                .findFirst()
                .orElseThrow();
        String productCSV = productManifest.getJsonObject("status")
                .getJsonArray("channels")
                .stream()
                .map(o->(JsonObject)o)
                .findFirst()
                .map(o->o.getString("currentCSV"))
                .orElseThrow();
        csvName = productCSV;

        log.info("Using CSV name {}", csvName);
        log.info("Using operator source {}", operatorSource);
        log.info("Using operator name {}", operatorName);
    }

    @AfterAll
    void cleanRestOfResources() {
        Exec.execute(Arrays.asList("oc", "delete", "all", "--selector", "app=enmasse"), 120_000, false);
        Exec.execute(Arrays.asList("oc", "delete", "crd", "-l", "app=enmasse"), 120_000, false);
        Exec.execute(Arrays.asList("oc", "delete", "apiservices", "-l", "app=enmasse"), 120_000, false);
        Exec.execute(Arrays.asList("oc", "delete", "cm", "-l", "app=enmasse"), 120_000, false);
        Exec.execute(Arrays.asList("oc", "delete", "secret", "-l", "app=enmasse"), 120_000, false);
    }

    @AfterEach
    void collectLogs(ExtensionContext context) {
        if (context.getExecutionException().isPresent()) { //test failed
            logCollector.collectLogsOfPodsInNamespace(infraNamespace);
            logCollector.collectEvents(infraNamespace);
        }
    }

    @Test
    @Order(1)
    void installOperator() throws Exception {
        JsonObject subscription = new JsonObject(Files.readString(new File("src/main/resources/operator-subscription.json").toPath()));
        subscription.getJsonObject("metadata").put("name", subscriptionName);
        subscription.getJsonObject("metadata").put("namespace", infraNamespace);
        subscription.getJsonObject("spec").put("startingCSV", csvName);
        subscription.getJsonObject("spec").put("name", operatorName);
        subscription.getJsonObject("spec").put("source", operatorSource);
        log.info("Creating {}", subscription.toString());
        ExecutionResultData res = KubeCMDClient.createCR(infraNamespace, subscription.encode(), CREATE_CR_TIMEOUT_MILLIS);
        if(!res.getRetCode()) {
            Assertions.fail(res.getStdErr());
        }
        Thread.sleep(30_000);
        TestUtils.waitUntilDeployed(infraNamespace);
    }

    @Test
    @Order(2)
    void testCreateExampleResources() throws Exception {
        Predicate<String> isInfraCR = kind -> kind.equals("StandardInfraConfig") || kind.equals("BrokeredInfraConfig") || kind.equals("AddressPlan") || kind.equals("AddressSpacePlan") || kind.equals("AuthenticationService");
        //oc get csv -n openshift-operators -o yaml
        ExecutionResultData result = KubeCMDClient.runOnCluster("get", "csv", "-n", infraNamespace, "-o", "json", csvName);
        JsonObject csv = new JsonObject(result.getStdOut());
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
        //give some time for plans to sync
        Thread.sleep(60_000);
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
    }

    @Test
    @Order(3)
    void testBasicMessagingAfterOlmInstallation() throws Exception {
        AddressSpace exampleSpace = kubernetes.getAddressSpaceClient(infraNamespace).withName("myspace").get();
        Address exampleAddress = kubernetes.getAddressClient(infraNamespace).withName("myspace.myqueue").get();
        getClientUtils().assertCanConnect(exampleSpace, new UserCredentials("user", "enmasse"), Collections.singletonList(exampleAddress), resourcesManager);
    }

    @Test
    @Order(4)
    void uninstallOperator() throws Exception {
        TestUtils.cleanAllEnmasseResourcesFromNamespace(infraNamespace);
        //oc delete subscriptions -n openshift-operators amq-online-operator
        KubeCMDClient.runOnCluster("delete", "subscriptions", "-n", infraNamespace, subscriptionName);
        //oc delete csv -n openshift-operators amqonline.1.3.0
        KubeCMDClient.runOnCluster("delete", "csv", "-n", infraNamespace, csvName);
        kubernetes.getConsoleServiceClient(infraNamespace).withPropagationPolicy("Background").delete();
        TestUtils.waitForNReplicas(0, false, Map.of("app", "enmasse"), Collections.emptyMap(), new TimeoutBudget(30, TimeUnit.SECONDS), 2000);
    }

}