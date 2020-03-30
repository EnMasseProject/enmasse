/*
 * Copyright 2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.systemtest.operator;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collections;
import org.slf4j.Logger;

import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;

import io.enmasse.systemtest.Environment;
import io.enmasse.systemtest.OLMInstallationType;
import io.enmasse.systemtest.executor.Exec;
import io.enmasse.systemtest.executor.ExecutionResultData;
import io.enmasse.systemtest.logs.CustomLogger;
import io.enmasse.systemtest.platform.KubeCMDClient;
import io.enmasse.systemtest.platform.Kubernetes;
import io.enmasse.systemtest.utils.TestUtils;

public class OLMOperatorManager {

    private static final String DEFAULT_NAME_ENMASSE_SOURCE = "enmasse-source";
    private static final Logger log = CustomLogger.getLogger();
    private Kubernetes kube = Kubernetes.getInstance();
    private String clusterExternalImageRegistry;
    private String clusterInternalImageRegistry;
    private static OLMOperatorManager instance;

    private OLMOperatorManager() {
        clusterExternalImageRegistry = Environment.getInstance().getClusterExternalImageRegistry();
        clusterInternalImageRegistry = Environment.getInstance().getClusterInternalImageRegistry();
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
        String namespace = getNamespaceByOlmInstallationType(installation);

        if (installation == OLMInstallationType.SPECIFIC) {
            kube.createNamespace(namespace, Collections.singletonMap("allowed", "true"));
        }

        String catalogSourceName = DEFAULT_NAME_ENMASSE_SOURCE;
        String catalogNamespace = namespace;

        buildPushCustomOperatorRegistry(catalogNamespace, manifestsImage);
        String customRegistryImageToUse = getCustomOperatorRegistryInternalImage(catalogNamespace);
        deployCatalogSource(catalogSourceName, catalogNamespace, customRegistryImageToUse);

        if (installation == OLMInstallationType.SPECIFIC) {
            Path operatorGroupFile = Files.createTempFile("operatorgroup", ".yaml");
            String operatorGroup = Files.readString(Paths.get("custom-operator-registry", "operator-group.yaml"));
            Files.writeString(operatorGroupFile, operatorGroup.replaceAll("\\$\\{OPERATOR_NAMESPACE}", namespace));
            KubeCMDClient.applyFromFile(namespace, operatorGroupFile);
        }

        applySubscription(namespace, catalogSourceName, catalogNamespace, csvName);

        TestUtils.waitForPodReady("enmasse-operator", namespace);

    }

    public void applySubscription(String installationNamespace, String catalogSourceName, String catalogNamespace, String csvName) throws IOException {
        Path subscriptionFile = Files.createTempFile("subscription", ".yaml");
        String subscription = Files.readString(Paths.get("custom-operator-registry", "subscription.yaml"));
        Files.writeString(subscriptionFile,
                subscription
                    .replaceAll("\\$\\{OPERATOR_NAMESPACE}", installationNamespace)
                    .replaceAll("\\$\\{CATALOG_SOURCE_NAME}", catalogSourceName)
                    .replaceAll("\\$\\{CATALOG_NAMESPACE}", catalogNamespace)
                    .replaceAll("\\$\\{CSV}", csvName));
        KubeCMDClient.applyFromFile(installationNamespace, subscriptionFile);
    }

    public void deployCatalogSource(String catalogSourceName, String catalogNamespace, String customRegistryImageToUse) throws IOException {
        Path catalogSourceFile = Files.createTempFile("catalogsource", ".yaml");
        String catalogSource = Files.readString(Paths.get("custom-operator-registry", "catalog-source.yaml"));
        Files.writeString(catalogSourceFile,
                catalogSource
                    .replaceAll("\\$\\{CATALOG_SOURCE_NAME}", catalogSourceName)
                    .replaceAll("\\$\\{OPERATOR_NAMESPACE}", catalogNamespace)
                    .replaceAll("\\$\\{REGISTRY_IMAGE}", customRegistryImageToUse));
        KubeCMDClient.applyFromFile(catalogNamespace, catalogSourceFile);
    }

    public void buildPushCustomOperatorRegistry(String namespace, String manifestsImage) throws Exception {
        String customRegistryImageToPush = clusterExternalImageRegistry+"/"+namespace+"/systemtests-operator-registry:latest";

        String olmManifestsImage = manifestsImage.replace(clusterInternalImageRegistry, clusterExternalImageRegistry);

        int retries = 5;
        ExecutionResultData results = null;
        while (retries > 0) {
            results = Exec.execute(Arrays.asList("make", "-C", "custom-operator-registry", "FROM="+olmManifestsImage, "TAG="+customRegistryImageToPush), true);
            if(results.getRetCode()) {
                return;
            }
            Thread.sleep(1000);
            retries--;
        }
        assertTrue(results != null && results.getRetCode(), "custom operator registry image build failed ");
    }

    public String getCustomOperatorRegistryInternalImage(String namespace) {
        return clusterInternalImageRegistry+"/"+namespace+"/systemtests-operator-registry:latest";
    }

    public String getLatestStartingCsv() throws Exception {
        String enmasseCSV = Files.readString(Paths.get(Environment.getInstance().getTemplatesPath().toString(), "install", "components", "example-olm", "subscription.yaml"));
        var yaml = new YAMLMapper().readTree(enmasseCSV);
        return yaml.get("spec").get("startingCSV").asText();
    }

    public String getLatestManifestsImage() throws Exception {
        String exampleCatalogSource = Files.readString(Paths.get(Environment.getInstance().getTemplatesPath().toString(), "install", "components", "example-olm", "catalog-source.yaml"));
        var tree = new YAMLMapper().readTree(exampleCatalogSource);
        return tree.get("spec").get("image").asText();
    }

    public String getNamespaceByOlmInstallationType(OLMInstallationType installation) {
        return installation == OLMInstallationType.DEFAULT ? kube.getOlmNamespace() : kube.getInfraNamespace();
    }

}
