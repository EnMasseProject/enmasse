/*
 * Copyright 2016-2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package io.enmasse.systemtest.platform.apps;

import io.enmasse.systemtest.Endpoint;
import io.enmasse.systemtest.Environment;
import io.enmasse.systemtest.certs.BrokerCertBundle;
import io.enmasse.systemtest.framework.TestPlanInfo;
import io.enmasse.systemtest.framework.LoggerUtils;
import io.enmasse.systemtest.logs.GlobalLogCollector;
import io.enmasse.systemtest.platform.KubeCMDClient;
import io.enmasse.systemtest.platform.Kubernetes;
import io.enmasse.systemtest.time.TimeoutBudget;
import io.enmasse.systemtest.utils.TestUtils;
import io.fabric8.kubernetes.api.model.ConfigMap;
import io.fabric8.kubernetes.api.model.ConfigMapBuilder;
import io.fabric8.kubernetes.api.model.ConfigMapVolumeSourceBuilder;
import io.fabric8.kubernetes.api.model.ContainerBuilder;
import io.fabric8.kubernetes.api.model.ContainerPortBuilder;
import io.fabric8.kubernetes.api.model.ContainerState;
import io.fabric8.kubernetes.api.model.ContainerStateTerminated;
import io.fabric8.kubernetes.api.model.ContainerStatus;
import io.fabric8.kubernetes.api.model.DoneablePod;
import io.fabric8.kubernetes.api.model.EnvFromSourceBuilder;
import io.fabric8.kubernetes.api.model.EnvVarBuilder;
import io.fabric8.kubernetes.api.model.IntOrString;
import io.fabric8.kubernetes.api.model.Namespace;
import io.fabric8.kubernetes.api.model.NamespaceBuilder;
import io.fabric8.kubernetes.api.model.PersistentVolumeClaim;
import io.fabric8.kubernetes.api.model.PersistentVolumeClaimBuilder;
import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.api.model.PodBuilder;
import io.fabric8.kubernetes.api.model.PodCondition;
import io.fabric8.kubernetes.api.model.PodStatus;
import io.fabric8.kubernetes.api.model.Quantity;
import io.fabric8.kubernetes.api.model.Secret;
import io.fabric8.kubernetes.api.model.SecretBuilder;
import io.fabric8.kubernetes.api.model.SecretVolumeSourceBuilder;
import io.fabric8.kubernetes.api.model.Service;
import io.fabric8.kubernetes.api.model.ServiceBuilder;
import io.fabric8.kubernetes.api.model.ServicePort;
import io.fabric8.kubernetes.api.model.ServicePortBuilder;
import io.fabric8.kubernetes.api.model.VolumeBuilder;
import io.fabric8.kubernetes.api.model.VolumeMountBuilder;
import io.fabric8.kubernetes.api.model.apps.Deployment;
import io.fabric8.kubernetes.api.model.apps.DeploymentBuilder;
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
import io.fabric8.kubernetes.client.dsl.PodResource;
import io.fabric8.kubernetes.client.utils.ReplaceValueStream;
import io.vertx.core.json.JsonObject;
import org.junit.jupiter.api.Assertions;
import org.slf4j.Logger;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.Arrays;
import java.util.Base64;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Function;
import java.util.function.Predicate;

import static io.enmasse.systemtest.platform.Kubernetes.executeWithInput;
import static io.enmasse.systemtest.platform.Kubernetes.getClient;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.time.Duration.ofMinutes;
import static java.time.Duration.ofSeconds;

public class SystemtestsKubernetesApps {

    private static final Logger log = LoggerUtils.getLogger();

    private static final Kubernetes kube = Kubernetes.getInstance();
    private static final Environment env = Environment.getInstance();

    public static final Path TEMPLATES_ROOT = Paths.get(env.getTemplatesPath());

    //namespaces for apps
    public static final String MESSAGING_PROJECT = "systemtests-clients";
    public static final String SELENIUM_PROJECT = "systemtests-selenium";
    private static final String SCALE_TEST_CLIENTS_PROJECT = "systemtests-scale-test-clients";
    public static final String INFINISPAN_PROJECT = env.getInfinispanProject();
    public static final String POSTGRESQL_PROJECT = env.getPostgresqlProject();
    public static final String H2_PROJECT = env.getH2Project();
    /**
     * No need to add this namespace to ST_NAMESPACES, logs are already collected by process
     */
    public static final String CONTAINER_BUILDS_PROJECT = "systemtests-container-builds";

    //namespaces of ST apps (add namespace here in case you want to collect logs)
    public static final String[] ST_NAMESPACES = new String[] {
            INFINISPAN_PROJECT,
            POSTGRESQL_PROJECT,
            H2_PROJECT,
            SELENIUM_PROJECT
    };

    //messaging external clients
    public static final String MESSAGING_CLIENTS = "systemtests-clients";

    // api proxy
    public static final String API_PROXY = "api-proxy";

    //selenium
    public static final String SELENIUM_FIREFOX = "selenium-firefox";
    public static final String SELENIUM_CHROME = "selenium-chrome";
    public static final String SELENIUM_CONFIG_MAP = "rhea-configmap";

    //cert validator
    public static final String OPENSHIFT_CERT_VALIDATOR = "systemtests-cert-validator";

    //postgress for auth service
    public static final String POSTGRES_APP = "postgres-app";

    //scale tests
    private static final String SCALE_TEST_CLIENT = "scale-test-client";
    private static final String SCALE_TEST_CLIENT_ID_LABEL = "id";
    private static final String SCALE_TEST_CLIENT_TYPE_LABEL = "client";

    //infinispan iot
    public static final String INFINISPAN_SERVER = "infinispan";
    private static final Path INFINISPAN_EXAMPLE_BASE;
    private static final String[] INFINISPAN_OPENSHIFT;
    private static final String[] INFINISPAN_KUBERNETES;

    //postgress iot
    public static final String POSTGRESQL_SERVER = "postgresql";
    private static final Path POSTGRESQL_EXAMPLE_BASE;
    private static final Path[] POSTGRESQL_CREATE_TABLE_SQL;

    //h2 iot
    public static final String H2_SERVER = "h2";
    private static final Path H2_EXAMPLE_BASE;
    private static final Path H2_CREATE_SQL;
    public static final String[] H2_SHELL_COMMAND = new String[]{
            "java",
            "-cp",
            "h2.jar",
            "org.h2.tools.Shell",
            "-user",
            "admin",
            "-password",
            "admin1234",
            "-url",
            "jdbc:h2:tcp://localhost:9092//data/device-registry"
    };

    static {

        final Path examplesIoT = TEMPLATES_ROOT.resolve("install/components/iot/examples");

        INFINISPAN_EXAMPLE_BASE = examplesIoT.resolve("infinispan");
        INFINISPAN_OPENSHIFT = new String[]{
                "common",
                "openshift"
        };
        INFINISPAN_KUBERNETES = new String[]{
                "common",
                "kubernetes"
        };

        POSTGRESQL_EXAMPLE_BASE = examplesIoT.resolve("postgresql/deploy");
        POSTGRESQL_CREATE_TABLE_SQL = new Path[]{
                examplesIoT.resolve("postgresql/create.devcon.sql"),
                examplesIoT.resolve("postgresql/create.sql")
        };

        H2_EXAMPLE_BASE = examplesIoT.resolve("h2/deploy");
        H2_CREATE_SQL = examplesIoT.resolve("h2/create.sql");
    }

    public static String getMessagingAppPodName() throws Exception {
        return getMessagingAppPodName(MESSAGING_PROJECT);
    }

    public static String getMessagingAppPodName(String namespace) throws Exception {
        TestUtils.waitUntilCondition("Pod is reachable",
                waitPhase -> kube.listPods(namespace).stream().filter(pod -> pod.getMetadata().getName().contains(namespace) &&
                        pod.getStatus().getContainerStatuses().get(0).getReady()).count() == 1,
                new TimeoutBudget(1, TimeUnit.MINUTES));
        return kube.listPods(namespace).stream().filter(pod -> pod.getMetadata().getName().contains(namespace) &&
                pod.getStatus().getContainerStatuses().get(0).getReady()).findAny().get().getMetadata().getName();
    }

    public static String deployMessagingClientApp() throws Exception {
        kube.createNamespace(MESSAGING_PROJECT);
        kube.createDeploymentFromResource(MESSAGING_PROJECT, getMessagingAppDeploymentResource());
        TestUtils.waitForExpectedReadyPods(kube, MESSAGING_PROJECT, 1, new TimeoutBudget(5, TimeUnit.MINUTES));
        return getMessagingAppPodName();
    }

    public static String deployMessagingClientApp(String namespace) throws Exception {
        if (!kube.namespaceExists(namespace)) {
            if (namespace.equals("allowed-namespace")) {
                Namespace allowedNamespace = new NamespaceBuilder().withNewMetadata()
                        .withName("allowed-namespace").addToLabels("allowed", "true").endMetadata().build();
                getClient().namespaces().create(allowedNamespace);
            } else {
                kube.createNamespace(namespace);
            }

        }
        kube.createDeploymentFromResource(namespace, getMessagingAppDeploymentResource(namespace));
        TestUtils.waitForExpectedReadyPods(kube, namespace, 1, new TimeoutBudget(5, TimeUnit.MINUTES));
        return getMessagingAppPodName(namespace);
    }

    public static void collectMessagingClientAppLogs(Path path) {
        try {
            Files.createDirectories(path);
            GlobalLogCollector collector = new GlobalLogCollector(kube, path, SystemtestsKubernetesApps.MESSAGING_PROJECT);
            collector.collectLogsOfPodsInNamespace(SystemtestsKubernetesApps.MESSAGING_PROJECT);
            collector.collectRouterState("deleteMessagingClientApp");
        } catch (Exception e) {
            log.error("Failed to collect pod logs from namespace : {}", SystemtestsKubernetesApps.MESSAGING_PROJECT);
        }
    }

    public static void deleteMessagingClientApp() throws Exception {
        if (kube.deploymentExists(MESSAGING_PROJECT, MESSAGING_CLIENTS)) {
            kube.deleteDeployment(MESSAGING_PROJECT, MESSAGING_CLIENTS);
        }
        kube.deleteNamespace(SystemtestsKubernetesApps.MESSAGING_PROJECT);
    }

    public static void deployOpenshiftCertValidator(String namespace, Kubernetes kubeClient) throws Exception {
        kubeClient.createNamespace(namespace);
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
        kubeClient.createNamespace(namespace);
        kubeClient.createServiceFromResource(namespace, getSystemtestsServiceResource(SystemtestsKubernetesApps.SELENIUM_FIREFOX, 4444));
        kubeClient.createConfigmapFromResource(namespace, getRheaConfigMap());
        kubeClient.createDeploymentFromResource(namespace,
                getSeleniumNodeDeploymentResource(SELENIUM_FIREFOX, "selenium/standalone-firefox"));
        kubeClient.createIngressFromResource(namespace, getSystemtestsIngressResource(SELENIUM_FIREFOX, 4444));
        TestUtils.waitForExpectedReadyPods(kube, namespace, 1, new TimeoutBudget(1, TimeUnit.MINUTES));
    }

    public static void deployChromeSeleniumApp(String namespace, Kubernetes kubeClient) throws Exception {
        kubeClient.createNamespace(namespace);
        kubeClient.createServiceFromResource(namespace, getSystemtestsServiceResource(SELENIUM_CHROME, 4444));
        kubeClient.createConfigmapFromResource(namespace, getRheaConfigMap());
        kubeClient.createDeploymentFromResource(namespace,
                getSeleniumNodeDeploymentResource(SystemtestsKubernetesApps.SELENIUM_CHROME, "selenium/standalone-chrome"));
        kubeClient.createIngressFromResource(namespace, getSystemtestsIngressResource(SELENIUM_CHROME, 4444));
        TestUtils.waitForExpectedReadyPods(kube, namespace, 1, new TimeoutBudget(1, TimeUnit.MINUTES));
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
        kube.createSecret(namespace, getPostgresSecret());
        kube.createServiceFromResource(namespace, getSystemtestsServiceResource(POSTGRES_APP, 5432));
        kube.createPvc(namespace, getPostgresPVC());
        kube.createConfigmapFromResource(namespace, getPostgresConfigMap());
        kube.createDeploymentFromResource(namespace, getPostgresDeployment());
        return kube.getEndpoint(POSTGRES_APP, namespace, "http");
    }

    public static void deletePostgresDB(String namespace) {
        if (kube.deploymentExists(namespace, POSTGRES_APP)) {
            kube.deleteService(namespace, POSTGRES_APP);
            kube.deletePvc(namespace, POSTGRES_APP);
            kube.deleteConfigmap(namespace, POSTGRES_APP);
            kube.deleteDeployment(namespace, POSTGRES_APP);
            kube.deleteSecret(namespace, POSTGRES_APP);
        }
    }

    public static void deployProxyApiApp() throws Exception {
        kube.createServiceFromResource(kube.getInfraNamespace(), getProxyApiServiceResource());
        kube.createDeploymentFromResource(kube.getInfraNamespace(), getProxyApiAppDeploymentResource());
    }

    public static void deleteProxyApiApp() {
        kube.deleteDeployment(kube.getInfraNamespace(), API_PROXY);
        kube.deleteService(kube.getInfraNamespace(), API_PROXY);
    }

    public static String getProxyApiDnsName() {
        return String.format("%s.%s.svc", SystemtestsKubernetesApps.API_PROXY, kube.getInfraNamespace());
    }

    private static Function<InputStream, InputStream> namespaceReplacer(final String namespace) {
        final Map<String, String> values = new HashMap<>();
        values.put("NAMESPACE", namespace);
        return in -> ReplaceValueStream.replaceValues(in, values);
    }

    public static Path[] resolveAll(final Path base, final String... localPaths) {
        return Arrays.stream(localPaths)
                .map(base::resolve)
                .toArray(Path[]::new);
    }

    public static Endpoint deployInfinispanServer() throws Exception {

        if (env.isSkipDeployInfinispan()) {
            return getInfinispanEndpoint(INFINISPAN_PROJECT);
        }

        kube.createNamespace(INFINISPAN_PROJECT);

        final KubernetesClient client = getClient();

        // apply "common" and "manual" folders

        if (!Kubernetes.isOpenShiftCompatible()) {
            log.info("Installing Infinispan for Kubernetes");
            kube.apply(INFINISPAN_PROJECT, namespaceReplacer(INFINISPAN_PROJECT),
                    resolveAll(INFINISPAN_EXAMPLE_BASE, INFINISPAN_KUBERNETES));
        } else {
            log.info("Installing Infinispan for OpenShift");
            kube.apply(INFINISPAN_PROJECT, namespaceReplacer(INFINISPAN_PROJECT),
                    resolveAll(INFINISPAN_EXAMPLE_BASE, INFINISPAN_OPENSHIFT));
        }

        // wait for the deployment

        client
                .apps().statefulSets()
                .inNamespace(INFINISPAN_PROJECT)
                .withName(INFINISPAN_SERVER)
                .waitUntilReady(5, TimeUnit.MINUTES);

        // return hotrod enpoint

        return getInfinispanEndpoint(INFINISPAN_PROJECT);
    }

    private static Endpoint getInfinispanEndpoint(final String namespace) {
        return kube.getServiceEndpoint(INFINISPAN_SERVER, namespace, "infinispan");
    }

    public static void deleteInfinispanServer() throws Exception {

        if (env.isSkipDeployInfinispan()) {
            return;
        }

        // delete "common" and "manual" folders
        final KubernetesClient client = getClient();

        if (client.apps()
                .statefulSets()
                .inNamespace(INFINISPAN_PROJECT)
                .withName(INFINISPAN_SERVER)
                .get() != null) {

            log.info("Infinispan server will be removed");

            for (final Path path : resolveAll(INFINISPAN_EXAMPLE_BASE, INFINISPAN_OPENSHIFT)) {
                KubeCMDClient.deleteFromFile(INFINISPAN_PROJECT, path);
            }

        }
    }

    public static void collectInfinispanServerLogs(Path path) {
        try {
            GlobalLogCollector collector = new GlobalLogCollector(kube, path, SystemtestsKubernetesApps.INFINISPAN_PROJECT);
            collector.collectLogsOfPodsInNamespace(SystemtestsKubernetesApps.INFINISPAN_PROJECT);
        } catch (Exception e) {
            log.error("Failed to collect pod logs from namespace : {}", SystemtestsKubernetesApps.INFINISPAN_PROJECT);
        }
    }

    /**
     * Deploy the PostgreSQL server for the JDBC device registry.
     */
    public static Endpoint deployPostgresqlServer() throws Exception {

        if (env.isSkipDeployPostgresql()) {
            return getPostgresqlEndpoint(POSTGRESQL_PROJECT);
        }

        kube.createNamespace(POSTGRESQL_PROJECT);

        final KubernetesClient client = getClient();

        kube.apply(POSTGRESQL_PROJECT, namespaceReplacer(POSTGRESQL_PROJECT), POSTGRESQL_EXAMPLE_BASE);

        // wait for the deployment

        client
                .apps().deployments()
                .inNamespace(POSTGRESQL_PROJECT)
                .withName(POSTGRESQL_SERVER)
                .waitUntilReady(5, TimeUnit.MINUTES);

        // wait until all containers are ready as well

        var podLister = client
                .pods()
                .inNamespace(POSTGRESQL_PROJECT)
                .withLabel("app", "postgresql");

        TestUtils.waitUntilCondition(() -> {
            return podLister
                .list().getItems().stream()
                .filter(conditionIsTrue("ContainersReady"))
                .findFirst()
                .map(p -> true)
                .orElse(false);
        }, ofMinutes(5), ofSeconds(10));

        var pod = podLister
                .list().getItems().stream().findFirst()
                .orElseThrow(() -> new IllegalStateException("Pod that was ready just dissapeared"));
        var podAccess = client.pods()
                .inNamespace(pod.getMetadata().getNamespace())
                .withName(pod.getMetadata().getName());

        // deploy SQL schema

        deployPostgreSQLSchema(podAccess, POSTGRESQL_CREATE_TABLE_SQL);

        // return endpoint

        return getPostgresqlEndpoint(POSTGRESQL_PROJECT);
    }

    private static void deployPostgreSQLSchema(final PodResource<Pod, DoneablePod> podAccess, final Path... sql) throws IOException, InterruptedException, TimeoutException {
        log.info("Deploying SQL schemas: {}", new Object[]{sql});

        for (Path path : sql) {
            try (InputStream sqlFile = Files.newInputStream(path)) {
                executeWithInput(
                        podAccess, sqlFile,
                        input -> input.write("\n\\q\n".getBytes(UTF_8)),
                        Duration.ofSeconds(10),
                        "bash", "-c", "PGPASSWORD=user12 psql device-registry registry");
            }
        }
    }

    /**
     * Get the endpoint of the PostgreSQL server for the JDBC device registry.
     */
    private static Endpoint getPostgresqlEndpoint(final String namespace) {
        return kube.getServiceEndpoint(POSTGRESQL_SERVER, namespace, "postgresql");
    }

    /**
     * Delete the PostgreSQL server for the JDBC device registry.
     */
    public static void deletePostgresqlServer() throws Exception {

        if (env.isSkipDeployPostgresql()) {
            return;
        }

        final KubernetesClient client = getClient();

        if (client.apps()
                .deployments()
                .inNamespace(POSTGRESQL_PROJECT)
                .withName(POSTGRESQL_SERVER)
                .get() != null) {

            log.info("Postgresql server will be removed");

            KubeCMDClient.deleteFromFile(POSTGRESQL_PROJECT, POSTGRESQL_EXAMPLE_BASE);
        }
    }

    // h2

    /**
     * Deploy the H2 server for the JDBC device registry.
     */
    public static Endpoint deployH2Server() throws Exception {

        if (env.isSkipDeployH2()) {
            return getH2Endpoint(H2_PROJECT);
        }

        kube.createNamespace(H2_PROJECT);

        final KubernetesClient client = getClient();

        kube.apply(H2_PROJECT, namespaceReplacer(H2_PROJECT), H2_EXAMPLE_BASE);

        // wait for the deployment

        client
                .apps().deployments()
                .inNamespace(H2_PROJECT)
                .withName(H2_SERVER)
                .waitUntilReady(5, TimeUnit.MINUTES);

        // deploy the SQL schema

        var pod = getH2ServerPod().orElseThrow(() -> new IllegalStateException("No H2 pod found after deployment was ready"));

        // wait until all containers are ready as well

        pod.waitUntilCondition(conditionIsTrue("ContainersReady"), 5, TimeUnit.MINUTES);

        // deploy SQL schema

        deployH2SQLSchema(pod, H2_CREATE_SQL);

        // return endpoint

        return getH2Endpoint(H2_PROJECT);
    }

    public static Optional<PodResource<Pod, DoneablePod>> getH2ServerPod() {
        final var client = getClient();
        return getClient()
                .pods()
                .inNamespace(H2_PROJECT)
                .withLabel("app", "h2")
                .list().getItems().stream().findFirst()
                .map(pod -> client.pods()
                        .inNamespace(pod.getMetadata().getNamespace())
                        .withName(pod.getMetadata().getName()));
    }

    private static void deployH2SQLSchema(final PodResource<Pod, DoneablePod> podAccess, final Path sql) throws IOException, InterruptedException, TimeoutException {
        log.info("Deploying SQL schema: {}", sql);

        try (InputStream sqlFile = Files.newInputStream(sql)) {
            executeWithInput(
                    podAccess, sqlFile,
                    input -> input.write("\nexit\n".getBytes(UTF_8)),
                    Duration.ofSeconds(10),
                    H2_SHELL_COMMAND);
        }

    }

    /**
     * Get the endpoint of the H2 server for the JDBC device registry.
     */
    private static Endpoint getH2Endpoint(final String namespace) {
        return kube.getServiceEndpoint(H2_SERVER, namespace, "h2");
    }

    /**
     * Delete the PostgreSQL server for the JDBC device registry.
     */
    public static void deleteH2Server() throws Exception {

        if (env.isSkipDeployH2()) {
            return;
        }

        final KubernetesClient client = getClient();

        if (client.apps()
                .deployments()
                .inNamespace(H2_PROJECT)
                .withName(H2_SERVER)
                .get() != null) {

            log.info("H2 server will be removed");

            KubeCMDClient.deleteFromFile(H2_PROJECT, H2_EXAMPLE_BASE);
        }
    }

    private static Predicate<Pod> conditionIsTrue(final String type) {
        return p -> Optional.ofNullable(p)
                .map(Pod::getStatus)
                .map(PodStatus::getConditions)
                .flatMap(o -> o.stream().filter(c -> type.equals(c.getType())).findFirst())
                .map(PodCondition::getStatus)
                .map("True"::equals)
                .orElse(false);
    }

    public static void deployAMQBroker(String namespace, String name, String user, String password, BrokerCertBundle certBundle) throws Exception {
        kube.createNamespace(namespace);

        getClient().rbac().roles().inNamespace(namespace).createOrReplace(new RoleBuilder()
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
        getClient().rbac().roleBindings().inNamespace(namespace).createOrReplace(new RoleBindingBuilder()
                .withNewMetadata()
                .withName(name)
                .withNamespace(namespace)
                .endMetadata()
                .withNewRoleRef("rbac.authorization.k8s.io", "Role", name)
                .withSubjects(new SubjectBuilder()
                        .withKind("ServiceAccount")
                        .withName("address-space-controller")
                        .withNamespace(kube.getInfraNamespace())
                        .build())
                .build());

        kube.createSecret(namespace, getBrokerSecret(name, certBundle, user, password));

        kube.createDeploymentFromResource(namespace, getBrokerDeployment(name, user, password), 3, TimeUnit.MINUTES);

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

        kube.createServiceFromResource(namespace, service);

        kube.createExternalEndpoint(name, namespace, service, tlsPort);

        getClient()
                .apps().deployments()
                .inNamespace(namespace)
                .withName(name)
                .waitUntilReady(5, TimeUnit.MINUTES);

        Thread.sleep(5000);
    }

    public static void deleteAMQBroker(String namespace, String name) throws Exception {
        getClient().rbac().roles().inNamespace(namespace).withName(name).cascading(true).delete();
        getClient().rbac().roleBindings().inNamespace(namespace).withName(name).cascading(true).delete();
        kube.deleteSecret(namespace, name);
        kube.deleteService(namespace, name);
        kube.deleteDeployment(namespace, name);
        kube.deleteExternalEndpoint(namespace, name);
        Thread.sleep(5000);
    }

    public static void collectAMQBrokerLogs(Path path, String namespace) {
        try {
            GlobalLogCollector collector = new GlobalLogCollector(kube, path, namespace);
            collector.collectLogsOfPodsInNamespace(namespace);
            collector.collectEvents(namespace);
        } catch (Exception e) {
            log.error("Failed to collect pod logs from namespace : {}", namespace);
        }
    }

    public static void scaleDownDeployment(String namespace, String name) throws Exception {
        kube.setDeploymentReplicas(namespace, name, 0);
        TestUtils.waitForExpectedReadyPods(kube, namespace, 0, new TimeoutBudget(30, TimeUnit.SECONDS));
    }

    public static void scaleUpDeployment(String namespace, String name) throws Exception {
        kube.setDeploymentReplicas(namespace, name, 1);
        TestUtils.waitForExpectedReadyPods(kube, namespace, 1, new TimeoutBudget(30, TimeUnit.SECONDS));
    }

    public static void setupScaleTestEnv(Kubernetes kubeClient) {
        kubeClient.createNamespace(SCALE_TEST_CLIENTS_PROJECT);
    }

    private static void collectLogsScaleTestClient(String clientId, Path logsPath, Map<String, String> labels) {
        try {
            Files.createDirectories(logsPath);
            GlobalLogCollector collector = new GlobalLogCollector(kube, logsPath, SCALE_TEST_CLIENTS_PROJECT);
            collector.collectLogsOfPodsByLabels(SCALE_TEST_CLIENTS_PROJECT, null, labels);
        } catch (Exception e) {
            log.error("Failed to collect client {} logs", clientId, e);
        }
    }

    public static void buildOperatorRegistryImage(Kubernetes kubeClient, String olmManifestsImage, String destinationImage, String destinationRegistry, Path... buildWorkspaceFiles) throws Exception {
        kubeClient.createNamespace(CONTAINER_BUILDS_PROJECT);

        String secretName = "olm-manifests-docker-config";
        String username = "foo";
        String pwd = kubeClient.getApiToken();
        JsonObject dockerconfigjson = new JsonObject();
        dockerconfigjson.put("auths", new JsonObject()
                .put(destinationRegistry, new JsonObject()
                        .put("username", username)
                        .put("password", pwd)
                        .put("auth", Base64.getEncoder().encodeToString((username + ":" + pwd).getBytes(StandardCharsets.UTF_8)))));

        Secret secret = new SecretBuilder()
                .withNewMetadata()
                .withName(secretName)
                .endMetadata()
                .withType("kubernetes.io/dockerconfigjson")
                .addToData(".dockerconfigjson", Base64.getEncoder().encodeToString(dockerconfigjson.encode().getBytes(StandardCharsets.UTF_8)))
                .build();
        kubeClient.createSecret(CONTAINER_BUILDS_PROJECT, secret);

        String workspaceConfigMap = "workspace-cm";
        var configmap = new ConfigMapBuilder()
                .withNewMetadata()
                .withName(workspaceConfigMap)
                .endMetadata();
        for (Path file : buildWorkspaceFiles) {
            log.debug("Adding file {} to build workspace", file.getFileName().toString());
            configmap.addToData(file.getFileName().toString(), Files.readString(file));
        }
        kubeClient.createConfigmapFromResource(CONTAINER_BUILDS_PROJECT, configmap.build());

        StringBuilder copyCommand = new StringBuilder();
        for (Path file : buildWorkspaceFiles) {
            String name = file.getFileName().toString();
            copyCommand.append("cp").append(" ")
                .append("dockerfile-storage/").append(name)
                .append(" ").append("workspace/").append(name)
                .append(" ").append("&&").append(" ");
        }
        String command = copyCommand.substring(0, copyCommand.length()-3);

        Pod pod = new PodBuilder()
                .withNewMetadata()
                .withName("container-image-builder")
                .withNamespace(CONTAINER_BUILDS_PROJECT)
                .endMetadata()
                .withNewSpec()
                .withInitContainers(new ContainerBuilder()
                        .withName("set-up-workspace")
                        .withImage("busybox:latest")
                        .withCommand("sh", "-c")
                        .withArgs(command)
                        .withVolumeMounts(
                                new VolumeMountBuilder()
                                .withName("build-workspace")
                                .withMountPath("/workspace")
                                .build(),
                                new VolumeMountBuilder()
                                .withName("dockerfile-storage")
                                .withMountPath("/dockerfile-storage")
                                .build())
                        .build())
                .withContainers(new ContainerBuilder()
                        .withName("kaniko")
                        .withImage("gcr.io/kaniko-project/executor:v0.23.0")
                        .withArgs(
                                "--context=dir:///workspace",
                                "--destination=" + destinationImage,
                                "--build-arg=MANIFESTS_IMAGE=" + olmManifestsImage,
                                "--insecure-registry=true",
                                "--insecure-pull",
                                "--skip-tls-verify=true")
                        .withVolumeMounts(
                                new VolumeMountBuilder()
                                .withName("docker-config")
                                .withMountPath("/kaniko/.docker")
                                .build(),
                                new VolumeMountBuilder()
                                .withName("build-workspace")
                                .withMountPath("/workspace")
                                .build())
                        .build()
                        )
                .withRestartPolicy("Never")
                .withVolumes(
                        new VolumeBuilder()
                        .withName("docker-config")
                        .withSecret(new SecretVolumeSourceBuilder()
                                .withSecretName(secretName)
                                .addNewItem()
                                .withKey(".dockerconfigjson")
                                .withPath("config.json")
                                .endItem()
                                .build())
                        .build(),
                        new VolumeBuilder()
                        .withName("dockerfile-storage")
                        .withConfigMap(new ConfigMapVolumeSourceBuilder()
                                .withName(workspaceConfigMap)
                                .build())
                        .build(),
                        new VolumeBuilder()
                        .withName("build-workspace")
                        .withNewEmptyDir()
                        .endEmptyDir()
                        .build()
                        )
                .endSpec()
                .build();

        createPodRetrying(kubeClient, CONTAINER_BUILDS_PROJECT, pod);

        Function<Pod, String> containerReasonGetter = p -> Optional.ofNullable(p)
            .map(Pod::getStatus)
            .map(PodStatus::getContainerStatuses)
            .filter(c -> !c.isEmpty())
            .map(c -> c.get(0))
            .map(ContainerStatus::getState)
            .map(ContainerState::getTerminated)
            .map(ContainerStateTerminated::getReason)
            .orElse("");

        try {
            TestUtils.waitUntilCondition("Pod is created", waitPhase -> {
                try {
                    return Kubernetes.getInstance().listPods(CONTAINER_BUILDS_PROJECT)
                            .stream().anyMatch(p -> p.getMetadata().getName().contains(pod.getMetadata().getName()));
                } catch (Exception ex) {
                    return false;
                }
            }, new TimeoutBudget(60, TimeUnit.SECONDS));
            kubeClient.waitPodUntilCondition(pod, p -> {
                String reason = containerReasonGetter.apply(p);
                return reason.equals("Completed") || reason.equals("Error");
            }, 5, TimeUnit.MINUTES);
            Pod podRes = kubeClient.getPod(CONTAINER_BUILDS_PROJECT, pod.getMetadata().getName());
            String reason = containerReasonGetter.apply(podRes);
            if (reason.equals("Error")) {
                log.error("Operator registry image build failed because of error");
                collectContainerBuildLogs(kubeClient);
                cleanBuiltContainerImages(kubeClient);
                Assertions.fail("Failed to build custom operator registry because of error");
            }
            log.info("Operator registry image successfully built");
        } catch (InterruptedException e) {
            log.error("Operator registry image build failed because of timeout");
            collectContainerBuildLogs(kubeClient);
            cleanBuiltContainerImages(kubeClient);
            Assertions.fail("Failed to build custom operator registry because of timeout");
        }

    }

    private static void createPodRetrying(Kubernetes kubeClient, String namespace, Pod resource) {
        Exception error = null;
        int retries = 3;
        do {
            try {
                kubeClient.createPodFromResource(namespace, resource);
                error = null;
                return;
            } catch (Exception e) {
                if (e.getMessage().contains("retry after the token is automatically created and added to the service account")) {
                    error = e;
                    retries--;
                } else {
                    Assertions.fail(e);
                }
            }
        } while (retries > 0);
        Assertions.fail(error);
    }

    private static void collectContainerBuildLogs(Kubernetes kubeClient) {
        GlobalLogCollector collector = new GlobalLogCollector(kubeClient,
                TestUtils.getFailedTestLogsPath(TestPlanInfo.getInstance().getActualTestClass()), CONTAINER_BUILDS_PROJECT);
        collector.collectLogsOfPodsInNamespace(CONTAINER_BUILDS_PROJECT);
    }

    public static void cleanBuiltContainerImages(Kubernetes kubeClient) throws Exception {
        kubeClient.deleteNamespace(CONTAINER_BUILDS_PROJECT);
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
                .withNewResources()
                .addToRequests("memory", new Quantity("1024Mi"))
                .endResources()
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
                .withImagePullPolicy(env.getImagePullPolicy())
                .withCommand("sleep")
                .withArgs("infinity")
                .withEnv(new EnvVarBuilder().withName("PN_TRACE_FRM").withValue("true").build())
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

        /*
         * return new DoneableDeployment(getMessagingAppDeploymentResource())
         * .editMetadata()
         * .withName(namespace)
         * .endMetadata()
         * .editSpec()
         * .withNewSelector()
         * .addToMatchLabels("app", namespace)
         * .endSelector()
         * .editTemplate()
         * .withNewMetadata()
         * .addToLabels("app", namespace)
         * .endMetadata()
         * .endTemplate()
         * .endSpec()
         * .done();
         */
    }

    private static Deployment getProxyApiAppDeploymentResource() {
        return new DeploymentBuilder()
                .withNewMetadata()
                .withName(API_PROXY)
                .addToLabels("app", API_PROXY)
                .endMetadata()
                .withNewSpec()
                .withNewSelector()
                .addToMatchLabels("app", API_PROXY)
                .endSelector()
                .withReplicas(1)
                .withNewTemplate()
                .withNewMetadata()
                .addToLabels("app", API_PROXY)
                .endMetadata()
                .withNewSpec()
                .addNewContainer()
                .withName(API_PROXY)
                .withImage("quay.io/enmasse/api-proxy:latest")
                .withPorts(new ContainerPortBuilder().withContainerPort(8443).withName("https").withProtocol("TCP").build())
                .withVolumeMounts(new VolumeMountBuilder().withMountPath("/etc/tls/private").withName("api-proxy-tls").withReadOnly(true).build())
                .endContainer()
                .withVolumes(Collections.singletonList(new VolumeBuilder().withName("api-proxy-tls").withSecret(new SecretVolumeSourceBuilder().withDefaultMode(420).withSecretName("api-proxy-cert").build()).build()))
                .endSpec()
                .endTemplate()
                .endSpec()
                .build();
    }

    private static Service getProxyApiServiceResource() {
        return new ServiceBuilder()
                .withNewMetadata()
                .withName(API_PROXY)
                .addToLabels("app", API_PROXY)
                .addToAnnotations("service.alpha.openshift.io/serving-cert-secret-name", "api-proxy-cert")
                .endMetadata()
                .withNewSpec()
                .addToSelector("app", API_PROXY)
                .addNewPort()
                .withName("https")
                .withPort(8443)
                .withProtocol("TCP")
                .withNewTargetPort("https")
                .endPort()
                .withClusterIP("None") // Headless
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
                        .withHost(appName + "."
                                + (env.kubernetesDomain().equals("nip.io") ? new URL(env.getApiUrl()).getHost() + ".nip.io" : env.kubernetesDomain()))
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
                .withImage("quay.io/enmasse/systemtests-cert-validator:1.0")
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
                                .withImage("quay.io/enmasse/artemis-base:2.11.0")
                                .withCommand("/bin/sh")
                                .withArgs("-c",
                                        "/opt/apache-artemis/bin/artemis create /var/run/artemis --allow-anonymous --force --user " + user + " --password " + password + " --role admin")
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
                                .withImage("quay.io/enmasse/artemis-base:2.11.0")
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
                .withImage("quay.io/enmasse/artemis-base:2.11.0")
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
    private static Service getScaleTestClientServiceResource(String clientId, Map<String, String> labels) {
        return new ServiceBuilder()
                .withNewMetadata()
                .withName(SCALE_TEST_CLIENT + "-" + clientId)
                .addToLabels(labels)
                .endMetadata()
                .withNewSpec()
                .addToSelector(labels)
                .addNewPort()
                .withName("http")
                .withPort(8080)
                .withProtocol("TCP")
                .endPort()
                .endSpec()
                .build();
    }

}
