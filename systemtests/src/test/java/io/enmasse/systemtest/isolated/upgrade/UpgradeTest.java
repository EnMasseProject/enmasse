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
import io.enmasse.systemtest.EnmasseInstallType;
import io.enmasse.systemtest.Environment;
import io.enmasse.systemtest.IndicativeSentences;
import io.enmasse.systemtest.UserCredentials;
import io.enmasse.systemtest.bases.TestBase;
import io.enmasse.systemtest.bases.isolated.ITestIsolatedStandard;
import io.enmasse.systemtest.condition.OpenShift;
import io.enmasse.systemtest.executor.Exec;
import io.enmasse.systemtest.operator.OperatorManager;
import io.enmasse.systemtest.platform.KubeCMDClient;
import io.enmasse.systemtest.logs.CustomLogger;
import io.enmasse.systemtest.messagingclients.AbstractClient;
import io.enmasse.systemtest.messagingclients.ClientArgument;
import io.enmasse.systemtest.messagingclients.ClientArgumentMap;
import io.enmasse.systemtest.messagingclients.ExternalClients;
import io.enmasse.systemtest.messagingclients.rhea.RheaClientReceiver;
import io.enmasse.systemtest.messagingclients.rhea.RheaClientSender;
import io.enmasse.systemtest.time.TimeoutBudget;
import io.enmasse.systemtest.utils.AddressUtils;
import io.enmasse.systemtest.utils.AuthServiceUtils;
import io.enmasse.systemtest.utils.TestUtils;
import io.enmasse.user.model.v1.User;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.slf4j.Logger;

import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Stream;

import static io.enmasse.systemtest.TestTag.UPGRADE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

@Tag(UPGRADE)
@ExternalClients
class UpgradeTest extends TestBase implements ITestIsolatedStandard {

    private static final int MESSAGE_COUNT = 50;
    private static Logger log = CustomLogger.getLogger();
    private static String productName;
    private EnmasseInstallType type;

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

        assertTrue(sendMessage("brokered", new RheaClientSender(), new UserCredentials("test-brokered", "test"), "brokered-queue", "pepa", MESSAGE_COUNT, true));
        assertTrue(sendMessage("standard", new RheaClientSender(), new UserCredentials("test-standard", "test"), "standard-queue-small", "pepa", MESSAGE_COUNT, true));
        assertTrue(sendMessage("standard", new RheaClientSender(), new UserCredentials("test-standard", "test"), "standard-queue-xlarge", "pepa", MESSAGE_COUNT, true));

        createIoTConfigCMD(kubernetes.getInfraNamespace(), Paths.get(templates));
        Thread.sleep(30_000);
        TestUtils.waitUntilDeployed(kubernetes.getInfraNamespace());

        createIoTProject(kubernetes.getInfraNamespace(), Paths.get(templates));
        Thread.sleep(30_000);
        TestUtils.waitUntilDeployed(kubernetes.getInfraNamespace());

        createIoTUser(kubernetes.getInfraNamespace(), Paths.get(templates));
        Thread.sleep(30_000);

        //TODO add iot devices and test them

        if (this.type.equals(EnmasseInstallType.ANSIBLE)) {
            installEnmasseAnsible(Paths.get(Environment.getInstance().getUpgradeTemplates()), true);
        } else {
            upgradeEnmasseBundle(Paths.get(Environment.getInstance().getUpgradeTemplates()));
        }

        //TODO check iot devicesq

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
        Path inventoryFile = Paths.get(System.getProperty("user.dir"), "ansible", "inventory", kubernetes.getOcpVersion() == 3 ? "systemtests.inventory" : "systemtests.ocp4.inventory");
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
                    log.info("Comparing: {}", container.getImage()
                            .replaceAll("^.*/", "")
                            .replace("enmasse-controller-manager", "controller-manager"));
                    if (!images.contains(container.getImage()
                            .replaceAll("^.*/", "")
                            .replace("enmasse-controller-manager", "controller-manager")) && !container.getImage().contains("postgresql")) {
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
        Exec.execute(Arrays.asList("sh", Paths.get(templates.toString(), "install", "components", "iot", "examples", "k8s-tls", "create").toString()),
                60_000, true, true, Collections.singletonMap("CLUSTER", new URL(Environment.getInstance().getApiUrl()).getHost()));
        KubeCMDClient.runOnCluster("create", "secret", "tls", "iot-mqtt-adapter-tls",
                "--key", Paths.get(templates.toString(), "install", "components", "iot", "examples", "k8s-tls", "build", "iot-mqtt-adapter-key.pem").toString(),
                "--cert", Paths.get(templates.toString(), "install", "components", "iot", "examples", "k8s-tls", "build", "iot-mqtt-adapter-fullchain.pem").toString());
        KubeCMDClient.applyFromFile(kubernetes.getInfraNamespace(), Paths.get(templates.toString(), "install", "components", "iot", "examples", "infinispan", "common"));
        KubeCMDClient.applyFromFile(kubernetes.getInfraNamespace(), Paths.get(templates.toString(), "install", "components", "iot", "examples", "infinispan", "manual"));
        KubeCMDClient.applyFromFile(kubernetes.getInfraNamespace(), Paths.get(templates.toString(), "install", "components", "iot", "examples", "iot-config.yaml"));
    }

    private void createIoTProject(String namespace, Path templates) {
        log.info("Creating IoT project in namespace {}", namespace);
        KubeCMDClient.applyFromFile(kubernetes.getInfraNamespace(), Paths.get(templates.toString(), "install", "components", "iot", "examples", "iot-project-managed.yaml"));
    }

    private void createIoTUser(String namespace, Path templates){
        log.info("Creating IoT user in namespace {}", namespace);
        KubeCMDClient.applyFromFile(kubernetes.getInfraNamespace(), Paths.get(templates.toString(), "install", "components", "iot", "examples", "iot-user.yaml"));
    }

}
