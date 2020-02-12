/*
 * Copyright 2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.systemtest.isolated.upgrade;


import io.enmasse.address.model.Address;
import io.enmasse.address.model.AddressSpace;
import io.enmasse.admin.model.v1.AuthenticationService;
import io.enmasse.admin.model.v1.AuthenticationServiceSpecStandardStorage;
import io.enmasse.admin.model.v1.AuthenticationServiceSpecStandardType;
import io.enmasse.iot.model.v1.IoTProject;
import io.enmasse.systemtest.Endpoint;
import io.enmasse.systemtest.EnmasseInstallType;
import io.enmasse.systemtest.Environment;
import io.enmasse.systemtest.UserCredentials;
import io.enmasse.systemtest.amqp.AmqpClient;
import io.enmasse.systemtest.bases.TestBase;
import io.enmasse.systemtest.bases.isolated.ITestIsolatedStandard;
import io.enmasse.systemtest.condition.OpenShift;
import io.enmasse.systemtest.condition.OpenShiftVersion;
import io.enmasse.systemtest.executor.Exec;
import io.enmasse.systemtest.iot.CredentialsRegistryClient;
import io.enmasse.systemtest.iot.DeviceRegistryClient;
import io.enmasse.systemtest.iot.HttpAdapterClient;
import io.enmasse.systemtest.iot.MessageSendTester;
import io.enmasse.systemtest.logs.CustomLogger;
import io.enmasse.systemtest.messagingclients.AbstractClient;
import io.enmasse.systemtest.messagingclients.ClientArgument;
import io.enmasse.systemtest.messagingclients.ClientArgumentMap;
import io.enmasse.systemtest.messagingclients.ExternalClients;
import io.enmasse.systemtest.messagingclients.rhea.RheaClientReceiver;
import io.enmasse.systemtest.messagingclients.rhea.RheaClientSender;
import io.enmasse.systemtest.operator.OperatorManager;
import io.enmasse.systemtest.platform.KubeCMDClient;
import io.enmasse.systemtest.time.TimeoutBudget;
import io.enmasse.systemtest.utils.AddressUtils;
import io.enmasse.systemtest.utils.AuthServiceUtils;
import io.enmasse.systemtest.utils.IoTUtils;
import io.enmasse.systemtest.utils.TestUtils;
import io.enmasse.systemtest.utils.UserUtils;
import io.enmasse.user.model.v1.Operation;
import io.enmasse.user.model.v1.User;
import io.enmasse.user.model.v1.UserAuthorizationBuilder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.slf4j.Logger;

import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Stream;

import static io.enmasse.systemtest.TestTag.UPGRADE;
import static org.junit.jupiter.api.Assertions.*;

@Tag(UPGRADE)
@ExternalClients
class UpgradeTest extends TestBase implements ITestIsolatedStandard {

    private static final int MESSAGE_COUNT = 50;
    private static Logger log = CustomLogger.getLogger();
    private static String productName;
    private EnmasseInstallType type;

    private HttpAdapterClient httpAdapterClient;
    private AmqpClient consumerClientIot;
    private String namespaceIot;



    @BeforeAll
    void prepareUpgradeEnv() throws Exception {
        isolatedResourcesManager.setReuseAddressSpace();
        productName = Environment.getInstance().getProductName();
    }

    @AfterEach
    void removeEnmasse() throws Exception {
        if (this.type.equals(EnmasseInstallType.BUNDLE)) {
            assertTrue(OperatorManager.getInstance().clean());
        } else {
            OperatorManager.getInstance().deleteEnmasseAnsible();
        }
        kubernetes.deleteNamespace(namespaceIot);
        if (consumerClientIot != null) {
            consumerClientIot.close();
            consumerClientIot = null;
        }
        if (httpAdapterClient != null) {
            httpAdapterClient.close();
        }
    }

    @ParameterizedTest(name = "testUpgradeBundle-{0}")
    @MethodSource("provideVersions")
    void testUpgradeBundle(String version, String templates) throws Exception {
        this.type = EnmasseInstallType.BUNDLE;
        doTestUpgrade(templates, version);
    }

    @ParameterizedTest(name = "testUpgradeAnsible-{0}")
    @MethodSource("provideVersions")
    @OpenShift
    void testUpgradeAnsible(String version, String templates) throws Exception {
        this.type = EnmasseInstallType.ANSIBLE;
        doTestUpgrade(templates, version);
    }


    ///////////////////////////////////////////////////////////////////////////////////////////////////////
    // Help methods
    ///////////////////////////////////////////////////////////////////////////////////////////////////////

    private void doTestUpgrade(String templates, String version) throws Exception {
        if (this.type.equals(EnmasseInstallType.ANSIBLE)) {
            installEnmasseAnsible(Paths.get(templates), false);
        } else {
            installEnmasseBundle(Paths.get(templates), version);
        }

        String authServiceName = !getApiVersion(version).equals("v1alpha1") ? "standard-authservice" : null;

        if (authServiceName != null) {
            ensurePersistentAuthService(authServiceName);
        }

        createAddressSpaceCMD(kubernetes.getInfraNamespace(), "brokered", "brokered", "brokered-single-broker", authServiceName, getApiVersion(version));
        Thread.sleep(30_000);
        resourcesManager.waitForAddressSpaceReady(resourcesManager.getAddressSpace("brokered"));

        createAddressSpaceCMD(kubernetes.getInfraNamespace(), "standard", "standard", "standard-unlimited-with-mqtt", authServiceName, getApiVersion(version));
        Thread.sleep(30_000);
        resourcesManager.waitForAddressSpaceReady(resourcesManager.getAddressSpace("standard"));

        createUserCMD(kubernetes.getInfraNamespace(), "test-brokered", "test", "brokered", getApiVersion(version));
        createUserCMD(kubernetes.getInfraNamespace(), "test-standard", "test", "standard", getApiVersion(version));
        Thread.sleep(30_000);

        createAddressCMD(kubernetes.getInfraNamespace(), "brokered-queue", "brokered-queue", "brokered", "queue", "brokered-queue", getApiVersion(version));
        createAddressCMD(kubernetes.getInfraNamespace(), "brokered-topic", "brokered-topic", "brokered", "topic", "brokered-topic", getApiVersion(version));
        Thread.sleep(30_000);
        AddressUtils.waitForDestinationsReady(AddressUtils.getAddresses(resourcesManager.getAddressSpace("brokered")).toArray(new Address[0]));

        createAddressCMD(kubernetes.getInfraNamespace(), "standard-queue-xlarge", "standard-queue-xlarge", "standard", "queue", "standard-xlarge-queue", getApiVersion(version));
        createAddressCMD(kubernetes.getInfraNamespace(), "standard-queue-small", "standard-queue-small", "standard", "queue", "standard-small-queue", getApiVersion(version));
        createAddressCMD(kubernetes.getInfraNamespace(), "standard-topic", "standard-topic", "standard", "topic", "standard-small-topic", getApiVersion(version));
        createAddressCMD(kubernetes.getInfraNamespace(), "standard-anycast", "standard-anycast", "standard", "anycast", "standard-small-anycast", getApiVersion(version));
        createAddressCMD(kubernetes.getInfraNamespace(), "standard-multicast", "standard-multicast", "standard", "multicast", "standard-small-multicast", getApiVersion(version));
        Thread.sleep(30_000);
        AddressUtils.waitForDestinationsReady(AddressUtils.getAddresses(resourcesManager.getAddressSpace("standard")).toArray(new Address[0]));

        // Workaround - addresses may report ready before the broker pod backing the address report ready=true.  This happens because broker liveness/readiness is judged on a
        // Jolokia based probe. As jolokia becomes available after AMQP management, address can be ready when the broker is not. See https://github.com/EnMasseProject/enmasse/issues/2979
        kubernetes.awaitPodsReady(new TimeoutBudget(5, TimeUnit.MINUTES));

        assertTrue(sendMessage("brokered", new RheaClientSender(), new UserCredentials("test-brokered", "test"), "brokered-queue", "pepa", MESSAGE_COUNT, true));
        assertTrue(sendMessage("standard", new RheaClientSender(), new UserCredentials("test-standard", "test"), "standard-queue-small", "pepa", MESSAGE_COUNT, true));
        assertTrue(sendMessage("standard", new RheaClientSender(), new UserCredentials("test-standard", "test"), "standard-queue-xlarge", "pepa", MESSAGE_COUNT, true));

        //iot
        createIoTConfigCMD(kubernetes.getInfraNamespace(), Paths.get(templates));
        Thread.sleep(30_000);
        TestUtils.waitUntilDeployed(kubernetes.getInfraNamespace());

        namespaceIot = "myapp";

        createIoTProject(namespaceIot, Paths.get(templates));
        Thread.sleep(30_000);
        resourcesManager.waitForAddressSpaceReady(resourcesManager.getAddressSpace(namespaceIot,"iot"));
        IoTUtils.waitForIoTProjectReady(kubernetes, resourcesManager.getIoTProject("iot", namespaceIot));

        createIoTUser(namespaceIot, Paths.get(templates));
        Thread.sleep(30_000);

        //add iot devices and test them
        String deviceIdIot = String.valueOf(4711); //UUID.randomUUID().toString()
        String honoAuthId = "sensor1"; //UUID.randomUUID().toString()
        String honoPassword = "hono-secret"; //UUID.randomUUID().toString()
        String tenantIdIot = IoTUtils.getTenantId(resourcesManager.getIoTProject("iot", namespaceIot));
        AddressSpace addressSpaceIot = resourcesManager.getAddressSpace(namespaceIot, "iot");

        createIoTDevice(namespaceIot, deviceIdIot, honoAuthId, honoPassword);
        Thread.sleep(30_000);

        createConsumerClientIot(tenantIdIot, addressSpaceIot);
        startHTTPClient(namespaceIot, honoAuthId, honoPassword);
        sendTelemetryMessageSingle(tenantIdIot);
        sendEventMessageSingle(tenantIdIot);

        /* UPGRADE */
        if (this.type.equals(EnmasseInstallType.ANSIBLE)) {
            installEnmasseAnsible(Paths.get(Environment.getInstance().getUpgradeTemplates()), true);
        } else {
            upgradeEnmasseBundle(Paths.get(Environment.getInstance().getUpgradeTemplates()));
        }

        //check iot devices
        AddressSpace iotSpace = resourcesManager.getAddressSpace(namespaceIot, "iot");
        assertNotNull(iotSpace);
        sendTelemetryMessageSingle(tenantIdIot);
        sendEventMessageSingle(tenantIdIot);

        AddressSpace brokered = resourcesManager.getAddressSpace("brokered");
        assertNotNull(brokered);
        AddressSpace standard = resourcesManager.getAddressSpace("standard");
        assertNotNull(standard);
        Arrays.asList(brokered, standard).forEach(a -> {
            try {
                resourcesManager.waitForAddressSpaceReady(a);
            } catch (Exception e) {
                fail(String.format("Address space didn't return to ready after upgrade : %s", a), e);
            }
        });

        AddressUtils.waitForDestinationsReady(AddressUtils.getAddresses(brokered).toArray(new Address[0]));
        AddressUtils.waitForDestinationsReady(AddressUtils.getAddresses(standard).toArray(new Address[0]));

        List<User> items = kubernetes.getUserClient().list().getItems();
        log.info("After upgrade {} user(s)", items.size());
        items.forEach(u -> log.info("User {}", u.getSpec().getUsername()));

        if (!version.equals("1.0")) {

            assertTrue(receiveMessages("brokered", new RheaClientReceiver(), new UserCredentials("test-brokered", "test"), "brokered-queue", MESSAGE_COUNT, true));
            assertTrue(receiveMessages("standard", new RheaClientReceiver(), new UserCredentials("test-standard", "test"), "standard-queue-small", MESSAGE_COUNT, true));
            assertTrue(receiveMessages("standard", new RheaClientReceiver(), new UserCredentials("test-standard", "test"), "standard-queue-xlarge", MESSAGE_COUNT, true));
        } else {
            if (!KubeCMDClient.getUser(kubernetes.getInfraNamespace(), "brokered", "test-brokered").getRetCode()) {
                createUserCMD(kubernetes.getInfraNamespace(), "test-brokered", "test", "brokered", "v1alpha1");
            }
            if (!KubeCMDClient.getUser(kubernetes.getInfraNamespace(), "standard", "test-standard").getRetCode()) {
                createUserCMD(kubernetes.getInfraNamespace(), "test-standard", "test", "standard", "v1alpha1");
            }
            Thread.sleep(30_000);

            assertTrue(sendMessage("brokered", new RheaClientSender(), new UserCredentials("test-brokered", "test"), "brokered-queue", "pepa", MESSAGE_COUNT, true));
            assertTrue(sendMessage("standard", new RheaClientSender(), new UserCredentials("test-standard", "test"), "standard-queue-small", "pepa", MESSAGE_COUNT, true));
            assertTrue(sendMessage("standard", new RheaClientSender(), new UserCredentials("test-standard", "test"), "standard-queue-xlarge", "pepa", MESSAGE_COUNT, true));
        }
    }

    private static String getVersionFromTemplateDir(Path templateDir) {
        return templateDir.toString().substring(templateDir.toString().lastIndexOf("-") + 1);
    }

    private void createAddressSpaceCMD(String namespace, String name, String type, String plan, String authService, String apiVersion) {
        log.info("Creating addressspace {} in namespace {}", name, namespace);
        Path scriptPath = Paths.get(System.getProperty("user.dir"), "scripts", "create_address_space.sh");
        List<String> cmd = Arrays.asList("/bin/bash", "-c", scriptPath.toString() + " " + namespace + " " + name + " " + type + " " + plan + " " + authService + " " + apiVersion);
        assertTrue(Exec.execute(cmd, 10_000, true).getRetCode(), "AddressSpace not created");
    }

    private void createUserCMD(String namespace, String userName, String password, String addressSpace, String apiVersion) {
        log.info("Creating user {} in namespace {}", userName, namespace);
        Path scriptPath = Paths.get(System.getProperty("user.dir"), "scripts", "create_user.sh");
        List<String> cmd = Arrays.asList("/bin/bash", "-c", scriptPath.toString() + " " + userName + " " + password + " " + namespace + " " + addressSpace + " " + apiVersion);
        assertTrue(Exec.execute(cmd, 20_000, true).getRetCode(), "User not created");
    }

    private void createAddressCMD(String namespace, String name, String address, String addressSpace, String type, String plan, String apiVersion) {
        log.info("Creating address {} in namespace {}", name, namespace);
        Path scriptPath = Paths.get(System.getProperty("user.dir"), "scripts", "create_address.sh");
        List<String> cmd = Arrays.asList("/bin/bash", "-c", scriptPath.toString() + " " + namespace + " " + addressSpace + " " + name + " " + address + " " + type + " " + plan + " " + apiVersion);
        assertTrue(Exec.execute(cmd, 20_000, true).getRetCode(), "Address not created");
    }

    private void installEnmasseBundle(Path templateDir, String version) throws Exception {
        log.info("Application will be deployed using bundle version {}", version);
        KubeCMDClient.createNamespace(kubernetes.getInfraNamespace());
        KubeCMDClient.switchProject(kubernetes.getInfraNamespace());
        if (version.startsWith("0.26") || version.equals("1.0")) {
            KubeCMDClient.applyFromFile(kubernetes.getInfraNamespace(), Paths.get(templateDir.toString(), "install", "bundles", productName.equals("enmasse") ? "enmasse-with-standard-authservice" : productName));
        } else {
            KubeCMDClient.applyFromFile(kubernetes.getInfraNamespace(), Paths.get(templateDir.toString(), "install", "bundles", productName));
            KubeCMDClient.applyFromFile(kubernetes.getInfraNamespace(), Paths.get(templateDir.toString(), "install", "components", "example-plans"));
            KubeCMDClient.applyFromFile(kubernetes.getInfraNamespace(), Paths.get(templateDir.toString(), "install", "components", "example-authservices", "standard-authservice.yaml"));
            KubeCMDClient.applyFromFile(kubernetes.getInfraNamespace(), Paths.get(templateDir.toString(), "install", "components", "example-roles"));
            KubeCMDClient.applyFromFile(kubernetes.getInfraNamespace(), Paths.get(templateDir.toString(), "install", "preview-bundles", "iot"));
        }
        Thread.sleep(60_000);
        TestUtils.waitUntilDeployed(kubernetes.getInfraNamespace());
    }

    private void upgradeEnmasseBundle(Path templateDir) throws Exception {
        KubeCMDClient.applyFromFile(kubernetes.getInfraNamespace(), Paths.get(templateDir.toString(), "install", "bundles", productName));
        Thread.sleep(600_000);
        checkImagesUpdated(getVersionFromTemplateDir(templateDir));
    }

    private String getApiVersion(String version) {
        return version.equals("1.0") || version.contains("0.26") ? "v1alpha1" : "v1beta1";
    }

    private void installEnmasseAnsible(Path templatePaths, boolean upgrade) throws Exception {
        log.info("Application will be installed using ansible");
        Path inventoryFile = Paths.get(System.getProperty("user.dir"), "ansible", "inventory", kubernetes.getOcpVersion() == OpenShiftVersion.OCP3 ? "systemtests.inventory" : "systemtests.ocp4.inventory");
        Path ansiblePlaybook = Paths.get(templatePaths.toString(), "ansible", "playbooks", "openshift", "deploy_all.yml");
        List<String> cmd = Arrays.asList("ansible-playbook", ansiblePlaybook.toString(), "-i", inventoryFile.toString(),
                "--extra-vars", String.format("namespace=%s authentication_services=[\"standard\"] enable_iot=True", kubernetes.getInfraNamespace()));

        assertTrue(Exec.execute(cmd, 300_000, true).getRetCode(), "Deployment of new version of enmasse failed");
        log.info("Sleep after {}", upgrade ? "upgrade" : "install");
        Thread.sleep(60_000);

        if (upgrade) {
            Thread.sleep(600_000);
            checkImagesUpdated(getVersionFromTemplateDir(templatePaths));
        } else {
            TestUtils.waitUntilDeployed(kubernetes.getInfraNamespace());
        }
    }

    private void checkImagesUpdated(String version) throws Exception {
        Path makefileDir = Paths.get(System.getProperty("user.dir"), "..");
        Path imageEnvDir = Paths.get(makefileDir.toString(), "imageenv.txt");

        String images = Files.readString(imageEnvDir);
        log.info("Expected images: {}", images);

        TestUtils.waitUntilCondition("Images are updated", (phase) -> {
            AtomicBoolean ready = new AtomicBoolean(true);
            log.info("=======================================================");
            kubernetes.listPods().forEach(pod -> {
                pod.getSpec().getContainers().forEach(container -> {
                    log.info("Pod {}, current container {}", pod.getMetadata().getName(), container.getImage());
                    String replaced = container.getImage()
                        .replaceAll("^.*/", "")
                        .replace("enmasse-controller-manager", "controller-manager")
                        .replace("console-httpd", "console-server");

                    log.info("Comparing: {}", replaced);
                    if (!images.contains(replaced) && !container.getImage().contains("postgresql")) {
                        log.warn("Container is not upgraded");
                        ready.set(false);
                    } else {
                        log.info("Container is upgraded");
                    }
                });
                pod.getSpec().getInitContainers().forEach(initContainer -> {
                    log.info("Pod {}, current initContainer {}", pod.getMetadata().getName(), initContainer.getImage());
                    if (!images.contains(initContainer.getImage()
                            .replaceAll("^.*/", ""))) {
                        log.warn("Init container is not upgraded");
                        ready.set(false);
                    } else {
                        log.info("InitContainer is upgraded");
                    }
                });
                log.info("*********************************************");
            });
            return ready.get();
        }, new TimeoutBudget(10, TimeUnit.MINUTES));
        TestUtils.waitUntilDeployed(kubernetes.getInfraNamespace());
    }

    protected boolean sendMessage(String addressSpace, AbstractClient client, UserCredentials
            credentials, String address, String content, int count, boolean logToOutput) throws Exception {
        ClientArgumentMap arguments = new ClientArgumentMap();
        arguments.put(ClientArgument.USERNAME, credentials.getUsername());
        arguments.put(ClientArgument.PASSWORD, credentials.getPassword());
        arguments.put(ClientArgument.CONN_SSL, "true");
        arguments.put(ClientArgument.MSG_CONTENT, content);
        arguments.put(ClientArgument.BROKER, KubeCMDClient.getMessagingEndpoint(kubernetes.getInfraNamespace(), addressSpace) + ":443");
        arguments.put(ClientArgument.ADDRESS, address);
        arguments.put(ClientArgument.COUNT, Integer.toString(count));
        arguments.put(ClientArgument.MSG_DURABLE, "true");
        arguments.put(ClientArgument.LOG_MESSAGES, "json");
        client.setArguments(arguments);

        return client.run(logToOutput);
    }

    protected boolean receiveMessages(String addressSpace, AbstractClient client, UserCredentials
            credentials, String address, int count, boolean logToOutput) throws Exception {
        ClientArgumentMap arguments = new ClientArgumentMap();
        arguments.put(ClientArgument.USERNAME, credentials.getUsername());
        arguments.put(ClientArgument.PASSWORD, credentials.getPassword());
        arguments.put(ClientArgument.CONN_SSL, "true");
        arguments.put(ClientArgument.BROKER, KubeCMDClient.getMessagingEndpoint(kubernetes.getInfraNamespace(), addressSpace) + ":443");
        arguments.put(ClientArgument.ADDRESS, address);
        arguments.put(ClientArgument.COUNT, Integer.toString(count));
        arguments.put(ClientArgument.LOG_MESSAGES, "json");
        client.setArguments(arguments);

        return client.run(logToOutput);
    }

    private void ensurePersistentAuthService(String authServiceName) throws Exception {
        AuthenticationService authService = kubernetes.getAuthenticationServiceClient().withName(authServiceName).get();

        if (authService.getSpec() != null && authService.getSpec().getStandard() != null) {

            AuthenticationServiceSpecStandardStorage storage = authService.getSpec().getStandard().getStorage();
            if (storage == null || !AuthenticationServiceSpecStandardType.persistent_claim.equals(storage.getType())) {
                log.info("Installed auth service {} does not use persistent claim, recreating it ", authServiceName);
                resourcesManager.removeAuthService(authService);

                AuthenticationService replacement = AuthServiceUtils.createStandardAuthServiceObject(authServiceName, true);
                kubernetes.getAuthenticationServiceClient().create(replacement);

                log.info("Replacement auth service : {}", replacement);

                Thread.sleep(30_000);
                TestUtils.waitUntilDeployed(kubernetes.getInfraNamespace());
            }
        }
    }

    private static Stream<Arguments> provideVersions() {
        return Arrays.stream(environment.getStartTemplates().split(",")).map(templates -> Arguments.of(getVersionFromTemplateDir(Paths.get(templates)), templates));
    }

    private void createIoTConfigCMD(String namespace, Path templates) throws MalformedURLException {
        log.info("Creating IoT configuration in namespace {}", namespace);
        //TODO cluster host url too long for openshift4
        Exec.execute(Arrays.asList("sh", Paths.get(templates.toString(), "install", "components", "iot", "examples", "k8s-tls", "create").toString()),
                60_000, true, true, Collections.singletonMap("CLUSTER", "my-cluster")); //new URL(Environment.getInstance().getApiUrl()).getHost().replace("api.", "")));
        //TODO generating secrets doesn't work on Mac
        KubeCMDClient.runOnCluster("create", "secret", "tls", "iot-mqtt-adapter-tls",
                "--key", Paths.get(templates.toString(), "install", "components", "iot", "examples", "k8s-tls", "build", "iot-mqtt-adapter-key.pem").toString(),
                "--cert", Paths.get(templates.toString(), "install", "components", "iot", "examples", "k8s-tls", "build", "iot-mqtt-adapter-fullchain.pem").toString());
        KubeCMDClient.applyFromFile(kubernetes.getInfraNamespace(), Paths.get(templates.toString(), "install", "components", "iot", "examples", "infinispan", "common"));
        KubeCMDClient.applyFromFile(kubernetes.getInfraNamespace(), Paths.get(templates.toString(), "install", "components", "iot", "examples", "infinispan", "manual"));
        KubeCMDClient.applyFromFile(kubernetes.getInfraNamespace(), Paths.get(templates.toString(), "install", "components", "iot", "examples", "iot-config.yaml"));
    }

    private void createIoTProject(String namespaceIot, Path templates) {
        log.info("Creating IoT project in namespace {}", namespaceIot);
        kubernetes.createNamespace(namespaceIot);
        KubeCMDClient.applyFromFile(namespaceIot, Paths.get(templates.toString(), "install", "components", "iot", "examples", "iot-project-managed.yaml"));
    }

    private void createIoTUser(String namespaceIot, Path templates){
        log.info("Creating IoT user in namespace {}", namespaceIot);
        KubeCMDClient.applyFromFile(namespaceIot, Paths.get(templates.toString(), "install", "components", "iot", "examples", "iot-user.yaml"));
    }

    private void createIoTDevice(String namespaceIot, String deviceIdIot, String honoAuthId, String honoPassword) throws Exception {
        log.info("Registering new IoT device {} in namespace {}", deviceIdIot, namespaceIot);

        DeviceRegistryClient deviceRegistryClient = new DeviceRegistryClient(kubernetes.getExternalEndpoint("device-registry"));
        deviceRegistryClient.registerDevice(String.format("%s.iot", namespaceIot), deviceIdIot);

        CredentialsRegistryClient credentialsRegistryClient = new CredentialsRegistryClient(kubernetes.getExternalEndpoint("device-registry"));
        credentialsRegistryClient.addCredentials(String.format("%s.iot", namespaceIot), deviceIdIot, honoAuthId, honoPassword, null);
    }

    private void createConsumerClientIot(String tenantIdIot, AddressSpace addressSpaceIot) throws Exception {
        log.info("Creating consumer client for IOT");
        String consumerClientIotUsername = "consumer-user"; //UUID.randomUUID().toString();
        String consumerClientIotPassword = "consumer-password"; //UUID.randomUUID().toString();
        User businessApplicationUser = UserUtils.createUserResource(new UserCredentials(consumerClientIotUsername, consumerClientIotPassword))
                .editSpec()
                .withAuthorization(
                        Collections.singletonList(new UserAuthorizationBuilder()
                                .withAddresses(
                                        "telemetry" + "/" + tenantIdIot,
                                        "telemetry" + "/" + tenantIdIot + "/*",
                                        "event" + "/" + tenantIdIot,
                                        "event" + "/" + tenantIdIot + "/*")
                                .withOperations(Operation.recv)
                                .build()))
                .endSpec()
                .done();
        resourcesManager.createOrUpdateUser(addressSpaceIot, businessApplicationUser);
        consumerClientIot = getAmqpClientFactory().createQueueClient(addressSpaceIot);
        consumerClientIot.getConnectOptions()
                .setUsername(consumerClientIotUsername)
                .setPassword(consumerClientIotPassword);
    }

    private void startHTTPClient(String namespaceIot, String honoAuthId, String honoPassword) throws Exception {
        log.info("Starting HTTP adapter client for IOT");
        Endpoint httpAdapterEndpoint = kubernetes.getExternalEndpoint("iot-http-adapter");
        IoTProject project = resourcesManager.getIoTProject("iot", namespaceIot);
        httpAdapterClient = new HttpAdapterClient(httpAdapterEndpoint, honoAuthId, IoTUtils.getTenantId(project), honoPassword);
    }

    private void sendTelemetryMessageSingle(String tenantIdIot) throws Exception {
        log.info("Testing sending of single telemetry message");
        new MessageSendTester()
                .type(MessageSendTester.Type.TELEMETRY)
                .delay(Duration.ofSeconds(1))
                .consumerFactory(MessageSendTester.ConsumerFactory.of(consumerClientIot, tenantIdIot))
                .sender(httpAdapterClient::send)
                .amount(1)
                .consume(MessageSendTester.Consume.BEFORE)
                .execute();
    }

    private void sendEventMessageSingle(String tenantIdIot) throws Exception {
        log.info("Testing sending of single event message");
        new MessageSendTester()
                .type(MessageSendTester.Type.EVENT)
                .delay(Duration.ofSeconds(1))
                .consumerFactory(MessageSendTester.ConsumerFactory.of(consumerClientIot, tenantIdIot))
                .sender(httpAdapterClient::send)
                .amount(1)
                .consume(MessageSendTester.Consume.BEFORE)
                .execute();
    }
}
