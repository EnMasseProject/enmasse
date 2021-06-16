/*
 * Copyright 2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.systemtest.operator;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.Iterator;
import java.util.concurrent.TimeUnit;

import io.enmasse.systemtest.time.TimeoutBudget;
import io.fabric8.kubernetes.api.model.HasMetadata;
import java.util.ArrayList;
import java.util.Collections;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import io.enmasse.systemtest.executor.ExecutionResultData;
import org.slf4j.Logger;

import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;

import io.enmasse.systemtest.Environment;
import io.enmasse.systemtest.OLMInstallationType;
import io.enmasse.systemtest.logs.CustomLogger;
import io.enmasse.systemtest.platform.KubeCMDClient;
import io.enmasse.systemtest.platform.Kubernetes;
import io.enmasse.systemtest.platform.apps.SystemtestsKubernetesApps;
import io.enmasse.systemtest.utils.TestUtils;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class OLMOperatorManager {

    private static final Logger log = CustomLogger.getLogger();
    private final String templatesPath;
    private Kubernetes kube = Kubernetes.getInstance();
    private String clusterExternalImageRegistry;
    private String clusterInternalImageRegistry;
    private static OLMOperatorManager instance;
    private static final ArrayList<String> installPlanList = new ArrayList<>();

    private OLMOperatorManager() {
        clusterExternalImageRegistry = Environment.getInstance().getClusterExternalImageRegistry();
        if (clusterExternalImageRegistry == null || clusterExternalImageRegistry.isBlank()) {
            clusterExternalImageRegistry = kube.getClusterExternalImageRegistry();
        }
        clusterInternalImageRegistry = Environment.getInstance().getClusterInternalImageRegistry();
        if (clusterInternalImageRegistry == null || clusterInternalImageRegistry.isBlank()) {
            clusterInternalImageRegistry = kube.getClusterInternalImageRegistry();
        }
        log.info("Using image registries: {} and {}", clusterExternalImageRegistry, clusterInternalImageRegistry);
        templatesPath = Environment.getInstance().getTemplatesPath(Kubernetes.getInstance());
    }

    public static synchronized OLMOperatorManager getInstance() {
        if (instance == null) {
            instance = new OLMOperatorManager();
        }
        return instance;
    }

    public void install(OLMInstallationType installation) throws Exception {
        install(installation, getLatestManifestsImage(), getLatestStartingCsv());
    }

    public void install(OLMInstallationType installation, String manifestsImage, String csvName) throws Exception {
        log.info("Installing using olm from {} and {}", manifestsImage, csvName);
        String namespace = getNamespaceByOlmInstallationType(installation);

        if (installation == OLMInstallationType.SPECIFIC) {
            kube.createNamespace(namespace, Collections.singletonMap("allowed", "true"));
        }

        if(!Environment.getInstance().isTestDownstream().equals("true")){
            deployCatalogSource(namespace);
        }

        if (installation == OLMInstallationType.SPECIFIC) {
            Path operatorGroupFile = Files.createTempFile("operatorgroup", ".yaml");
            String operatorGroup = Files.readString(Paths.get(templatesPath, "install", "components", "example-olm", "operator-group.yaml"));
            Files.writeString(operatorGroupFile, operatorGroup.replaceAll("\\$\\{OPERATOR_NAMESPACE}", namespace));
            KubeCMDClient.applyFromFile(namespace, operatorGroupFile);
        }

        if(Environment.getInstance().isTestDownstream().equals("true")){
            applyDownstreamSubscription(namespace);

            if(Environment.getInstance().getInstallPlanApproval().equals("Manual")) {
                TestUtils.waitForInstallPlanPresent(namespace);
                String installPlanName = (String) getInstallPlanName(namespace).get(0);
                approveInstallPlan(namespace, installPlanName);
            }
        }
        else{
            applySubscription(namespace, namespace, csvName, Environment.getInstance().getOperatorName(), Environment.getInstance().getOperatorChannel());
        }

        TestUtils.waitForPodReady("enmasse-operator", namespace);
    }

    public void clean() throws Exception {
        //TODO investigate how to remove images pushed to image registry
        SystemtestsKubernetesApps.cleanBuiltContainerImages(kube);
    }

    public void applySubscription(String installationNamespace, String catalogNamespace, String csvName, String operatorName, String operatorChannel) throws IOException {
        Path subscriptionFile = Files.createTempFile("subscription", ".yaml");
        String subscription = Files.readString(Paths.get(templatesPath, "install", "components", "example-olm", "subscription.yaml"));
        Files.writeString(subscriptionFile,
                subscription
                        .replaceAll("\\$\\{OPERATOR_NAMESPACE}", installationNamespace)
                        .replaceAll("\\$\\{CSV}", csvName));
        KubeCMDClient.applyFromFile(installationNamespace, subscriptionFile);
        KubeCMDClient.awaitStatusCondition(new TimeoutBudget(2, TimeUnit.MINUTES), installationNamespace,
                "subscription", "enmasse-sub", "CatalogSourcesUnhealthy=False");
    }

    public void applyDownstreamSubscription (String installationNamespace) throws IOException, InterruptedException {        Path subscriptionFile = Files.createTempFile("subscription", ".yaml");
        String subscription = Files.readString(Paths.get(System.getProperty("user.dir"), "..","systemtests","olm", "subscription.yaml"));
        Files.writeString(subscriptionFile,
                subscription
                        .replaceAll("\\$\\{OPERATOR_NAMESPACE}", installationNamespace)
                        .replaceAll("\\$\\{CATALOG_SOURCE}", Environment.getInstance().getCatalogSource())
                        .replaceAll("\\$\\{PRODUCT_VERSION}", Environment.getInstance().getProductVersion())
                        .replaceAll("\\$\\{INSTALL_PLAN_APPROVAL}", Environment.getInstance().getInstallPlanApproval()));
        KubeCMDClient.applyFromFile(installationNamespace, subscriptionFile);
        KubeCMDClient.awaitStatusCondition(new TimeoutBudget(2, TimeUnit.MINUTES), installationNamespace,
                "subscription", "amq-online", "CatalogSourcesUnhealthy=False");
    }

    public void approveInstallPlan(String namespace, String installPlanName) {
        log.info("Approving install plan {} in namespace {}", installPlanName, namespace);
        ExecutionResultData result = KubeCMDClient.runOnCluster("patch", "installplan", installPlanName, "--namespace", namespace, "--type", "merge",  "--patch", "{\"spec\":{\"approved\":true}}");
        //oc patch installplan install-whmhz --namespace openshift-operators --type merge --patch {"spec":{"approved":true}}
        log.info(result.getStdOut());
        installPlanList.removeIf(string -> string.contains(installPlanName));
        assertTrue(result.getRetCode(), result.getStdOut());
    }

    public ArrayList getInstallPlanName(String namespace) {
        ExecutionResultData result = KubeCMDClient.runOnCluster("get", "installplan", "-n", namespace);
        String installPlansString = result.getStdOut();
        String[] installPlansLines = installPlansString.split("\n");

        for (String line : installPlansLines) {
            // line: NAME  CSV  APPROVAL   APPROVED
            String[] wholeLine = line.split(" ");

            // name
            if (wholeLine[0].startsWith("install-")) {
                if (!installPlanList.contains(wholeLine[0])) {
                    log.info("Adding {} install plan to the list of available install plans.", wholeLine[0]);
                    installPlanList.add(wholeLine[0]);
                }
            }
        }
        if ((installPlanList.isEmpty())) {
            log.warn("No install plans located in namespace: " + namespace);
        }
        return installPlanList;
    }

    //delete later, just another method for getting install plan name
    public ArrayList getInstallPlanNameFromSubDescription(String namespace) {
        ExecutionResultData result = KubeCMDClient.runOnCluster("describe", "subscription", "amq-online", "-n", namespace);
        String installPlansString = result.getStdOut();
        String[] installPlansLines = installPlansString.split("\n");

        for (int i = 0; i < installPlansLines.length; i++) {
            if (installPlansLines[i].contains("Kind:") && installPlansLines[i].contains("InstallPlan")) {
                if (installPlansLines[i+1].contains("Name:")){
                    String line = installPlansLines[i+1].trim();
                    String pattern = "Name:\\s*(install-[a-zA-Z0-9]*)";
                    Pattern r = Pattern.compile(pattern);
                    Matcher m = r.matcher(line);
                    if (m.find( )) {
                        String installPlanName = m.group(1);
                        if (!installPlanList.contains(installPlanName)) {
                            log.info("Adding {} install plan to the list of available install plans.", installPlanName);
                            installPlanList.add(installPlanName);
                        }
                    }
                }
            }
        }
        if ((installPlanList.isEmpty())) {
            log.warn("No install plans located in namespace: " + namespace);
        }
        return installPlanList;
    }

    public void deployCatalogSource(String catalogNamespace) throws IOException {
        Path catalogSourceFile = Files.createTempFile("catalogsource", ".yaml");
        String catalogSource = Files.readString(Paths.get(templatesPath, "install", "components", "example-olm", "catalog-source.yaml"));
        Files.writeString(catalogSourceFile,
                catalogSource
                        .replaceAll("\\$\\{OPERATOR_NAMESPACE}", catalogNamespace));
        KubeCMDClient.applyFromFile(catalogNamespace, catalogSourceFile);
        KubeCMDClient.awaitCatalogSourceReady(new TimeoutBudget(2, TimeUnit.MINUTES), catalogNamespace, "enmasse-source");
    }

    private boolean saysDoingNothing(String text) {
        return text != null && text.replace("\n", "").replace("\r", "").strip().equals("Doing nothing");
    }

    public String getLatestStartingCsv() throws Exception {
        String enmasseCSV = Files.readString(Paths.get(templatesPath, "install", "components", "example-olm", "subscription.yaml"));
        var yaml = new YAMLMapper().readTree(enmasseCSV);
        return yaml.get("spec").get("startingCSV").asText();
    }

    public String getLatestManifestsImage() throws Exception {
        String exampleCatalogSource = Files.readString(Paths.get(templatesPath.toString(), "install", "components", "example-olm", "catalog-source.yaml"));
        var tree = new YAMLMapper().readTree(exampleCatalogSource);
        return tree.get("spec").get("image").asText();
    }

    public String getNamespaceByOlmInstallationType(OLMInstallationType installation) {
        return installation == OLMInstallationType.DEFAULT ? kube.getOlmNamespace() : kube.getInfraNamespace();
    }
}