/*
 * Copyright 2016-2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package io.enmasse.systemtest;

import io.fabric8.kubernetes.api.model.*;
import io.fabric8.kubernetes.api.model.apps.Deployment;
import io.fabric8.kubernetes.api.model.apps.DeploymentBuilder;
import io.fabric8.kubernetes.api.model.extensions.*;

import java.io.File;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Base64;

public class SystemtestsKubernetesApps {
    public static final String MESSAGING_CLIENTS = "messaging-clients";
    public static final String SELENIUM_FIREFOX = "selenium-firefox";
    public static final String SELENIUM_CHROME = "selenium-chrome";
    public static final String SELENIUM_PROJECT = "selenium";
    public static final String SELENIUM_CONFIG_MAP = "rhea-configmap";
    public static final String OPENSHIFT_CERT_VALIDATOR = "openshift-cert-validator";
    public static final String POSTGRES_APP = "postgres-app";

    public static void deployMessagingClientApp(String namespace, Kubernetes kubeClient) throws Exception {
        kubeClient.createServiceFromResource(namespace, getSystemtestsServiceResource(MESSAGING_CLIENTS, 4242));
        kubeClient.createDeploymentFromResource(namespace, getMessagingAppDeploymentResource());
        kubeClient.createIngressFromResource(namespace, getSystemtestsIngressResource(MESSAGING_CLIENTS, 4242));
        Thread.sleep(5000);
    }

    public static void deleteMessagingClientApp(String namespace, Kubernetes kubeClient) {
        if (kubeClient.deploymentExists(namespace, MESSAGING_CLIENTS)) {
            kubeClient.deleteDeployment(namespace, MESSAGING_CLIENTS);
            kubeClient.deleteService(namespace, MESSAGING_CLIENTS);
            kubeClient.deleteIngress(namespace, MESSAGING_CLIENTS);
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

    public static Endpoint getMessagingClientEndpoint(String namespace, Kubernetes kubeClient) {
        return new Endpoint(kubeClient.getIngressHost(namespace, MESSAGING_CLIENTS), 80);
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
        return kubeCli.getEndpoint(POSTGRES_APP, "http");
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
                .withImage("kornysd/docker-clients:1.2")
                .addNewPort()
                .withContainerPort(4242)
                .endPort()
                .withNewLivenessProbe()
                .withNewTcpSocket()
                .withNewPort(4242)
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
        File rheaJs = new File("client_executable/rhea/dist/rhea.js");
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
                .withImage("famargon/openshift-cert-validator:latest")
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
                .withAccessModes("ReadWriteMany")
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
}
