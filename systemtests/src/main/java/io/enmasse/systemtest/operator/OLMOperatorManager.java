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
import java.util.concurrent.TimeUnit;

import io.enmasse.systemtest.time.TimeoutBudget;
import io.fabric8.kubernetes.api.model.HasMetadata;
import org.slf4j.Logger;

import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;

import io.enmasse.systemtest.Environment;
import io.enmasse.systemtest.OLMInstallationType;
import io.enmasse.systemtest.logs.CustomLogger;
import io.enmasse.systemtest.platform.KubeCMDClient;
import io.enmasse.systemtest.platform.Kubernetes;
import io.enmasse.systemtest.platform.apps.SystemtestsKubernetesApps;
import io.enmasse.systemtest.utils.TestUtils;

public class OLMOperatorManager {

    private static final Logger log = CustomLogger.getLogger();
    private final String templatesPath;
    private Kubernetes kube = Kubernetes.getInstance();
    private String clusterExternalImageRegistry;
    private String clusterInternalImageRegistry;
    private static OLMOperatorManager instance;

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

    public void applyDownstreamSubscription (String installationNamespace) throws IOException {
        Path subscriptionFile = Files.createTempFile("subscription", ".yaml");
        String subscription = Files.readString(Paths.get(System.getProperty("user.dir"), "..","systemtests","olm", "subscription.yaml"));
        Files.writeString(subscriptionFile,
                subscription
                        .replaceAll("\\$\\{OPERATOR_NAMESPACE}", installationNamespace)
                        .replaceAll("\\$\\{CATALOG_SOURCE}", Environment.getInstance().getCatalogSource())
                        .replaceAll("\\$\\{PRODUCT_VERSION}", Environment.getInstance().getProductVersion()));
        KubeCMDClient.applyFromFile(installationNamespace, subscriptionFile);
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