/*
 * Copyright 2016-2019, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package io.enmasse.systemtest.platform.apps;

import io.enmasse.systemtest.Endpoint;
import io.enmasse.systemtest.Environment;
import io.enmasse.systemtest.certs.BrokerCertBundle;
import io.enmasse.systemtest.logs.CustomLogger;
import io.enmasse.systemtest.logs.GlobalLogCollector;
import io.enmasse.systemtest.manager.IsolatedResourcesManager;
import io.enmasse.systemtest.platform.Kubernetes;
import io.enmasse.systemtest.time.TimeoutBudget;
import io.enmasse.systemtest.utils.TestUtils;
import io.fabric8.kubernetes.api.model.ConfigMap;
import io.fabric8.kubernetes.api.model.ConfigMapBuilder;
import io.fabric8.kubernetes.api.model.ContainerBuilder;
import io.fabric8.kubernetes.api.model.ContainerPortBuilder;
import io.fabric8.kubernetes.api.model.EnvFromSourceBuilder;
import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.IntOrString;
import io.fabric8.kubernetes.api.model.Namespace;
import io.fabric8.kubernetes.api.model.NamespaceBuilder;
import io.fabric8.kubernetes.api.model.PersistentVolumeClaim;
import io.fabric8.kubernetes.api.model.PersistentVolumeClaimBuilder;
import io.fabric8.kubernetes.api.model.Quantity;
import io.fabric8.kubernetes.api.model.Secret;
import io.fabric8.kubernetes.api.model.SecretBuilder;
import io.fabric8.kubernetes.api.model.Service;
import io.fabric8.kubernetes.api.model.ServiceBuilder;
import io.fabric8.kubernetes.api.model.ServicePort;
import io.fabric8.kubernetes.api.model.ServicePortBuilder;
import io.fabric8.kubernetes.api.model.VolumeBuilder;
import io.fabric8.kubernetes.api.model.VolumeMountBuilder;
import io.fabric8.kubernetes.api.model.apps.Deployment;
import io.fabric8.kubernetes.api.model.apps.DeploymentBuilder;
import io.fabric8.kubernetes.api.model.apps.DoneableDeployment;
import io.fabric8.kubernetes.api.model.extensions.HTTPIngressPath;
import io.fabric8.kubernetes.api.model.extensions.Ingress;
import io.fabric8.kubernetes.api.model.extensions.IngressBackend;
import io.fabric8.kubernetes.api.model.extensions.IngressBuilder;
import io.fabric8.kubernetes.api.model.extensions.IngressRuleBuilder;
import io.fabric8.kubernetes.api.model.rbac.PolicyRuleBuilder;
import io.fabric8.kubernetes.api.model.rbac.RoleBindingBuilder;
import io.fabric8.kubernetes.api.model.rbac.RoleBuilder;
import io.fabric8.kubernetes.api.model.rbac.SubjectBuilder;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.dsl.Applicable;
import io.fabric8.kubernetes.client.dsl.Deletable;
import io.fabric8.kubernetes.client.dsl.ParameterNamespaceListVisitFromServerGetDeleteRecreateWaitApplicable;
import io.fabric8.kubernetes.client.utils.ReplaceValueStream;
import org.slf4j.Logger;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Base64;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.function.Function;

public class SystemtestsKubernetesApps {
    private static Logger log = CustomLogger.getLogger();

    public static final String MESSAGING_CLIENTS = "systemtests-clients";
    public static final String SELENIUM_FIREFOX = "selenium-firefox";
    public static final String SELENIUM_CHROME = "selenium-chrome";
    public static final String SELENIUM_PROJECT = "systemtests-selenium";
    public static final String MESSAGING_PROJECT = "systemtests-clients";
    public static final String SELENIUM_CONFIG_MAP = "rhea-configmap";
    public static final String OPENSHIFT_CERT_VALIDATOR = "systemtests-cert-validator";
    public static final String POSTGRES_APP = "postgres-app";
    public static final String INFINISPAN_SERVER = "infinispan";
    private static final Path INFINISPAN_EXAMPLE_BASE = Paths.get("../templates/iot/examples/infinispan");

    public static String getMessagingAppPodName() throws Exception {
        return getMessagingAppPodName(MESSAGING_PROJECT);
    }

    public static String getMessagingAppPodName(String namespace) throws Exception {
        TestUtils.waitUntilCondition("Pod is reachable", waitPhase -> Kubernetes.getInstance().listPods(namespace).stream().filter(pod -> pod.getMetadata().getName().contains(namespace) &&
                pod.getStatus().getContainerStatuses().get(0).getReady()).count() == 1, new TimeoutBudget(1, TimeUnit.MINUTES));

        return Kubernetes.getInstance().listPods(namespace).stream().filter(pod -> pod.getMetadata().getName().contains(namespace) &&
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

    public static String deployMessagingClientApp(String namespace) throws Exception {
        if (!Kubernetes.getInstance().namespaceExists(namespace)) {
            if (namespace.equals("allowed-namespace")) {
                Namespace allowedNamespace = new NamespaceBuilder().withNewMetadata()
                        .withName("allowed-namespace").addToLabels("allowed", "true").endMetadata().build();
                Kubernetes.getInstance().getClient().namespaces().create(allowedNamespace);
            } else {
                Kubernetes.getInstance().createNamespace(namespace);
            }

        }
        Kubernetes.getInstance().createDeploymentFromResource(namespace, getMessagingAppDeploymentResource(namespace));
        TestUtils.waitForExpectedReadyPods(Kubernetes.getInstance(), namespace, 1, new TimeoutBudget(5, TimeUnit.MINUTES));
        return getMessagingAppPodName(namespace);
    }


    public static void collectMessagingClientAppLogs(Path path) {
        try {
            Files.createDirectories(path);
            GlobalLogCollector collector = new GlobalLogCollector(Kubernetes.getInstance(), path.toFile(), SystemtestsKubernetesApps.MESSAGING_PROJECT);
            collector.collectLogsOfPodsInNamespace(SystemtestsKubernetesApps.MESSAGING_PROJECT);
        } catch (Exception e) {
            log.error("Failed to collect pod logs from namespace : {}", SystemtestsKubernetesApps.MESSAGING_PROJECT);
        }
    }


    public static void deleteMessagingClientApp() throws Exception {
        if (Kubernetes.getInstance().deploymentExists(MESSAGING_PROJECT, MESSAGING_CLIENTS)) {
            Kubernetes.getInstance().deleteDeployment(MESSAGING_PROJECT, MESSAGING_CLIENTS);
        }
        Kubernetes.getInstance().deleteNamespace(SystemtestsKubernetesApps.MESSAGING_PROJECT);
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

    public static void deleteOpenshiftCertValidator(String namespace, Kubernetes kubeClient) throws Exception {
        if (kubeClient.deploymentExists(namespace, OPENSHIFT_CERT_VALIDATOR)) {
            kubeClient.deleteDeployment(namespace, OPENSHIFT_CERT_VALIDATOR);
            kubeClient.deleteService(namespace, OPENSHIFT_CERT_VALIDATOR);
            kubeClient.deleteIngress(namespace, OPENSHIFT_CERT_VALIDATOR);
        }
        kubeClient.deleteNamespace(namespace);
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

    private static Function<InputStream, InputStream> namespaceReplacer(final String namespace) {
        final Map<String, String> values = new HashMap<>();
        values.put("NAMESPACE", namespace);
        return in -> ReplaceValueStream.replaceValues(in, values);
    }

    public static Endpoint deployInfinispanServer(final String namespace) throws Exception {

        if (Environment.getInstance().isSkipDeployInfinispan()) {
            return getInfinispanEndpoint(namespace);
        }

        final Kubernetes kubeCli = Kubernetes.getInstance();
        final KubernetesClient client = kubeCli.getClient();

        // apply "common" and "manual" folders

        applyDirectories(namespaceReplacer(namespace),
                INFINISPAN_EXAMPLE_BASE.resolve("common"),
                INFINISPAN_EXAMPLE_BASE.resolve("manual"));

        // wait for the deployment

        client
                .apps().statefulSets()
                .inNamespace(namespace)
                .withName(INFINISPAN_SERVER)
                .waitUntilReady(5, TimeUnit.MINUTES);

        // return hotrod enpoint

        return getInfinispanEndpoint(namespace);
    }

    private static Endpoint getInfinispanEndpoint(final String namespace) {
        return Kubernetes.getInstance().getEndpoint(INFINISPAN_SERVER, namespace, "hotrod");
    }

    public static void deleteInfinispanServer(final String namespace) throws Exception {

        if (Environment.getInstance().isSkipDeployInfinispan()) {
            return;
        }

        // delete "common" and "manual" folders
        final Kubernetes kubeCli = Kubernetes.getInstance();
        final KubernetesClient client = kubeCli.getClient();

        if (client.apps()
                .statefulSets()
                .inNamespace(namespace)
                .withName(INFINISPAN_SERVER)
                .get() != null) {

            log.info("Infinispan server will be removed");

            deleteDirectories(namespaceReplacer(namespace),
                    INFINISPAN_EXAMPLE_BASE.resolve("common"),
                    INFINISPAN_EXAMPLE_BASE.resolve("manual"));

        }
    }

    public static void deployAMQBroker(String namespace, String name, String user, String password, BrokerCertBundle certBundle) throws Exception {
        Kubernetes kubeCli = Kubernetes.getInstance();
        if (!kubeCli.namespaceExists(namespace)) {
            kubeCli.createNamespace(namespace);
        }

        kubeCli.getClient().rbac().roles().inNamespace(namespace).createOrReplace(new RoleBuilder()
                .withNewMetadata()
                .withName(name)
                .withNamespace(namespace)
                .endMetadata()
                .withRules(new PolicyRuleBuilder()
                        .addToApiGroups("")
                        .addToResources("secrets")
                        .addToResourceNames(name)
                        .addToVerbs("get")
                        .build())
                .build());
        kubeCli.getClient().rbac().roleBindings().inNamespace(namespace).createOrReplace(new RoleBindingBuilder()
                .withNewMetadata()
                .withName(name)
                .withNamespace(namespace)
                .endMetadata()
                .withNewRoleRef("rbac.authorization.k8s.io", "Role", name)
                .withSubjects(new SubjectBuilder()
                        .withKind("ServiceAccount")
                        .withName("address-space-controller")
                        .withNamespace(kubeCli.getInfraNamespace())
                        .build())
                .build());

        kubeCli.createSecret(namespace, getBrokerSecret(name, certBundle, user, password));

        kubeCli.createDeploymentFromResource(namespace, getBrokerDeployment(name, user, password));

        ServicePort tlsPort = new ServicePortBuilder()
                .withName("amqps")
                .withPort(5671)
                .withTargetPort(new IntOrString(5671))
                .build();

        ServicePort mutualTlsPort = new ServicePortBuilder()
                .withName("amqpsmutual")
                .withPort(55671)
                .withTargetPort(new IntOrString(55671))
                .build();

        Service service = getSystemtestsServiceResource(name, name, new ServicePortBuilder()
                        .withName("amqp")
                        .withPort(5672)
                        .withTargetPort(new IntOrString(5672))
                        .build(),
                tlsPort,
                mutualTlsPort);

        kubeCli.createServiceFromResource(namespace, service);

        kubeCli.createExternalEndpoint(name, namespace, service, tlsPort);

        kubeCli.getClient()
                .apps().deployments()
                .inNamespace(namespace)
                .withName(name)
                .waitUntilReady(5, TimeUnit.MINUTES);

        Thread.sleep(5000);
    }

    public static void deleteAMQBroker(String namespace, String name) throws Exception {
        Kubernetes kubeCli = Kubernetes.getInstance();
        kubeCli.getClient().rbac().roles().inNamespace(namespace).withName(name).cascading(true).delete();
        kubeCli.getClient().rbac().roleBindings().inNamespace(namespace).withName(name).cascading(true).delete();
        kubeCli.deleteSecret(namespace, name);
        kubeCli.deleteService(namespace, name);
        kubeCli.deleteDeployment(namespace, name);
        kubeCli.deleteExternalEndpoint(namespace, name);
        Thread.sleep(5000);
    }

    public static void scaleDownDeployment(String namespace, String name) throws Exception {
        Kubernetes kubeCli = Kubernetes.getInstance();
        kubeCli.setDeploymentReplicas(namespace, name, 0);
        TestUtils.waitForExpectedReadyPods(kubeCli, namespace, 0, new TimeoutBudget(30, TimeUnit.SECONDS));
    }

    public static void scaleUpDeployment(String namespace, String name) throws Exception {
        Kubernetes kubeCli = Kubernetes.getInstance();
        kubeCli.setDeploymentReplicas(namespace, name, 1);
        TestUtils.waitForExpectedReadyPods(kubeCli, namespace, 1, new TimeoutBudget(30, TimeUnit.SECONDS));
    }

    public static void applyDirectories(final Function<InputStream, InputStream> streamManipulator, final Path... paths) throws Exception {
        loadDirectories(streamManipulator, Applicable::createOrReplace, paths);
    }

    public static void deleteDirectories(final Function<InputStream, InputStream> streamManipulator, final Path... paths) throws Exception {
        loadDirectories(streamManipulator, o -> {
            o.fromServer().get().forEach(item -> {
                // Workaround for https://github.com/fabric8io/kubernetes-client/issues/1856
                Kubernetes.getInstance().getClient().resource(item).cascading(true).delete();
            });
        }, paths);
    }

    public static void loadDirectories(final Function<InputStream, InputStream> streamManipulator, Consumer<ParameterNamespaceListVisitFromServerGetDeleteRecreateWaitApplicable<HasMetadata, Boolean>> consumer, final Path... paths) throws Exception {
        for (Path path : paths) {
            loadDirectory(streamManipulator, consumer, path);
        }
    }

    public static void loadDirectory(final Function<InputStream, InputStream> streamManipulator, Consumer<ParameterNamespaceListVisitFromServerGetDeleteRecreateWaitApplicable<HasMetadata, Boolean>> consumer, final Path path) throws Exception {

        final Kubernetes kubeCli = Kubernetes.getInstance();
        final KubernetesClient client = kubeCli.getClient();

        log.info("Loading resources from: {}", path);

        Files.walkFileTree(path, new SimpleFileVisitor<>() {
            public FileVisitResult visitFile(final Path file, final BasicFileAttributes attrs) throws IOException {

                log.debug("Found: {}", file);

                if (!Files.isRegularFile(file)) {
                    log.debug("File is not a regular file: {}", file);
                    return FileVisitResult.CONTINUE;
                }

                if (!file.getFileName().toString().endsWith(".yaml")) {
                    log.info("Skipping file: does not end with '.yaml': {}", file);
                    return FileVisitResult.CONTINUE;
                }

                log.info("Processing: {}", file);

                try (InputStream f = Files.newInputStream(file)) {

                    final InputStream in;
                    if (streamManipulator != null) {
                        in = streamManipulator.apply(f);
                    } else {
                        in = f;
                    }

                    if (in != null) {
                        consumer.accept(client.load(in));
                    }
                }

                return FileVisitResult.CONTINUE;
            }
        });

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

    private static Deployment getMessagingAppDeploymentResource(String namespace) {
        return new DeploymentBuilder(getMessagingAppDeploymentResource())
                .editMetadata()
                .withName(namespace)
                .endMetadata()
                .editSpec()
                .withNewSelector()
                .addToMatchLabels("app", namespace)
                .endSelector()
                .editTemplate()
                .withNewMetadata()
                .addToLabels("app", namespace)
                .endMetadata()
                .endTemplate()
                .endSpec()
                .build();

       /* return new DoneableDeployment(getMessagingAppDeploymentResource())
            .editMetadata()
                .withName(namespace)
            .endMetadata()
            .editSpec()
             .withNewSelector()
             .addToMatchLabels("app", namespace)
             .endSelector()
             .editTemplate()
             .withNewMetadata()
             .addToLabels("app", namespace)
             .endMetadata()
             .endTemplate()
             .endSpec()
             .done();*/
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

    private static Service getSystemtestsServiceResource(String serviceName, String appName, ServicePort... ports) {
        return new ServiceBuilder()
                .withNewMetadata()
                .withName(serviceName)
                .addToLabels("run", appName)
                .endMetadata()
                .withNewSpec()
                .addToPorts(ports)
                .addToSelector("app", appName)
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
                .withImage("quay.io/enmasse/systemtests-cert-validator:latest")
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

    private static Secret getBrokerSecret(String name, BrokerCertBundle certBundle, String user, String password) throws Exception {
        Map<String, String> data = new HashMap<>();

        byte[] content = Files.readAllBytes(new File("src/main/resources/broker/broker.xml").toPath());
        data.put("broker.xml", Base64.getEncoder().encodeToString(content));

        data.put("broker.ks", Base64.getEncoder().encodeToString(certBundle.getKeystore()));
        data.put("broker.ts", Base64.getEncoder().encodeToString(certBundle.getTruststore()));
        data.put("ca.crt", Base64.getEncoder().encodeToString(certBundle.getCaCert()));

        return new SecretBuilder()
                .withNewMetadata()
                .withName(name)
                .endMetadata()
                .addToData(data)
                .addToStringData("user", user)
                .addToStringData("password", password)
                .build();
    }

    private static Deployment getBrokerDeployment(String name, String user, String password) {
        return new DeploymentBuilder()
                .withNewMetadata()
                .withName(name)
                .addToLabels("app", name)
                .endMetadata()
                .withNewSpec()
                .withNewSelector()
                .addToMatchLabels("app", name)
                .endSelector()
                .withReplicas(1)
                .withNewTemplate()
                .withNewMetadata()
                .addToLabels("app", name)
                .endMetadata()
                .withNewSpec()
                .addToInitContainers(new ContainerBuilder()
                                .withName("artemis-init")
                                .withImage("quay.io/enmasse/artemis-base:2.10.0")
                                .withCommand("/bin/sh")
                                .withArgs("-c", "/opt/apache-artemis/bin/artemis create /var/run/artemis --allow-anonymous --force --user " + user + " --password " + password + " --role admin")
                                .withVolumeMounts(new VolumeMountBuilder()
                                                .withName("data")
                                                .withMountPath("/var/run/artemis")
                                                .build(),
                                        new VolumeMountBuilder()
                                                .withName(name)
                                                .withMountPath("/etc/amq-secret-volume")
                                                .build())
                                .build(),
                        new ContainerBuilder()
                                .withName("replace-broker-xml")
                                .withImage("quay.io/enmasse/artemis-base:2.10.0")
                                .withCommand("/bin/sh")
                                .withArgs("-c", "cp /etc/amq-secret-volume/broker.xml /var/run/artemis/etc/broker.xml")
                                .withVolumeMounts(new VolumeMountBuilder()
                                                .withName("data")
                                                .withMountPath("/var/run/artemis")
                                                .build(),
                                        new VolumeMountBuilder()
                                                .withName(name)
                                                .withMountPath("/etc/amq-secret-volume")
                                                .build())
                                .build())
                .addNewContainer()
                .withName(name)
                .withImage("quay.io/enmasse/artemis-base:2.10.0")
                .withImagePullPolicy("IfNotPresent")
                .withCommand("/bin/sh")
                .withArgs("-c", "/var/run/artemis/bin/artemis run")
                .addToPorts(new ContainerPortBuilder()
                                .withContainerPort(5672)
                                .withName("amqp")
                                .build(),
                        new ContainerPortBuilder()
                                .withContainerPort(5671)
                                .withName("amqps")
                                .build(),
                        new ContainerPortBuilder()
                                .withContainerPort(55671)
                                .withName("amqpsmutual")
                                .build())
                .withVolumeMounts(new VolumeMountBuilder()
                                .withName("data")
                                .withMountPath("/var/run/artemis")
                                .build(),
                        new VolumeMountBuilder()
                                .withName(name)
                                .withMountPath("/etc/amq-secret-volume")
                                .build())
                .endContainer()
                .addToVolumes(new VolumeBuilder()
                                .withName("data")
                                .withNewEmptyDir()
                                .endEmptyDir()
                                .build(),
                        new VolumeBuilder()
                                .withName(name)
                                .withNewSecret()
                                .withSecretName(name)
                                .endSecret()
                                .build())
                .endSpec()
                .endTemplate()
                .endSpec()
                .build();
    }

}