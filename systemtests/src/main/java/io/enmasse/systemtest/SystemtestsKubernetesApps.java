/*
 * Copyright 2016-2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package io.enmasse.systemtest;

import io.enmasse.systemtest.utils.TestUtils;
import io.fabric8.kubernetes.api.model.*;
import io.fabric8.kubernetes.api.model.apps.Deployment;
import io.fabric8.kubernetes.api.model.apps.DeploymentBuilder;
import io.fabric8.kubernetes.api.model.extensions.*;

import java.io.File;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Base64;
import java.util.concurrent.TimeUnit;

public class SystemtestsKubernetesApps {
    public static final String MESSAGING_CLIENTS = "systemtests-clients";
    public static final String SELENIUM_FIREFOX = "selenium-firefox";
    public static final String SELENIUM_CHROME = "selenium-chrome";
    public static final String SELENIUM_PROJECT = "systemtests-selenium";
    public static final String MESSAGING_PROJECT = "systemtests-clients";
    public static final String SELENIUM_CONFIG_MAP = "rhea-configmap";
    public static final String OPENSHIFT_CERT_VALIDATOR = "systemtests-cert-validator";
    public static final String POSTGRES_APP = "postgres-app";
    public static final String INFINISPAN_SERVER = "infinispan-server";

    public static String getMessagingAppPodName() throws Exception {
        TestUtils.waitUntilCondition("Pod is reachable", waitPhase -> Kubernetes.getInstance().listPods(MESSAGING_PROJECT).stream().filter(pod -> pod.getMetadata().getName().contains(MESSAGING_CLIENTS) &&
                pod.getStatus().getContainerStatuses().get(0).getReady()).count() == 1, new TimeoutBudget(1, TimeUnit.MINUTES));
        return Kubernetes.getInstance().listPods(MESSAGING_PROJECT).stream().filter(pod -> pod.getMetadata().getName().contains(MESSAGING_CLIENTS) &&
                pod.getStatus().getContainerStatuses().get(0).getReady()).findAny().get().getMetadata().getName();
    }

    public static String deployMessagingClientApp() throws Exception {
        if (!Kubernetes.getInstance().namespaceExists(MESSAGING_PROJECT)) {
            Kubernetes.getInstance().createNamespace(MESSAGING_PROJECT);
        }
        Kubernetes.getInstance().createDeploymentFromResource(MESSAGING_PROJECT, getMessagingAppDeploymentResource());
        TestUtils.waitForExpectedReadyPods(Kubernetes.getInstance(), MESSAGING_PROJECT, 1, new TimeoutBudget(1, TimeUnit.MINUTES));
        return getMessagingAppPodName();
    }

    public static void deleteMessagingClientApp() {
        if (Kubernetes.getInstance().deploymentExists(MESSAGING_PROJECT, MESSAGING_CLIENTS)) {
            Kubernetes.getInstance().deleteDeployment(MESSAGING_PROJECT, MESSAGING_CLIENTS);
        }
    }

    public static void deployOpenshiftCertValidator(String namespace, Kubernetes kubeClient) throws Exception {
        if (!kubeClient.namespaceExists(namespace)) {
            kubeClient.createNamespace(namespace);
        }
        kubeClient.createServiceFromResource(namespace, getSystemtestsServiceResource(OPENSHIFT_CERT_VALIDATOR, 8080));
        kubeClient.createDeploymentFromResource(namespace, getOpenshiftCertValidatorDeploymentResource());
        kubeClient.createIngressFromResource(namespace, getSystemtestsIngressResource(OPENSHIFT_CERT_VALIDATOR, 8080));
        Thread.sleep(5000);
    }

    public static void deleteOpenshiftCertValidator(String namespace, Kubernetes kubeClient) {
        if (kubeClient.deploymentExists(namespace, OPENSHIFT_CERT_VALIDATOR)) {
            kubeClient.deleteDeployment(namespace, OPENSHIFT_CERT_VALIDATOR);
            kubeClient.deleteService(namespace, OPENSHIFT_CERT_VALIDATOR);
            kubeClient.deleteIngress(namespace, OPENSHIFT_CERT_VALIDATOR);
        }
    }

    public static void deployFirefoxSeleniumApp(String namespace, Kubernetes kubeClient) throws Exception {
        if (!kubeClient.namespaceExists(namespace)) {
            kubeClient.createNamespace(namespace);
        }
        kubeClient.createServiceFromResource(namespace, getSystemtestsServiceResource(SystemtestsKubernetesApps.SELENIUM_FIREFOX, 4444));
        kubeClient.createConfigmapFromResource(namespace, getRheaConfigMap());
        kubeClient.createDeploymentFromResource(namespace,
                getSeleniumNodeDeploymentResource(SELENIUM_FIREFOX, "selenium/standalone-firefox"));
        kubeClient.createIngressFromResource(namespace, getSystemtestsIngressResource(SELENIUM_FIREFOX, 4444));
        Thread.sleep(5000);
    }

    public static void deployChromeSeleniumApp(String namespace, Kubernetes kubeClient) throws Exception {
        if (!kubeClient.namespaceExists(namespace))
            kubeClient.createNamespace(namespace);
        kubeClient.createServiceFromResource(namespace, getSystemtestsServiceResource(SELENIUM_CHROME, 4444));
        kubeClient.createConfigmapFromResource(namespace, getRheaConfigMap());
        kubeClient.createDeploymentFromResource(namespace,
                getSeleniumNodeDeploymentResource(SystemtestsKubernetesApps.SELENIUM_CHROME, "selenium/standalone-chrome"));
        kubeClient.createIngressFromResource(namespace, getSystemtestsIngressResource(SELENIUM_CHROME, 4444));
        Thread.sleep(5000);
    }

    public static void deleteFirefoxSeleniumApp(String namespace, Kubernetes kubeClient) throws Exception {
        kubeClient.deleteDeployment(namespace, SELENIUM_FIREFOX);
        kubeClient.deleteService(namespace, SELENIUM_FIREFOX);
        kubeClient.deleteIngress(namespace, SELENIUM_FIREFOX);
        kubeClient.deleteConfigmap(namespace, SELENIUM_CONFIG_MAP);
    }

    public static void deleteChromeSeleniumApp(String namespace, Kubernetes kubeClient) throws Exception {
        kubeClient.deleteDeployment(namespace, SELENIUM_CHROME);
        kubeClient.deleteService(namespace, SELENIUM_CHROME);
        kubeClient.deleteIngress(namespace, SELENIUM_CHROME);
        kubeClient.deleteConfigmap(namespace, SELENIUM_CONFIG_MAP);
    }

    public static void deleteSeleniumPod(String namespace, Kubernetes kubeClient) {
        kubeClient.listPods(namespace).forEach(pod -> kubeClient.deletePod(namespace, pod.getMetadata().getName()));
    }

    public static Endpoint getFirefoxSeleniumAppEndpoint(Kubernetes kubeClient) {
        return new Endpoint(kubeClient.getIngressHost(SELENIUM_PROJECT, SELENIUM_FIREFOX), 80);
    }

    public static Endpoint getChromeSeleniumAppEndpoint(Kubernetes kubeClient) {
        return new Endpoint(kubeClient.getIngressHost(SELENIUM_PROJECT, SELENIUM_CHROME), 80);
    }

    public static Endpoint getOpenshiftCertValidatorEndpoint(String namespace, Kubernetes kubeClient) {
        return new Endpoint(kubeClient.getIngressHost(namespace, OPENSHIFT_CERT_VALIDATOR), 80);
    }

    public static Endpoint deployPostgresDB(String namespace) throws Exception {
        Kubernetes kubeCli = Kubernetes.getInstance();
        kubeCli.createSecret(namespace, getPostgresSecret());
        kubeCli.createServiceFromResource(namespace, getSystemtestsServiceResource(POSTGRES_APP, 5432));
        kubeCli.createPvc(namespace, getPostgresPVC());
        kubeCli.createConfigmapFromResource(namespace, getPostgresConfigMap());
        kubeCli.createDeploymentFromResource(namespace, getPostgresDeployment());
        return kubeCli.getEndpoint(POSTGRES_APP, namespace, "http");
    }

    public static void deletePostgresDB(String namespace) {
        Kubernetes kubeCli = Kubernetes.getInstance();
        if (kubeCli.deploymentExists(namespace, POSTGRES_APP)) {
            kubeCli.deleteService(namespace, POSTGRES_APP);
            kubeCli.deletePvc(namespace, POSTGRES_APP);
            kubeCli.deleteConfigmap(namespace, POSTGRES_APP);
            kubeCli.deleteDeployment(namespace, POSTGRES_APP);
            kubeCli.deleteSecret(namespace, POSTGRES_APP);
        }
    }

    public static Endpoint deployInfinispanServer(String namespace) throws Exception {
        Kubernetes kubeCli = Kubernetes.getInstance();
        kubeCli.createServiceFromResource(namespace, getSystemtestsServiceResource(INFINISPAN_SERVER, 11222));
        kubeCli.createDeploymentFromResource(namespace, getInfinispanDeployment());
        return kubeCli.getEndpoint(INFINISPAN_SERVER, namespace, "http");
    }

    public static void deleteInfinispanServer(String namespace) {
        Kubernetes kubeCli = Kubernetes.getInstance();
        if (kubeCli.deploymentExists(namespace, INFINISPAN_SERVER)) {
            kubeCli.deleteService(namespace, INFINISPAN_SERVER);
            kubeCli.deleteDeployment(namespace, INFINISPAN_SERVER);
        }
    }

    ////////////////////////////////////////////////////////////////////////////////////////////
    // Resources
    ////////////////////////////////////////////////////////////////////////////////////////////

    private static Deployment getSeleniumNodeDeploymentResource(String appName, String imageName) {
        return new DeploymentBuilder()
                .withNewMetadata()
                .withName(appName)
                .endMetadata()
                .withNewSpec()
                .withNewSelector()
                .addToMatchLabels("app", appName)
                .endSelector()
                .withReplicas(1)
                .withNewTemplate()
                .withNewMetadata()
                .addToLabels("app", appName)
                .endMetadata()
                .withNewSpec()
                .addNewContainer()
                .withName(appName)
                .withImage(imageName)
                .addNewPort()
                .withContainerPort(4444)
                .endPort()
                .addNewVolumeMount()
                .withName(SELENIUM_CONFIG_MAP)
                .withMountPath("/opt/rhea/")
                .endVolumeMount()
                .withNewLivenessProbe()
                .withNewHttpGet()
                .withPath("/wd/hub")
                .withNewPort(4444)
                .endHttpGet()
                .withInitialDelaySeconds(10)
                .endLivenessProbe()
                .endContainer()
                .addNewVolume()
                .withName(SELENIUM_CONFIG_MAP)
                .withNewConfigMap()
                .withName(SELENIUM_CONFIG_MAP)
                .endConfigMap()
                .endVolume()
                .endSpec()
                .endTemplate()
                .endSpec()
                .build();
    }

    private static Deployment getMessagingAppDeploymentResource() {
        return new DeploymentBuilder()
                .withNewMetadata()
                .withName(MESSAGING_CLIENTS)
                .endMetadata()
                .withNewSpec()
                .withNewSelector()
                .addToMatchLabels("app", MESSAGING_CLIENTS)
                .endSelector()
                .withReplicas(1)
                .withNewTemplate()
                .withNewMetadata()
                .addToLabels("app", MESSAGING_CLIENTS)
                .endMetadata()
                .withNewSpec()
                .addNewContainer()
                .withName(MESSAGING_CLIENTS)
                .withImage("quay.io/enmasse/systemtests-clients:latest")
                .withCommand("sleep")
                .withArgs("infinity")
                .endContainer()
                .endSpec()
                .endTemplate()
                .endSpec()
                .build();
    }

    private static Service getSystemtestsServiceResource(String appName, int port) {
        return new ServiceBuilder()
                .withNewMetadata()
                .withName(appName)
                .addToLabels("run", appName)
                .endMetadata()
                .withNewSpec()
                .addToSelector("app", appName)
                .addNewPort()
                .withName("http")
                .withPort(port)
                .withProtocol("TCP")
                .endPort()
                .endSpec()
                .build();
    }

    private static Ingress getSystemtestsIngressResource(String appName, int port) throws Exception {
        Environment env = Environment.getInstance();
        IngressBackend backend = new IngressBackend();
        backend.setServiceName(appName);
        backend.setServicePort(new IntOrString(port));
        HTTPIngressPath path = new HTTPIngressPath();
        path.setPath("/");
        path.setBackend(backend);

        return new IngressBuilder()
                .withNewMetadata()
                .withName(appName)
                .addToLabels("route", appName)
                .endMetadata()
                .withNewSpec()
                .withRules(new IngressRuleBuilder()
                        .withHost(appName + "." + (env.kubernetesDomain().equals("nip.io") ? new URL(Environment.getInstance().getApiUrl()).getHost() + ".nip.io" : env.kubernetesDomain()))
                        .withNewHttp()
                        .withPaths(path)
                        .endHttp()
                        .build())
                .endSpec()
                .build();
    }

    private static ConfigMap getRheaConfigMap() throws Exception {
        File rheaHtml = new File("src/main/resources/rhea.html");
        File rheaJs = new File("src/main/resources/rhea.js");
        String htmlContent = Files.readString(rheaHtml.toPath());
        String jsContent = Files.readString(rheaJs.toPath());

        return new ConfigMapBuilder()
                .withNewMetadata()
                .withName(SELENIUM_CONFIG_MAP)
                .endMetadata()
                .addToData("rhea.html", htmlContent)
                .addToData("rhea.js", jsContent)
                .build();
    }

    private static Deployment getOpenshiftCertValidatorDeploymentResource() {
        return new DeploymentBuilder()
                .withNewMetadata()
                .withName(OPENSHIFT_CERT_VALIDATOR)
                .endMetadata()
                .withNewSpec()
                .withNewSelector()
                .addToMatchLabels("app", OPENSHIFT_CERT_VALIDATOR)
                .endSelector()
                .withReplicas(1)
                .withNewTemplate()
                .withNewMetadata()
                .addToLabels("app", OPENSHIFT_CERT_VALIDATOR)
                .endMetadata()
                .withNewSpec()
                .addNewContainer()
                .withName(OPENSHIFT_CERT_VALIDATOR)
                .withImage("enmasse/systemtests-cert-validator:latest")
                .addNewPort()
                .withContainerPort(8080)
                .endPort()
                .withNewLivenessProbe()
                .withNewTcpSocket()
                .withNewPort(8080)
                .endTcpSocket()
                .withInitialDelaySeconds(10)
                .withPeriodSeconds(5)
                .endLivenessProbe()
                .endContainer()
                .endSpec()
                .endTemplate()
                .endSpec()
                .build();
    }

    private static ConfigMap getPostgresConfigMap() {
        return new ConfigMapBuilder()
                .withNewMetadata()
                .withName(POSTGRES_APP)
                .addToLabels("app", POSTGRES_APP)
                .endMetadata()
                .addToData("POSTGRES_DB", "postgresdb")
                .addToData("POSTGRES_USER", "darthvader")
                .addToData("POSTGRES_PASSWORD", "anakinisdead")
                .addToData("PGDATA", "/var/lib/postgresql/data/pgdata")
                .build();
    }

    private static PersistentVolumeClaim getPostgresPVC() {
        return new PersistentVolumeClaimBuilder()
                .withNewMetadata()
                .withName(POSTGRES_APP)
                .addToLabels("app", POSTGRES_APP)
                .endMetadata()
                .withNewSpec()
                .withAccessModes("ReadWriteOnce")
                .withNewResources()
                .addToRequests("storage", new Quantity("5Gi"))
                .endResources()
                .endSpec()
                .build();
    }

    private static Deployment getPostgresDeployment() {
        return new DeploymentBuilder()
                .withNewMetadata()
                .withName(POSTGRES_APP)
                .addToLabels("app", POSTGRES_APP)
                .addToLabels("template", POSTGRES_APP)
                .endMetadata()
                .withNewSpec()
                .withNewSelector()
                .addToMatchLabels("app", POSTGRES_APP)
                .endSelector()
                .withReplicas(1)
                .withNewTemplate()
                .withNewMetadata()
                .addToLabels("app", POSTGRES_APP)
                .endMetadata()
                .withNewSpec()
                .addNewContainer()
                .withName(POSTGRES_APP)
                .withImage("postgres:10.4")
                .withImagePullPolicy("IfNotPresent")
                .addNewPort()
                .withContainerPort(5432)
                .endPort()
                .withEnvFrom(new EnvFromSourceBuilder()
                        .withNewConfigMapRef()
                        .withName(POSTGRES_APP)
                        .endConfigMapRef()
                        .build())
                .withVolumeMounts(new VolumeMountBuilder()
                        .withMountPath("/var/lib/postgresql/data/")
                        .withName(POSTGRES_APP).build())
                .endContainer()
                .addNewVolume()
                .withName(POSTGRES_APP)
                .withNewPersistentVolumeClaim()
                .withClaimName(POSTGRES_APP)
                .endPersistentVolumeClaim()
                .endVolume()
                .endSpec()
                .endTemplate()
                .endSpec()
                .build();
    }

    private static Secret getPostgresSecret() {
        return new SecretBuilder()
                .withNewMetadata()
                .withName(POSTGRES_APP)
                .endMetadata()
                .addToData("database-user", Base64.getEncoder().encodeToString("darthvader".getBytes(StandardCharsets.UTF_8)))
                .addToData("database-password", Base64.getEncoder().encodeToString("anakinisdead".getBytes(StandardCharsets.UTF_8)))
                .build();
    }

    private static Deployment getInfinispanDeployment() {
        return new DeploymentBuilder()
                .withNewMetadata()
                .withName(INFINISPAN_SERVER)
                .addToLabels("app", INFINISPAN_SERVER)
                .addToLabels("template", INFINISPAN_SERVER)
                .endMetadata()
                .withNewSpec()
                .withNewSelector()
                .addToMatchLabels("app", INFINISPAN_SERVER)
                .endSelector()
                .withReplicas(1)
                .withNewTemplate()
                .withNewMetadata()
                .addToLabels("app", INFINISPAN_SERVER)
                .endMetadata()
                .withNewSpec()
                .addNewContainer()
                .withName(INFINISPAN_SERVER)
                .withImage("jboss/infinispan-server:9.4.11.Final")
                .withImagePullPolicy("IfNotPresent")
                .endContainer()
                .endSpec()
                .endTemplate()
                .endSpec()
                .build();
    }
}
