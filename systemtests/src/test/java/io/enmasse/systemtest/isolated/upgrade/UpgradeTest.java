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
import io.enmasse.systemtest.Environment;
import io.enmasse.systemtest.UserCredentials;
import io.enmasse.systemtest.bases.TestBase;
import io.enmasse.systemtest.bases.isolated.ITestIsolatedStandard;
import io.enmasse.systemtest.condition.OpenShift;
import io.enmasse.systemtest.executor.Exec;
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
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import static io.enmasse.systemtest.TestTag.UPGRADE;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

@Tag(UPGRADE)
@ExternalClients
class UpgradeTest extends TestBase implements ITestIsolatedStandard {

    private static final int MESSAGE_COUNT = 50;
    private static Logger log = CustomLogger.getLogger();
    private static String productName;
    private static String startVersion;

    @BeforeAll
    void prepareUpgradeEnv() throws Exception {
        isolatedResourcesManager.setReuseAddressSpace();
        productName = Environment.getInstance().isDownstream() ? "amq-online" : "enmasse";
        startVersion = getVersionFromTemplateDir(Paths.get(Environment.getInstance().getStartTemplates()));
    }

    @AfterEach
    void removeEnmasse() {
        uninstallEnmasse(Paths.get(Environment.getInstance().getUpgradeTemplates()));
    }

    @Test
    void testUpgradeBundle() throws Exception {
        doTestUpgrade(false);
    }

    @Test
    @OpenShift
    void testUpgradeAnsible() throws Exception {
        doTestUpgrade(true);
    }


    ///////////////////////////////////////////////////////////////////////////////////////////////////////
    // Help methods
    ///////////////////////////////////////////////////////////////////////////////////////////////////////

    private void doTestUpgrade(boolean isAnsible) throws Exception {
        if (isAnsible) {
            installEnmasseAnsible(Paths.get(Environment.getInstance().getStartTemplates()), false);
        } else {
            installEnmasseBundle(Paths.get(Environment.getInstance().getStartTemplates()), startVersion);
        }

        String authServiceName = !getApiVersion().equals("v1alpha1") ? "standard-authservice" : null;

        if (authServiceName != null) {
            ensurePersistentAuthService(authServiceName);
        }

        createAddressSpaceCMD(kubernetes.getInfraNamespace(), "brokered", "brokered", "brokered-single-broker", authServiceName, getApiVersion());
        Thread.sleep(30_000);
        resourcesManager.waitForAddressSpaceReady(resourcesManager.getAddressSpace("brokered"));

        createAddressSpaceCMD(kubernetes.getInfraNamespace(), "standard", "standard", "standard-unlimited-with-mqtt", authServiceName, getApiVersion());
        Thread.sleep(30_000);
        resourcesManager.waitForAddressSpaceReady(resourcesManager.getAddressSpace("standard"));

        createUserCMD(kubernetes.getInfraNamespace(), "test-brokered", "test", "brokered", getApiVersion());
        createUserCMD(kubernetes.getInfraNamespace(), "test-standard", "test", "standard", getApiVersion());
        Thread.sleep(30_000);

        createAddressCMD(kubernetes.getInfraNamespace(), "brokered-queue", "brokered-queue", "brokered", "queue", "brokered-queue", getApiVersion());
        createAddressCMD(kubernetes.getInfraNamespace(), "brokered-topic", "brokered-topic", "brokered", "topic", "brokered-topic", getApiVersion());
        Thread.sleep(30_000);
        waitForDestinationsReady(AddressUtils.getAddresses(resourcesManager.getAddressSpace("brokered")).toArray(new Address[0]));

        createAddressCMD(kubernetes.getInfraNamespace(), "standard-queue-xlarge", "standard-queue-xlarge", "standard", "queue", "standard-xlarge-queue", getApiVersion());
        createAddressCMD(kubernetes.getInfraNamespace(), "standard-queue-small", "standard-queue-small", "standard", "queue", "standard-small-queue", getApiVersion());
        createAddressCMD(kubernetes.getInfraNamespace(), "standard-topic", "standard-topic", "standard", "topic", "standard-small-topic", getApiVersion());
        createAddressCMD(kubernetes.getInfraNamespace(), "standard-anycast", "standard-anycast", "standard", "anycast", "standard-small-anycast", getApiVersion());
        createAddressCMD(kubernetes.getInfraNamespace(), "standard-multicast", "standard-multicast", "standard", "multicast", "standard-small-multicast", getApiVersion());
        Thread.sleep(30_000);
        waitForDestinationsReady(AddressUtils.getAddresses(resourcesManager.getAddressSpace("standard")).toArray(new Address[0]));

        assertTrue(sendMessage("brokered", new RheaClientSender(), new UserCredentials("test-brokered", "test"), "brokered-queue", "pepa", MESSAGE_COUNT, true));
        assertTrue(sendMessage("standard", new RheaClientSender(), new UserCredentials("test-standard", "test"), "standard-queue-small", "pepa", MESSAGE_COUNT, true));
        assertTrue(sendMessage("standard", new RheaClientSender(), new UserCredentials("test-standard", "test"), "standard-queue-xlarge", "pepa", MESSAGE_COUNT, true));

        if (isAnsible) {
            installEnmasseAnsible(Paths.get(Environment.getInstance().getUpgradeTemplates()), true);
        } else {
            upgradeEnmasseBundle(Paths.get(Environment.getInstance().getUpgradeTemplates()));
        }

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

        waitForDestinationsReady(AddressUtils.getAddresses(brokered).toArray(new Address[0]));
        waitForDestinationsReady(AddressUtils.getAddresses(standard).toArray(new Address[0]));

        List<User> items = kubernetes.getUserClient().list().getItems();
        log.info("After upgrade {} user(s)", items.size());
        items.forEach(u -> log.info("User {}", u.getSpec().getUsername()));

        if (!startVersion.equals("1.0")) {

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

    private String getVersionFromTemplateDir(Path templateDir) {
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

    private void uninstallEnmasse(Path templateDir) {
        log.info("Application will be removed");
        Path inventoryFile = Paths.get(System.getProperty("user.dir"), "ansible", "inventory", "systemtests.inventory");
        Path ansiblePlaybook = Paths.get(templateDir.toString(), "ansible", "playbooks", "openshift", "uninstall.yml");
        List<String> cmd = Arrays.asList("ansible-playbook", ansiblePlaybook.toString(), "-i", inventoryFile.toString(),
                "--extra-vars", String.format("namespace=%s", kubernetes.getInfraNamespace()));
        assertTrue(Exec.execute(cmd, 300_000, true).getRetCode(), "Uninstall failed");
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
        }
        Thread.sleep(60_000);
        TestUtils.waitUntilDeployed(kubernetes.getInfraNamespace());
    }

    private void upgradeEnmasseBundle(Path templateDir) throws Exception {
        KubeCMDClient.applyFromFile(kubernetes.getInfraNamespace(), Paths.get(templateDir.toString(), "install", "bundles", productName));
        Thread.sleep(600_000);
        checkImagesUpdated(getVersionFromTemplateDir(templateDir));
    }

    private String getApiVersion() {
        return startVersion.equals("1.0") || startVersion.contains("0.26") ? "v1alpha1" : "v1beta1";
    }

    private void installEnmasseAnsible(Path templatePaths, boolean upgrade) throws Exception {
        log.info("Application will be installed using ansible");
        Path inventoryFile = Paths.get(System.getProperty("user.dir"), "ansible", "inventory", "systemtests.inventory");
        Path ansiblePlaybook = Paths.get(templatePaths.toString(), "ansible", "playbooks", "openshift", "deploy_all.yml");
        List<String> cmd = Arrays.asList("ansible-playbook", ansiblePlaybook.toString(), "-i", inventoryFile.toString(),
                "--extra-vars", String.format("namespace=%s authentication_services=[\"standard\"]", kubernetes.getInfraNamespace()));

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
        }, new TimeoutBudget(5, TimeUnit.MINUTES));
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
}
