/*
 * Copyright 2016-2019, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.systemtest.operator;

import io.enmasse.admin.model.v1.ConsoleService;
import io.enmasse.admin.model.v1.ConsoleServiceSpec;
import io.enmasse.systemtest.Environment;
import io.enmasse.systemtest.OLMInstallationType;
import io.enmasse.systemtest.condition.OpenShiftVersion;
import io.enmasse.systemtest.executor.Exec;
import io.enmasse.systemtest.executor.ExecutionResultData;
import io.enmasse.systemtest.logs.CustomLogger;
import io.enmasse.systemtest.platform.KubeCMDClient;
import io.enmasse.systemtest.platform.Kubernetes;
import io.enmasse.systemtest.platform.OpenShift;
import io.enmasse.systemtest.time.TimeoutBudget;
import io.enmasse.systemtest.utils.TestUtils;
import io.fabric8.kubernetes.api.model.EnvVar;
import org.slf4j.Logger;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class EnmasseOperatorManager {

    private static final Logger LOGGER = CustomLogger.getLogger();
    private final Kubernetes kube = Kubernetes.getInstance();
    private final Environment env = Environment.getInstance();
    private final String productName;
    private static EnmasseOperatorManager instance;
    private final OLMOperatorManager olm;

    private EnmasseOperatorManager() {
        productName = Environment.getInstance().getProductName();
        olm = OLMOperatorManager.getInstance();
    }

    public static synchronized EnmasseOperatorManager getInstance() {
        if (instance == null) {
            instance = new EnmasseOperatorManager();
        }
        return instance;
    }

    public OLMOperatorManager olm() {
        return olm;
    }

    public void installEnmasseBundle() throws Exception {
        LOGGER.info("***********************************************************");
        LOGGER.info("                  Enmasse operator install");
        LOGGER.info("***********************************************************");
        installOperators();
        installExamplesBundle(kube.getInfraNamespace());
        waitUntilOperatorReady(kube.getInfraNamespace());
        LOGGER.info("***********************************************************");
    }

    public void installEnmasseSharedInfraBundle() throws Exception {
        LOGGER.info("***********************************************************");
        LOGGER.info("         Enmasse operator shared infra install");
        LOGGER.info("***********************************************************");
        generateTemplates();
        kube.createNamespace(kube.getInfraNamespace(), Collections.singletonMap("allowed", "true"));
        KubeCMDClient.applyFromFile(kube.getInfraNamespace(), Paths.get(Environment.getInstance().getTemplatesPath(), "install", "preview-bundles", "enmasse"));
        TestUtils.waitUntilDeployed(kube.getInfraNamespace());
        LOGGER.info("***********************************************************");
    }

    public void installEnmasseOlm(OLMInstallationType installation) throws Exception {
        LOGGER.info("***********************************************************");
        LOGGER.info("                  Enmasse OLM install");
        LOGGER.info("***********************************************************");
        olm.install(installation);
        waitUntilOperatorReadyOlm(installation);
        LOGGER.info("***********************************************************");
    }

    public void installIoTOperator() {
        LOGGER.info("***********************************************************");
        LOGGER.info("                Enmasse IoT operator install");
        LOGGER.info("***********************************************************");
        LOGGER.info("Installing enmasse IoT operator from: {}", Environment.getInstance().getTemplatesPath());
        KubeCMDClient.applyFromFile(kube.getInfraNamespace(), Paths.get(Environment.getInstance().getTemplatesPath(), "install", "preview-bundles", "iot"));
        LOGGER.info("***********************************************************");
    }

    public void installMonitoringOperator() throws InterruptedException {
        LOGGER.info("***********************************************************");
        LOGGER.info("                Enmasse enmasse monitoring");
        LOGGER.info("***********************************************************");
        setEnmasseOperatorEnableMonitoring(false);
        kube.createNamespace(env.getMonitoringNamespace());
        KubeCMDClient.applyFromFile(env.getMonitoringNamespace(), Paths.get(env.getTemplatesPath(), "install", "components", "monitoring-operator"));
        waitForMonitoringResources();
        KubeCMDClient.applyFromFile(env.getMonitoringNamespace(), Paths.get(env.getTemplatesPath(), "install", "components", "monitoring-deployment"));
        TestUtils.waitForExpectedReadyPods(kube, env.getMonitoringNamespace(), 6, new TimeoutBudget(3, TimeUnit.MINUTES));
        enableMonitoringForNamespace();
        setEnmasseOperatorEnableMonitoring(true);
        KubeCMDClient.applyFromFile(kube.getInfraNamespace(), Paths.get(env.getTemplatesPath(), "install", "components", "kube-state-metrics"));
        LOGGER.info("***********************************************************");
    }

    public void deleteEnmasseBundle() throws Exception {
        LOGGER.info("***********************************************************");
        LOGGER.info("                  Enmasse operator delete");
        LOGGER.info("***********************************************************");
        deleteExamplesBundle(kube.getInfraNamespace());
        clean();
        LOGGER.info("***********************************************************");
    }

    public void deleteEnmasseOlm() throws Exception {
        LOGGER.info("***********************************************************");
        LOGGER.info("                  Enmasse OLM delete");
        LOGGER.info("***********************************************************");
        removeOlm();
        LOGGER.info("***********************************************************");
    }

    public void deleteEnmasseAnsible() {
        LOGGER.info("***********************************************************");
        LOGGER.info("            Enmasse operator delete by ansible");
        LOGGER.info("***********************************************************");
        Path inventoryFile = Paths.get(System.getProperty("user.dir"), "ansible", "inventory", kube.getOcpVersion() == OpenShiftVersion.OCP3 ? "systemtests.inventory" : "systemtests.ocp4.inventory");
        Path ansiblePlaybook = Paths.get(Environment.getInstance().getUpgradeTemplates(), "ansible", "playbooks", "openshift", "uninstall.yml");
        List<String> cmd = Arrays.asList("ansible-playbook", ansiblePlaybook.toString(), "-i", inventoryFile.toString(),
                "--extra-vars", String.format("namespace=%s", kube.getInfraNamespace()));
        assertTrue(Exec.execute(cmd, 300_000, true).getRetCode(), "Uninstall failed");
        LOGGER.info("***********************************************************");
    }

    public void deleteMonitoringOperator() throws Exception {
        LOGGER.info("***********************************************************");
        LOGGER.info("            Enmasse monitoring delete");
        LOGGER.info("***********************************************************");
        KubeCMDClient.deleteResource(env.getMonitoringNamespace(), "crd", "blackboxtargets.applicationmonitoring.integreatly.org");
        KubeCMDClient.deleteResource(env.getMonitoringNamespace(), "crd", "grafanadashboards.integreatly.org");
        KubeCMDClient.deleteResource(env.getMonitoringNamespace(), "crd", "grafanadatasources.integreatly.org");
        KubeCMDClient.deleteResource(env.getMonitoringNamespace(), "crd", "grafanas.integreatly.org");
        KubeCMDClient.deleteResource(env.getMonitoringNamespace(), "crd", "applicationmonitorings.applicationmonitoring.integreatly.org");
        KubeCMDClient.deleteResource(env.getMonitoringNamespace(), "crd", "alertmanagers.monitoring.coreos.com");
        KubeCMDClient.deleteResource(env.getMonitoringNamespace(), "crd", "podmonitors.monitoring.coreos.com");
        KubeCMDClient.deleteResource(env.getMonitoringNamespace(), "crd", "prometheuses.monitoring.coreos.com");
        KubeCMDClient.deleteResource(env.getMonitoringNamespace(), "crd", "prometheusrules.monitoring.coreos.com");
        KubeCMDClient.deleteResource(env.getMonitoringNamespace(), "crd", "servicemonitors.monitoring.coreos.com");
        kube.deleteNamespace(env.getMonitoringNamespace());
        LOGGER.info("***********************************************************");
    }

    private void installExamplesBundle(String namespace) throws Exception {
        installExamplePlans(namespace);
        installExampleRoles(namespace);
        if (kube.getOcpVersion() == OpenShiftVersion.OCP3) {
            installServiceCatalog(namespace);
        }
        installExampleAuthServices(namespace);
    }

    public void deleteExamplesBundle(String namespace) {
        removeExampleAuthServices(namespace);
        removeExampleRoles(namespace);
        if (kube.getOcpVersion() == OpenShiftVersion.OCP3) {
            removeServiceCatalog(namespace);
        }
        removeExamplePlans(namespace);
    }

    public void installEnmasseOlm() throws Exception {
        installEnmasseOlm(Environment.getInstance().olmInstallType());
    }

    public void installExamplesBundleOlm() throws Exception {
        installExamplesBundle(getNamespaceByOlmInstallationType(Environment.getInstance().olmInstallType()));
    }

    public void installOperators() {
        LOGGER.info("Installing enmasse operators from: {}", Environment.getInstance().getTemplatesPath());
        generateTemplates();
        kube.createNamespace(kube.getInfraNamespace(), Collections.singletonMap("allowed", "true"));
        KubeCMDClient.applyFromFile(kube.getInfraNamespace(), Paths.get(Environment.getInstance().getTemplatesPath(), "install", "bundles", productName));
    }

    public void installExamplePlans(String namespace) {
        LOGGER.info("Installing enmasse example plans from: {}", Environment.getInstance().getTemplatesPath());
        KubeCMDClient.applyFromFile(namespace, Paths.get(Environment.getInstance().getTemplatesPath(), "install", "components", "example-plans"));
    }

    public void installExampleRoles(String namespace) {
        LOGGER.info("Installing enmasse roles from: {}", Environment.getInstance().getTemplatesPath());
        KubeCMDClient.applyFromFile(namespace, Paths.get(Environment.getInstance().getTemplatesPath(), "install", "components", "example-roles"));
    }

    public void installExampleAuthServices(String namespace) throws Exception {
        LOGGER.info("Installing enmasse example auth services from: {}", Environment.getInstance().getTemplatesPath());
        KubeCMDClient.applyFromFile(namespace, Paths.get(Environment.getInstance().getTemplatesPath(), "install", "components", "example-authservices", "standard-authservice.yaml"));
        KubeCMDClient.applyFromFile(namespace, Paths.get(Environment.getInstance().getTemplatesPath(), "install", "components", "example-authservices", "none-authservice.yaml"));
        TestUtils.waitForPodReady("standard-authservice", namespace);
        TestUtils.waitForPodReady("none-authservice", namespace);
    }

    public void installServiceCatalog(String namespace) {
        LOGGER.info("Installing enmasse service catalog from: {}", Environment.getInstance().getTemplatesPath());
        KubeCMDClient.applyFromFile(namespace, Paths.get(Environment.getInstance().getTemplatesPath(), "install", "components", "service-broker"));
        KubeCMDClient.applyFromFile(namespace, Paths.get(Environment.getInstance().getTemplatesPath(), "install", "components", "cluster-service-broker"));
    }

    public void removeExamplePlans(String namespace) {
        LOGGER.info("Delete enmasse example plans from: {}", Environment.getInstance().getTemplatesPath());
        KubeCMDClient.deleteFromFile(namespace, Paths.get(Environment.getInstance().getTemplatesPath(), "install", "components", "example-plans"));
    }

    private void enableMonitoringForNamespace() {
        Kubernetes.getInstance().getClient().namespaces()
                .withName(kube.getInfraNamespace())
                .edit()
                .editMetadata()
                .addToLabels("monitoring-key", "middleware")
                .endMetadata()
                .done();
    }

    private void setEnmasseOperatorEnableMonitoring(boolean enable) {
        List<EnvVar> envVars = Kubernetes.getInstance().getClient().apps()
                .deployments()
                .inNamespace(kube.getInfraNamespace())
                .withName("enmasse-operator")
                .get().getSpec().getTemplate().getSpec().getContainers().get(0).getEnv();
        List<EnvVar> updatedEnvVars = envVars
                .stream()
                .peek(envVarObj -> {
                    if ("ENABLE_MONITORING".equals(envVarObj.getName())) {
                        envVarObj.setValue(Boolean.toString(enable));
                    }
                })
                .collect(Collectors.toList());

        Kubernetes.getInstance().getClient().apps()
                .deployments()
                .inNamespace(kube.getInfraNamespace())
                .withName("enmasse-operator")
                .edit()
                .editSpec()
                .editTemplate()
                .editSpec()
                .editFirstContainer()
                .withEnv(updatedEnvVars)
                .endContainer()
                .endSpec()
                .endTemplate()
                .endSpec()
                .done();
    }

    public boolean removeOlm() throws Exception {
        Consumer<String> remover = (namespace) -> {
            KubeCMDClient.runOnCluster("delete", "subscriptions", "-l", "app=enmasse", "-n", namespace);
            KubeCMDClient.runOnCluster("delete", "operatorgroups", "-l", "app=enmasse", "-n", namespace);
            KubeCMDClient.runOnCluster("delete", "catalogsources", "-l", "app=enmasse", "-n", namespace);
            KubeCMDClient.runOnCluster("delete", "csv", "-l", "app=enmasse", "-n", namespace);
            KubeCMDClient.runOnCluster("delete", "deployments", "-l", "app=enmasse", "-n", namespace);
            KubeCMDClient.runOnCluster("delete", "statefulsets", "-l", "app=enmasse", "-n", namespace);
            KubeCMDClient.runOnCluster("delete", "configmaps", "-l", "app=enmasse", "-n", namespace);
            KubeCMDClient.runOnCluster("delete", "secrets", "-l", "app=enmasse", "-n", namespace);
            KubeCMDClient.runOnCluster("delete", "services", "-l", "app=enmasse", "-n", namespace);
        };
        LOGGER.info("Delete enmasse OLM from: {}", Environment.getInstance().getTemplatesPath());
        if (isEnmasseOlmDeployed(kube.getOlmNamespace())) {
            remover.accept(kube.getOlmNamespace());
        }
        if (isEnmasseOlmDeployed(kube.getInfraNamespace())) {
            remover.accept(kube.getInfraNamespace());
        }
        olm.clean();
        return clean();
    }

    public void removeExampleRoles(String namespace) {
        LOGGER.info("Delete enmasse roles from: {}", Environment.getInstance().getTemplatesPath());
        KubeCMDClient.deleteFromFile(namespace, Paths.get(Environment.getInstance().getTemplatesPath(), "install", "components", "example-roles"));
    }

    public void removeExampleAuthServices(String namespace) {
        LOGGER.info("Delete enmasse example auth services from: {}", Environment.getInstance().getTemplatesPath());
        KubeCMDClient.deleteFromFile(namespace, Paths.get(Environment.getInstance().getTemplatesPath(), "install", "components", "example-authservices", "standard-authservice.yaml"));
        KubeCMDClient.deleteFromFile(namespace, Paths.get(Environment.getInstance().getTemplatesPath(), "install", "components", "example-authservices", "none-authservice.yaml"));
    }

    public void removeServiceCatalog(String namespace) {
        LOGGER.info("Delete enmasse service catalog from: {}", Environment.getInstance().getTemplatesPath());
        KubeCMDClient.deleteFromFile(namespace, Paths.get(Environment.getInstance().getTemplatesPath(), "install", "components", "service-broker"));
        KubeCMDClient.deleteFromFile(namespace, Paths.get(Environment.getInstance().getTemplatesPath(), "install", "components", "cluster-service-broker"));
    }

    public void removeIoT() {
        LOGGER.info("Delete enmasse IoT from: {}", Environment.getInstance().getTemplatesPath());
        KubeCMDClient.deleteFromFile(kube.getInfraNamespace(), Paths.get(Environment.getInstance().getTemplatesPath(), "install", "preview-bundles", "iot"));
        KubeCMDClient.runOnCluster("delete", "iotconfigs", "--all", "-n", kube.getInfraNamespace());
    }

    public boolean clean() throws Exception {
        cleanCRDs();
        KubeCMDClient.runOnCluster("delete", "clusterrolebindings", "-l", "app=enmasse");
        KubeCMDClient.runOnCluster("delete", "clusterroles", "-l", "app=enmasse");
        KubeCMDClient.runOnCluster("delete", "apiservices", "-l", "app=enmasse");
        KubeCMDClient.runOnCluster("delete", "oauthclients", "-l", "app=enmasse");
        if (kube.getOcpVersion() == OpenShiftVersion.OCP4) {
            KubeCMDClient.runOnCluster("delete", "consolelinks", "-l", "app=enmasse");
        }
        KubeCMDClient.runOnCluster("delete", "clusterservicebrokers", "-l", "app=enmasse");
        if (!kube.getInfraNamespace().equals(kube.getOlmNamespace())) {
            kube.deleteNamespace(kube.getInfraNamespace(), Duration.ofMinutes(kube.getOcpVersion() == OpenShiftVersion.OCP4 ? 10 : 5));
        }
        return true;
    }

    private void cleanCRDs() {
        KubeCMDClient.runOnCluster("delete", "crd", "-l", "app=enmasse,enmasse-component=iot");
        KubeCMDClient.runOnClusterWithTimeout(600_000, "delete", "crd", "-l", "app=enmasse", "--timeout=600s");
    }

    public void waitUntilOperatorReadyOlm() throws Exception {
        waitUntilOperatorReadyOlm(Environment.getInstance().olmInstallType());
    }

    public void waitUntilOperatorReadyOlm(OLMInstallationType installation) throws Exception {
        waitUntilOperatorReady(getNamespaceByOlmInstallationType(installation));
    }

    public void waitUntilOperatorReady(String namespace) throws Exception {
        Thread.sleep(5000);
        TestUtils.waitUntilDeployed(namespace);

        if (kube instanceof OpenShift) {
            // Kubernetes does not make a console service available by default.
            awaitConsoleReadiness(namespace);
        }
    }

    private void generateTemplates() {
        if (Files.notExists(Paths.get(Environment.getInstance().getTemplatesPath()))) {
            LOGGER.info("Generating templates.");
            Exec.execute(Arrays.asList("make", "-C", "..", "templates"));
        }
    }

    private void waitForMonitoringResources() {
        LOGGER.info("Waiting for monitoring resources to be installed");
        TestUtils.waitUntilCondition("Monitoring resources installed", phase -> {
            String permissions = KubeCMDClient.checkPermission("create", "prometheus", env.getMonitoringNamespace(), "application-monitoring-operator").getStdOut();
            return permissions.trim().equals("yes");
        }, new TimeoutBudget(3, TimeUnit.MINUTES));
    }

    private void awaitConsoleReadiness(String namespace) throws Exception {
        final String serviceName = "console";
        TestUtils.waitUntilCondition("global console readiness", waitPhase -> {
            try {
                final ConsoleService console = kube.getConsoleServiceClient().inNamespace(namespace).withName("console").get();
                if (console == null) {
                    LOGGER.info("ConsoleService {} not yet available", serviceName);
                    return false;
                }
                TestUtils.waitForPodReady("console", namespace);
                final ConsoleServiceSpec spec = console.getSpec();
                final boolean ready = spec != null && spec.getOauthClientSecret() != null
                        && spec.getSsoCookieSecret() != null && console.getStatus().getHost() != null;
                if (!ready) {
                    LOGGER.info("ConsoleService {} not yet fully ready: {}", serviceName, spec);
                }
                return ready;
            } catch (Exception e) {
                LOGGER.warn("Failed to get console service record : {}", serviceName, e);
            }

            return false;
        }, new TimeoutBudget(10, TimeUnit.MINUTES));
    }

    public boolean isEnmasseBundleDeployed() {
        return kube.namespaceExists(kube.getInfraNamespace())
                && kube.listPods(kube.getInfraNamespace()).stream().filter(pod -> pod.getMetadata().getName().contains("enmasse-operator")).count() == 1
                && kube.listPods(kube.getInfraNamespace()).stream().filter(pod -> pod.getMetadata().getName().contains("address-space-controller")).count() == 1;
    }

    public boolean isEnmasseOlmDeployed() {
        if (isEnmasseOlmDeployed(kube.getOlmNamespace())) {
            return true;
        }
        return kube.namespaceExists(kube.getInfraNamespace()) && isEnmasseOlmDeployed(kube.getInfraNamespace());
    }

    public boolean areExamplesApplied() {
        return kube.namespaceExists(kube.getInfraNamespace())
                && kube.getAddressSpacePlanClient(kube.getInfraNamespace()).withName("brokered-single-broker").get() != null
                && kube.getAuthenticationServiceClient(kube.getInfraNamespace()).withName("standard-authservice").get() != null
                && kube.getAuthenticationServiceClient(kube.getInfraNamespace()).withName("none-authservice").get() != null;
    }

    private boolean isEnmasseOlmDeployed(String namespace) {
        ExecutionResultData res = KubeCMDClient.runOnCluster("get", "subscriptions", "-n", namespace);
        if (res.getRetCode()) {
            return res.getStdOut().contains("enmasse-sub");
        }
        return false;
    }

    public String getNamespaceByOlmInstallationType(OLMInstallationType installation) {
        return olm.getNamespaceByOlmInstallationType(installation);
    }
}
