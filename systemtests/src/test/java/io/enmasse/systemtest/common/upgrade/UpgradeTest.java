/*
 * Copyright 2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.systemtest.common.upgrade;


import io.enmasse.systemtest.*;
import io.enmasse.systemtest.bases.TestBase;
import io.enmasse.systemtest.cmdclients.CmdClient;
import io.enmasse.systemtest.cmdclients.KubeCMDClient;
import io.enmasse.systemtest.messagingclients.AbstractClient;
import io.enmasse.systemtest.messagingclients.ClientArgument;
import io.enmasse.systemtest.messagingclients.ClientArgumentMap;
import io.enmasse.systemtest.messagingclients.rhea.RheaClientReceiver;
import io.enmasse.systemtest.messagingclients.rhea.RheaClientSender;
import io.enmasse.systemtest.utils.TestUtils;
import io.fabric8.kubernetes.api.model.ContainerStatus;
import io.fabric8.kubernetes.api.model.Pod;
import org.junit.jupiter.api.*;
import org.slf4j.Logger;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import static io.enmasse.systemtest.TestTag.upgrade;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Tag(upgrade)
class UpgradeTest extends TestBase {

    private static final int MESSAGE_COUNT = 50;
    private static Logger log = CustomLogger.getLogger();
    private static String productName;
    private static String startVersion;

    @BeforeAll
    void prepareUpgradeEnv() throws Exception {
        setReuseAddressSpace();
        productName = Environment.getInstance().isDownstream() ? "amq-online" : "enmasse";
        startVersion = getVersionFromTemplateDir(Paths.get(Environment.getInstance().getStartTemplates()));
        SystemtestsKubernetesApps.deployMessagingClientApp();
    }

    @AfterAll
    void clearClientsEnv() {
        SystemtestsKubernetesApps.deleteMessagingClientApp();
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

        createAddressSpaceCMD(kubernetes.getInfraNamespace(), "brokered", "brokered", "brokered-single-broker", !getApiVersion().equals("v1alpha1") ? "standard-authservice" : null, getApiVersion());
        createAddressSpaceCMD(kubernetes.getInfraNamespace(), "standard", "standard", "standard-unlimited-with-mqtt", !getApiVersion().equals("v1alpha1") ? "standard-authservice" : null, getApiVersion());
        Thread.sleep(30_000);

        createUserCMD(kubernetes.getInfraNamespace(), "test-brokered", "test", "brokered", getApiVersion());
        createUserCMD(kubernetes.getInfraNamespace(), "test-standard", "test", "standard", getApiVersion());
        Thread.sleep(30_000);

        createAddressCMD(kubernetes.getInfraNamespace(), "brokered-queue", "brokered-queue", "brokered", "queue", "brokered-queue", getApiVersion());
        createAddressCMD(kubernetes.getInfraNamespace(), "brokered-topic", "brokered-topic", "brokered", "topic", "brokered-topic", getApiVersion());
        createAddressCMD(kubernetes.getInfraNamespace(), "standard-queue", "standard-queue", "standard", "queue", "standard-large-queue", getApiVersion());
        createAddressCMD(kubernetes.getInfraNamespace(), "standard-queue-xlarge", "standard-queue-xlarge", "standard", "queue", "standard-xlarge-queue", getApiVersion());
        createAddressCMD(kubernetes.getInfraNamespace(), "standard-queue-small", "standard-queue-small", "standard", "queue", "standard-small-queue", getApiVersion());
        createAddressCMD(kubernetes.getInfraNamespace(), "standard-topic", "standard-topic", "standard", "topic", "standard-small-topic", getApiVersion());
        createAddressCMD(kubernetes.getInfraNamespace(), "standard-anycast", "standard-anycast", "standard", "anycast", "standard-small-anycast", getApiVersion());
        createAddressCMD(kubernetes.getInfraNamespace(), "standard-multicast", "standard-multicast", "standard", "multicast", "standard-small-multicast", getApiVersion());
        Thread.sleep(30_000);

        waitUntilDeployed(kubernetes.getInfraNamespace());
        Thread.sleep(60_000);

        assertTrue(sendMessage("brokered", new RheaClientSender(), new UserCredentials("test-brokered", "test"), "brokered-queue", "pepa", MESSAGE_COUNT, true));
        assertTrue(sendMessage("standard", new RheaClientSender(), new UserCredentials("test-standard", "test"), "standard-queue", "pepa", MESSAGE_COUNT, true));
        assertTrue(sendMessage("standard", new RheaClientSender(), new UserCredentials("test-standard", "test"), "standard-queue-small", "pepa", MESSAGE_COUNT, true));
        assertTrue(sendMessage("standard", new RheaClientSender(), new UserCredentials("test-standard", "test"), "standard-queue-xlarge", "pepa", MESSAGE_COUNT, true));

        if (isAnsible) {
            installEnmasseAnsible(Paths.get(Environment.getInstance().getUpgradeTemplates()), true);
        } else {
            upgradeEnmasseBundle(Paths.get(Environment.getInstance().getUpgradeTemplates()));
        }
        Thread.sleep(300_000);

        if (!startVersion.equals("1.0")) {

            assertTrue(receiveMessages("brokered", new RheaClientReceiver(), new UserCredentials("test-brokered", "test"), "brokered-queue", MESSAGE_COUNT, true));
            assertTrue(receiveMessages("standard", new RheaClientReceiver(), new UserCredentials("test-standard", "test"), "standard-queue", MESSAGE_COUNT, true));
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
            assertTrue(sendMessage("standard", new RheaClientSender(), new UserCredentials("test-standard", "test"), "standard-queue", "pepa", MESSAGE_COUNT, true));
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
        assertTrue(CmdClient.execute(cmd, 10_000, true).getRetCode(), "AddressSpace not created");
    }

    private void createUserCMD(String namespace, String userName, String password, String addressSpace, String apiVersion) {
        log.info("Creating user {} in namespace {}", userName, namespace);
        Path scriptPath = Paths.get(System.getProperty("user.dir"), "scripts", "create_user.sh");
        List<String> cmd = Arrays.asList("/bin/bash", "-c", scriptPath.toString() + " " + userName + " " + password + " " + namespace + " " + addressSpace + " " + apiVersion);
        assertTrue(CmdClient.execute(cmd, 20_000, true).getRetCode(), "User not created");
    }

    private void createAddressCMD(String namespace, String name, String address, String addressSpace, String type, String plan, String apiVersion) {
        log.info("Creating address {} in namespace {}", name, namespace);
        Path scriptPath = Paths.get(System.getProperty("user.dir"), "scripts", "create_address.sh");
        List<String> cmd = Arrays.asList("/bin/bash", "-c", scriptPath.toString() + " " + namespace + " " + addressSpace + " " + name + " " + address + " " + type + " " + plan + " " + apiVersion);
        assertTrue(CmdClient.execute(cmd, 20_000, true).getRetCode(), "Address not created");
    }

    private void uninstallEnmasse(Path templateDir) {
        log.info("Application will be removed");
        Path inventoryFile = Paths.get(System.getProperty("user.dir"), "ansible", "inventory", "systemtests.inventory");
        Path ansiblePlaybook = Paths.get(templateDir.toString(), "ansible", "playbooks", "openshift", "uninstall.yml");
        List<String> cmd = Arrays.asList("ansible-playbook", ansiblePlaybook.toString(), "-i", inventoryFile.toString(),
                "--extra-vars", String.format("namespace=%s", kubernetes.getInfraNamespace()));
        assertTrue(CmdClient.execute(cmd, 300_000, true).getRetCode(), "Uninstall failed");
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
        waitUntilDeployed(kubernetes.getInfraNamespace());
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

        assertTrue(CmdClient.execute(cmd, 300_000, true).getRetCode(), "Deployment of new version of enmasse failed");
        log.info("Sleep after {}", upgrade ? "upgrade" : "install");
        Thread.sleep(60_000);

        if (upgrade) {
            Thread.sleep(600_000);
            checkImagesUpdated(getVersionFromTemplateDir(templatePaths));
        } else {
            waitUntilDeployed(kubernetes.getInfraNamespace());
        }
    }

    private void checkImagesUpdated(String version) throws Exception {
        Path makefileDir = Paths.get(System.getProperty("user.dir"), "..");
        Path imageEnvDir = Paths.get(makefileDir.toString(), "imageenv.txt");

        CmdClient.execute(Arrays.asList("make", "-C", makefileDir.toString(), "TAG=" + version, "imageenv"), 10_000, false);
        String images = Files.readString(imageEnvDir);
        log.info("Expected images: {}", images);

        kubernetes.listPods().stream().filter(pod -> pod.getMetadata().getName().matches("^qdrouterd-.*-0"))
                .forEach(pod -> kubernetes.deletePod(kubernetes.getInfraNamespace(), pod.getMetadata().getName()));

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
        waitUntilDeployed(kubernetes.getInfraNamespace());
    }

    private void waitUntilDeployed(String namespace) throws Exception {
        TestUtils.waitUntilCondition("All pods and container is ready", waitPhase -> {
            List<Pod> pods = kubernetes.listPods(namespace);
            for (Pod pod : pods) {
                List<ContainerStatus> initContainers = pod.getStatus().getInitContainerStatuses();
                for (ContainerStatus s : initContainers) {
                    if (!s.getReady())
                        return false;
                }
                List<ContainerStatus> containers = pod.getStatus().getContainerStatuses();
                for (ContainerStatus s : containers) {
                    if (!s.getReady())
                        return false;
                }
            }
            return true;
        }, new TimeoutBudget(10, TimeUnit.MINUTES));
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
}
