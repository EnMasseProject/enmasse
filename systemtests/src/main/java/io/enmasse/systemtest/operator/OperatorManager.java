/*
 * Copyright 2016-2019, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.systemtest.operator;

import io.enmasse.admin.model.v1.ConsoleService;
import io.enmasse.admin.model.v1.ConsoleServiceSpec;
import io.enmasse.systemtest.Environment;
import io.enmasse.systemtest.certs.CertBundle;
import io.enmasse.systemtest.executor.Exec;
import io.enmasse.systemtest.platform.KubeCMDClient;
import io.enmasse.systemtest.logs.CustomLogger;
import io.enmasse.systemtest.platform.Kubernetes;
import io.enmasse.systemtest.platform.Minikube;
import io.enmasse.systemtest.platform.OpenShift;
import io.enmasse.systemtest.time.TimeoutBudget;
import io.enmasse.systemtest.utils.CertificateUtils;
import io.enmasse.systemtest.utils.TestUtils;
import io.fabric8.kubernetes.api.model.Secret;
import io.fabric8.kubernetes.api.model.SecretBuilder;
import io.fabric8.kubernetes.client.KubernetesClientException;
import org.slf4j.Logger;

import java.nio.file.Paths;
import java.util.concurrent.TimeUnit;
import java.util.Collections;

public class OperatorManager {
    private static Logger LOGGER = CustomLogger.getLogger();
    private Kubernetes kube = Kubernetes.getInstance();
    private String productName;
    private static OperatorManager instance;

    private OperatorManager() {
        productName = Environment.getInstance().isDownstream() ? "amq-online" : "enmasse";
    }

    public static synchronized OperatorManager getInstance() {
        if (instance == null) {
            instance = new OperatorManager();
        }
        return instance;
    }

    public void installEnmasseBundle() throws Exception {
        LOGGER.info("***********************************************************");
        LOGGER.info("                  Enmasse operator install");
        LOGGER.info("***********************************************************");
        installOperators();
        installExamplePlans();
        installExampleRoles();
        if (kube.getOcpVersion() < 4) {
            installServiceCatalog();
        }
        installExampleAuthServices();
        waithUntilOperatorReady();
        LOGGER.info("***********************************************************");
    }

    public void deleteEnmasseBundle() {
        LOGGER.info("***********************************************************");
        LOGGER.info("                  Enmasse operator delete");
        LOGGER.info("***********************************************************");
        removeExampleAuthServices();
        removeExampleRoles();
        if (kube.getOcpVersion() < 4) {
            removeServiceCatalog();
        }
        removeExamplePlans();
        removeOperators();
        LOGGER.info("***********************************************************");
    }

    public void installOperators() throws Exception {
        LOGGER.info("Installing enmasse operators from: {}", Environment.getInstance().getTemplatesPath());
        kube.createNamespace(Environment.getInstance().namespace(), Collections.singletonMap("allowed", "true"));
        if (kube instanceof Minikube) {
            CertBundle apiServertCert = CertificateUtils.createCertBundle("api-server." + kube.getInfraNamespace() + ".svc.cluster.local");
            Secret secret = new SecretBuilder()
                    .editOrNewMetadata()
                    .withName("api-server-cert")
                    .endMetadata()
                    .addToData("tls.key", apiServertCert.getKeyB64())
                    .addToData("tls.crt", apiServertCert.getCertB64())
                    .build();
            kube.createSecret(kube.getInfraNamespace(), secret);
        }
        KubeCMDClient.applyFromFile(Environment.getInstance().namespace(), Paths.get(Environment.getInstance().getTemplatesPath(), "install", "bundles", productName));
    }

    public void installExamplePlans() {
        LOGGER.info("Installing enmasse example role from: {}", Environment.getInstance().getTemplatesPath());
        KubeCMDClient.applyFromFile(Environment.getInstance().namespace(), Paths.get(Environment.getInstance().getTemplatesPath(), "install", "components", "example-plans"));
    }

    public void installExampleRoles() {
        LOGGER.info("Installing enmasse roles from: {}", Environment.getInstance().getTemplatesPath());
        KubeCMDClient.applyFromFile(Environment.getInstance().namespace(), Paths.get(Environment.getInstance().getTemplatesPath(), "install", "components", "example-roles"));
    }

    public void installExampleAuthServices() throws Exception {
        LOGGER.info("Installing enmasse example auth services from: {}", Environment.getInstance().getTemplatesPath());
        KubeCMDClient.applyFromFile(Environment.getInstance().namespace(), Paths.get(Environment.getInstance().getTemplatesPath(), "install", "components", "example-authservices", "standard-authservice.yaml"));
        KubeCMDClient.applyFromFile(Environment.getInstance().namespace(), Paths.get(Environment.getInstance().getTemplatesPath(), "install", "components", "example-authservices", "none-authservice.yaml"));
        TestUtils.waitForPodReady("standard-authservice", kube.getInfraNamespace());
        TestUtils.waitForPodReady("none-authservice", kube.getInfraNamespace());
    }

    public void installServiceCatalog() {
        LOGGER.info("Installing enmasse service catalog from: {}", Environment.getInstance().getTemplatesPath());
        KubeCMDClient.applyFromFile(Environment.getInstance().namespace(), Paths.get(Environment.getInstance().getTemplatesPath(), "install", "components", "service-broker"));
        KubeCMDClient.applyFromFile(Environment.getInstance().namespace(), Paths.get(Environment.getInstance().getTemplatesPath(), "install", "components", "cluster-service-broker"));
    }

    public void installIoTOperator() throws Exception {
        LOGGER.info("***********************************************************");
        LOGGER.info("                Enmasse IoT operator install");
        LOGGER.info("***********************************************************");
        LOGGER.info("Installing enmasse IoT operator from: {}", Environment.getInstance().getTemplatesPath());
        KubeCMDClient.applyFromFile(Environment.getInstance().namespace(), Paths.get(Environment.getInstance().getTemplatesPath(), "install", "preview-bundles", "iot"));
        LOGGER.info("***********************************************************");

    }

    public void removeOperators() {
        LOGGER.info("Delete enmasse operators from: {}", Environment.getInstance().getTemplatesPath());
        KubeCMDClient.deleteFromFile(Environment.getInstance().namespace(), Paths.get(Environment.getInstance().getTemplatesPath(), "install", "bundles", productName));
    }

    public void removeExamplePlans() {
        LOGGER.info("Delete enmasse example role from: {}", Environment.getInstance().getTemplatesPath());
        KubeCMDClient.deleteFromFile(Environment.getInstance().namespace(), Paths.get(Environment.getInstance().getTemplatesPath(), "install", "components", "example-plans"));
    }

    public void removeExampleRoles() {
        LOGGER.info("Delete enmasse roles from: {}", Environment.getInstance().getTemplatesPath());
        KubeCMDClient.deleteFromFile(Environment.getInstance().namespace(), Paths.get(Environment.getInstance().getTemplatesPath(), "install", "components", "example-roles"));
    }

    public void removeExampleAuthServices() {
        LOGGER.info("Delete enmasse example auth services from: {}", Environment.getInstance().getTemplatesPath());
        KubeCMDClient.deleteFromFile(Environment.getInstance().namespace(), Paths.get(Environment.getInstance().getTemplatesPath(), "install", "components", "example-authservices", "standard-authservice.yaml"));
        KubeCMDClient.deleteFromFile(Environment.getInstance().namespace(), Paths.get(Environment.getInstance().getTemplatesPath(), "install", "components", "example-authservices", "none-authservice.yaml"));
    }

    public void removeServiceCatalog() {
        LOGGER.info("Delete enmasse service catalog from: {}", Environment.getInstance().getTemplatesPath());
        KubeCMDClient.deleteFromFile(Environment.getInstance().namespace(), Paths.get(Environment.getInstance().getTemplatesPath(), "install", "components", "service-broker"));
        KubeCMDClient.deleteFromFile(Environment.getInstance().namespace(), Paths.get(Environment.getInstance().getTemplatesPath(), "install", "components", "cluster-service-broker"));
    }

    public void removeIoTOperator() {
        LOGGER.info("Delete enmasse IoT operator from: {}", Environment.getInstance().getTemplatesPath());
        KubeCMDClient.deleteFromFile(Environment.getInstance().namespace(), Paths.get(Environment.getInstance().getTemplatesPath(), "install", "preview-bundles", "iot"));
    }

    public void clean() throws Exception {
        KubeCMDClient.runOnCluster("delete", "clusterrolebindings", "-l", "app=enmasse");
        KubeCMDClient.runOnCluster("delete", "crd", "-l", "app=enmasse");
        KubeCMDClient.runOnCluster("delete", "clusterroles", "-l", "app=enmasse");
        KubeCMDClient.runOnCluster("delete", "apiservices", "-l", "app=enmasse");
        KubeCMDClient.runOnCluster("delete", "oauthclients", "-l", "app=enmasse");
        KubeCMDClient.runOnCluster("delete", "clusterservicebrokers", "-l", "app=enmasse");
        kube.deleteNamespace(Environment.getInstance().namespace());
    }

    public void waithUntilOperatorReady() throws Exception {
        Thread.sleep(5000);
        TestUtils.waitUntilDeployed(Environment.getInstance().namespace());

        if (kube instanceof OpenShift) {
            // Kubernetes does not make a console service available by default.
            awaitConsoleReadiness();
        }
    }

    private void awaitConsoleReadiness() throws Exception {
        final String serviceName = "console";

        TestUtils.waitUntilCondition("global console readiness", waitPhase -> {
            try {
                final ConsoleService console = kube.getConsoleServiceClient().withName("console").get();
                if (console == null) {
                    LOGGER.info("ConsoleService {} not yet available", serviceName);
                    return false;
                }

                final ConsoleServiceSpec spec = console.getSpec();
                final boolean ready = spec != null && spec.getOauthClientSecret() != null && spec.getSsoCookieSecret() != null;
                if (!ready) {
                    LOGGER.info("ConsoleService {} not yet fully ready: {}", serviceName, spec);
                }
                return ready;
            } catch (KubernetesClientException e) {
                LOGGER.warn("Failed to get console service record : {}", serviceName, e);
            }

            return false;
        }, new TimeoutBudget(3, TimeUnit.MINUTES));
    }

    public boolean isEnmasseBundleDeployed() {
        return kube.namespaceExists(kube.getInfraNamespace())
                && kube.listPods(kube.getInfraNamespace()).stream().filter(pod -> pod.getMetadata().getName().contains("enmasse-operator")).count() == 1;
    }

    public boolean isIoTOperatorDeployed() {
        return kube.getCRD("iotprojects.iot.enmasse.io") != null
                && kube.getCRD("iotconfigs.iot.enmasse.io") != null
                && kube.getServiceAccount(Environment.getInstance().namespace(), "iot-operator") != null;
    }
}
