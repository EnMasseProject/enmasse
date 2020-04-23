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
import io.enmasse.systemtest.OLMInstallationType;
import io.enmasse.systemtest.UserCredentials;
import io.enmasse.systemtest.bases.TestBase;
import io.enmasse.systemtest.bases.isolated.ITestIsolatedStandard;
import io.enmasse.systemtest.condition.OpenShift;
import io.enmasse.systemtest.condition.OpenShiftVersion;
import io.enmasse.systemtest.executor.Exec;
import io.enmasse.systemtest.logs.CustomLogger;
import io.enmasse.systemtest.logs.GlobalLogCollector;
import io.enmasse.systemtest.messagingclients.AbstractClient;
import io.enmasse.systemtest.messagingclients.ClientArgument;
import io.enmasse.systemtest.messagingclients.ClientArgumentMap;
import io.enmasse.systemtest.messagingclients.ExternalClients;
import io.enmasse.systemtest.messagingclients.rhea.RheaClientReceiver;
import io.enmasse.systemtest.messagingclients.rhea.RheaClientSender;
import io.enmasse.systemtest.operator.EnmasseOperatorManager;
import io.enmasse.systemtest.platform.KubeCMDClient;
import io.enmasse.systemtest.time.TimeoutBudget;
import io.enmasse.systemtest.utils.AddressUtils;
import io.enmasse.systemtest.utils.AuthServiceUtils;
import io.enmasse.systemtest.utils.TestUtils;
import io.enmasse.user.model.v1.User;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.slf4j.Logger;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Stream;

import static io.enmasse.systemtest.TestTag.UPGRADE;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * Note: OLM default upgrade (in openshift-operators namespace) is known to fail due to fsGroup issue since openshift 4.3.8
 * This won't be fixed untile 0.31.1 is released
 */
@Tag(UPGRADE)
@ExternalClients
class UpgradeTest extends TestBase implements ITestIsolatedStandard {

    private static final int MESSAGE_COUNT = 50;
    private static Logger log = CustomLogger.getLogger();
    private static String productName;
    private String infraNamespace;
    private EnmasseInstallType type;
    private OLMInstallationType olmType;
    private EnmasseOperatorManager operatorManager = EnmasseOperatorManager.getInstance();

    @BeforeAll
    void prepareUpgradeEnv() throws Exception {
        isolatedResourcesManager.setReuseAddressSpace();
        productName = Environment.getInstance().getProductName();
    }

    @AfterEach
    void removeEnmasse(ExtensionContext context) throws Exception {
        if (context.getExecutionException().isPresent()) {
            Path logsPath = TestUtils.getUpgradeTestLogsPath(context);
            GlobalLogCollector.saveInfraState(logsPath, infraNamespace);
        }
        boolean waitForNamespace = true;
        if (this.type.equals(EnmasseInstallType.BUNDLE)) {
            assertTrue(operatorManager.clean());
        } else if (this.type.equals(EnmasseInstallType.OLM)) {
            if (EnmasseOperatorManager.getInstance().isEnmasseOlmDeployed()) {
                for (var addrSpace : kubernetes.getAddressSpaceClient(infraNamespace).list().getItems()) {
                    LOGGER.info("address space '{}' will be removed", addrSpace);
                    try {
                        resourcesManager.deleteAddressSpace(addrSpace);
                    } catch (Exception e) {
                        LOGGER.warn("Delete address space failed", e);
                        throw e;
                    }
                }
            }
            assertTrue(operatorManager.removeOlm());
            if (olmType != null && olmType == OLMInstallationType.DEFAULT) {
                waitForNamespace = false;
            }
        } else {
            operatorManager.deleteEnmasseAnsible();
        }
        if (waitForNamespace) {
            TestUtils.waitForNamespaceDeleted(kubernetes, infraNamespace);
        }
    }

    @ParameterizedTest(name = "testUpgradeBundle-{0}")
    @MethodSource("provideVersions")
    void testUpgradeBundle(String version, String templates) throws Exception {
        this.type = EnmasseInstallType.BUNDLE;
        this.infraNamespace = kubernetes.getInfraNamespace();
        doTestUpgrade(templates, version);
    }

    @ParameterizedTest(name = "testUpgradeAnsible-{0}")
    @MethodSource("provideVersions")
    @OpenShift
    void testUpgradeAnsible(String version, String templates) throws Exception {
        this.type = EnmasseInstallType.ANSIBLE;
        this.infraNamespace = kubernetes.getInfraNamespace();
        doTestUpgrade(templates, version);
    }

    @ParameterizedTest(name = "testUpgradeOLMSpecific-{0}")
    @MethodSource("provideVersions")
    @OpenShift(version = OpenShiftVersion.OCP4)
    void testUpgradeOLMSpecific(String version, String templates) throws Exception {
        this.type = EnmasseInstallType.OLM;
        this.olmType = OLMInstallationType.SPECIFIC;
        this.infraNamespace = operatorManager.getNamespaceByOlmInstallationType(olmType);
        doTestUpgrade(templates, version);
    }

    @ParameterizedTest(name = "testUpgradeOLMDefault-{0}")
    @MethodSource("provideVersions")
    @OpenShift(version = OpenShiftVersion.OCP4)
    void testUpgradeOLMDefault(String version, String templates) throws Exception {
        this.type = EnmasseInstallType.OLM;
        this.olmType = OLMInstallationType.DEFAULT;
        this.infraNamespace = operatorManager.getNamespaceByOlmInstallationType(olmType);
        doTestUpgrade(templates, version);
    }

    ///////////////////////////////////////////////////////////////////////////////////////////////////////
    // Help methods
    ///////////////////////////////////////////////////////////////////////////////////////////////////////

    private void doTestUpgrade(String templates, String version) throws Exception {
        if (this.type.equals(EnmasseInstallType.ANSIBLE)) {
            installEnmasseAnsible(Paths.get(templates), false);
        } else if(this.type.equals(EnmasseInstallType.OLM)) {
            String csvVersionNumber = environment.enmasseOlmReplaces().substring(environment.enmasseOlmReplaces().indexOf(".")+1);
            if (!version.equals(csvVersionNumber)) {
                log.warn("Skipping OLM test from version {} , because replaces version is {}", version, environment.enmasseOlmReplaces());
                return;
            }
            if (olmType == OLMInstallationType.DEFAULT && (version.equals("0.31.0") || version.equals("1.4.0"))) {
                //versions with known fsgroup issue
                log.warn("Skipping OLM test for version {}, because know issue without fix");
                return;
            }
            installEnmasseOLM(Paths.get(templates), version);
        } else {
            installEnmasseBundle(Paths.get(templates), version);
        }

        String authServiceName = !getApiVersion(version).equals("v1alpha1") ? "standard-authservice" : null;

        if (authServiceName != null) {
            ensurePersistentAuthService(authServiceName);
        }

        createAddressSpaceCMD(infraNamespace, "brokered", "brokered", "brokered-single-broker", authServiceName, getApiVersion(version));
        Thread.sleep(30_000);
        resourcesManager.waitForAddressSpaceReady(resourcesManager.getAddressSpace(infraNamespace, "brokered"));

        createAddressSpaceCMD(infraNamespace, "standard", "standard", "standard-unlimited", authServiceName, getApiVersion(version));
        Thread.sleep(30_000);
        resourcesManager.waitForAddressSpaceReady(resourcesManager.getAddressSpace(infraNamespace, "standard"));

        createUserCMD(infraNamespace, "test-brokered", "test", "brokered", getApiVersion(version));
        createUserCMD(infraNamespace, "test-standard", "test", "standard", getApiVersion(version));
        Thread.sleep(30_000);

        createAddressCMD(infraNamespace, "brokered-queue", "brokered-queue", "brokered", "queue", "brokered-queue", getApiVersion(version));
        createAddressCMD(infraNamespace, "brokered-topic", "brokered-topic", "brokered", "topic", "brokered-topic", getApiVersion(version));
        Thread.sleep(30_000);
        AddressUtils.waitForDestinationsReady(AddressUtils.getAddresses(resourcesManager.getAddressSpace(infraNamespace, "brokered")).toArray(new Address[0]));

        createAddressCMD(infraNamespace, "standard-queue-xlarge", "standard-queue-xlarge", "standard", "queue", "standard-xlarge-queue", getApiVersion(version));
        createAddressCMD(infraNamespace, "standard-queue-small", "standard-queue-small", "standard", "queue", "standard-small-queue", getApiVersion(version));
        createAddressCMD(infraNamespace, "standard-topic", "standard-topic", "standard", "topic", "standard-small-topic", getApiVersion(version));
        createAddressCMD(infraNamespace, "standard-anycast", "standard-anycast", "standard", "anycast", "standard-small-anycast", getApiVersion(version));
        createAddressCMD(infraNamespace, "standard-multicast", "standard-multicast", "standard", "multicast", "standard-small-multicast", getApiVersion(version));
        Thread.sleep(30_000);
        AddressUtils.waitForDestinationsReady(AddressUtils.getAddresses(resourcesManager.getAddressSpace(infraNamespace, "standard")).toArray(new Address[0]));

        // Workaround - addresses may report ready before the broker pod backing the address report ready=true.  This happens because broker liveness/readiness is judged on a
        // Jolokia based probe. As jolokia becomes available after AMQP management, address can be ready when the broker is not. See https://github.com/EnMasseProject/enmasse/issues/2979
        kubernetes.awaitPodsReady(infraNamespace, new TimeoutBudget(5, TimeUnit.MINUTES));

        assertTrue(sendMessage("brokered", new RheaClientSender(), new UserCredentials("test-brokered", "test"), "brokered-queue", "pepa", MESSAGE_COUNT, true));
        assertTrue(sendMessage("standard", new RheaClientSender(), new UserCredentials("test-standard", "test"), "standard-queue-small", "pepa", MESSAGE_COUNT, true));
        assertTrue(sendMessage("standard", new RheaClientSender(), new UserCredentials("test-standard", "test"), "standard-queue-xlarge", "pepa", MESSAGE_COUNT, true));

        if (this.type.equals(EnmasseInstallType.ANSIBLE)) {
            installEnmasseAnsible(Paths.get(Environment.getInstance().getUpgradeTemplates()), true);
        } else if(this.type.equals(EnmasseInstallType.OLM)) {
            upgradeEnmasseOLM(Paths.get(Environment.getInstance().getUpgradeTemplates()), version);
        } else {
            upgradeEnmasseBundle(Paths.get(Environment.getInstance().getUpgradeTemplates()));
        }

        AddressSpace brokered = resourcesManager.getAddressSpace(infraNamespace, "brokered");
        assertNotNull(brokered);
        AddressSpace standard = resourcesManager.getAddressSpace(infraNamespace, "standard");
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

        List<User> items = kubernetes.getUserClient(infraNamespace).list().getItems();
        log.info("After upgrade {} user(s)", items.size());
        items.forEach(u -> log.info("User {}", u.getSpec().getUsername()));

        if (!version.equals("1.0")) {

            assertTrue(receiveMessages("brokered", new RheaClientReceiver(), new UserCredentials("test-brokered", "test"), "brokered-queue", MESSAGE_COUNT, true));
            assertTrue(receiveMessages("standard", new RheaClientReceiver(), new UserCredentials("test-standard", "test"), "standard-queue-small", MESSAGE_COUNT, true));
            assertTrue(receiveMessages("standard", new RheaClientReceiver(), new UserCredentials("test-standard", "test"), "standard-queue-xlarge", MESSAGE_COUNT, true));
        } else {
            if (!KubeCMDClient.getUser(infraNamespace, "brokered", "test-brokered").getRetCode()) {
                createUserCMD(infraNamespace, "test-brokered", "test", "brokered", "v1alpha1");
            }
            if (!KubeCMDClient.getUser(infraNamespace, "standard", "test-standard").getRetCode()) {
                createUserCMD(infraNamespace, "test-standard", "test", "standard", "v1alpha1");
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
        KubeCMDClient.createNamespace(infraNamespace);
        KubeCMDClient.switchProject(infraNamespace);
        if (version.startsWith("0.26") || version.equals("1.0")) {
            KubeCMDClient.applyFromFile(infraNamespace, Paths.get(templateDir.toString(), "install", "bundles", productName.equals("enmasse") ? "enmasse-with-standard-authservice" : productName));
        } else {
            KubeCMDClient.applyFromFile(infraNamespace, Paths.get(templateDir.toString(), "install", "bundles", productName));
            KubeCMDClient.applyFromFile(infraNamespace, Paths.get(templateDir.toString(), "install", "components", "example-plans"));
            KubeCMDClient.applyFromFile(infraNamespace, Paths.get(templateDir.toString(), "install", "components", "example-authservices", "standard-authservice.yaml"));
        }
        Thread.sleep(60_000);
        TestUtils.waitUntilDeployed(infraNamespace);
    }

    private void upgradeEnmasseBundle(Path templateDir) throws Exception {
        KubeCMDClient.applyFromFile(infraNamespace, Paths.get(templateDir.toString(), "install", "bundles", productName));
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
                "--extra-vars", String.format("namespace=%s authentication_services=[\"standard\"]", infraNamespace));

        assertTrue(Exec.execute(cmd, 300_000, true).getRetCode(), "Deployment of new version of enmasse failed");
        log.info("Sleep after {}", upgrade ? "upgrade" : "install");
        Thread.sleep(60_000);

        if (upgrade) {
            Thread.sleep(600_000);
            checkImagesUpdated(getVersionFromTemplateDir(templatePaths));
        } else {
            TestUtils.waitUntilDeployed(infraNamespace);
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
            kubernetes.listPods(infraNamespace).forEach(pod -> {
                pod.getSpec().getContainers().forEach(container -> {
                    log.info("Pod {}, current container {}", pod.getMetadata().getName(), container.getImage());
                    String replaced = container.getImage()
                        .replaceAll("^.*/", "")
                        .replace("enmasse-controller-manager", "controller-manager")
                        .replace("console-httpd", "console-server");

                    log.info("Comparing: {}", replaced);
                    if (!images.contains(replaced) && !container.getImage().contains("postgresql") && !container.getImage().contains("systemtests-operator-registry")) {
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
        TestUtils.waitUntilDeployed(infraNamespace);
    }

    protected boolean sendMessage(String addressSpace, AbstractClient client, UserCredentials
            credentials, String address, String content, int count, boolean logToOutput) throws Exception {
        ClientArgumentMap arguments = new ClientArgumentMap();
        arguments.put(ClientArgument.USERNAME, credentials.getUsername());
        arguments.put(ClientArgument.PASSWORD, credentials.getPassword());
        arguments.put(ClientArgument.CONN_SSL, "true");
        arguments.put(ClientArgument.MSG_CONTENT, content);
        arguments.put(ClientArgument.BROKER, KubeCMDClient.getMessagingEndpoint(infraNamespace, addressSpace) + ":443");
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
        arguments.put(ClientArgument.BROKER, KubeCMDClient.getMessagingEndpoint(infraNamespace, addressSpace) + ":443");
        arguments.put(ClientArgument.ADDRESS, address);
        arguments.put(ClientArgument.COUNT, Integer.toString(count));
        arguments.put(ClientArgument.LOG_MESSAGES, "json");
        client.setArguments(arguments);

        return client.run(logToOutput);
    }

    private void ensurePersistentAuthService(String authServiceName) throws Exception {
        AuthenticationService authService = kubernetes.getAuthenticationServiceClient(infraNamespace).withName(authServiceName).get();

        if (authService.getSpec() != null && authService.getSpec().getStandard() != null) {

            AuthenticationServiceSpecStandardStorage storage = authService.getSpec().getStandard().getStorage();
            if (storage == null || !AuthenticationServiceSpecStandardType.persistent_claim.equals(storage.getType())) {
                log.info("Installed auth service {} does not use persistent claim, recreating it ", authServiceName);
                resourcesManager.removeAuthService(authService);

                AuthenticationService replacement = AuthServiceUtils.createStandardAuthServiceObject(authServiceName, true);
                kubernetes.getAuthenticationServiceClient(infraNamespace).create(replacement);

                log.info("Replacement auth service : {}", replacement);

                Thread.sleep(30_000);
                TestUtils.waitUntilDeployed(infraNamespace);
            }
        }
    }

    private static Stream<Arguments> provideVersions() {
        return Arrays.stream(environment.getStartTemplates().split(",")).map(templates -> Arguments.of(getVersionFromTemplateDir(Paths.get(templates)), templates));
    }

    private void installEnmasseOLM(Path templateDir, String version) throws Exception {
        String manifestsImage = getManifestsImage(templateDir, version);
        String csvName = getCsvName(templateDir, version);
        operatorManager.olm().install(olmType, manifestsImage, csvName);

        Thread.sleep(30_000);

        KubeCMDClient.applyFromFile(infraNamespace, Paths.get(templateDir.toString(), "install", "components", "example-plans"));
        KubeCMDClient.applyFromFile(infraNamespace, Paths.get(templateDir.toString(), "install", "components", "example-authservices", "standard-authservice.yaml"));

        Thread.sleep(60_000);
        operatorManager.waitUntilOperatorReadyOlm(olmType);
        Thread.sleep(30_000);
    }

    private void upgradeEnmasseOLM(Path upgradeTemplates, String previousVersion) throws Exception {
        String newVersion = getVersionFromTemplateDir(upgradeTemplates);
        String manifestsImage = getManifestsImage(upgradeTemplates, newVersion);
        //update manifests image
        operatorManager.olm().buildPushCustomOperatorRegistry(infraNamespace, manifestsImage);

        //delete catalog pod to force update
        kubernetes.deletePod(infraNamespace, Map.of("olm.catalogSource", "enmasse-source"));

        String csvName = getCsvName(upgradeTemplates, newVersion);
        String catalogSourceName = "enmasse-source";
        String catalogNamespace = infraNamespace;
        //update subscription to point to new catalog and to use latest csv
        operatorManager.olm().applySubscription(infraNamespace, catalogSourceName, catalogNamespace, csvName);

        //should update
        Thread.sleep(300_000);
        checkImagesUpdated(getVersionFromTemplateDir(upgradeTemplates));
    }

    private String getManifestsImage(Path templateDir, String version) throws IOException, JsonProcessingException, JsonMappingException {
        String manifestsImage;
        if (version.equals("1.3")) {
            String exampleCatalogSource = Files.readString(Paths.get(templateDir.toString(), "install", "components", "enmasse-operator", "050-Deployment-enmasse-operator.yaml"));
            var yaml = new YAMLMapper().readTree(exampleCatalogSource);
            var spec = yaml.get("spec").get("template").get("spec");
            manifestsImage = spec.get("containers").get(0).get("image").asText();
        } else {
            String exampleCatalogSource = Files.readString(Paths.get(templateDir.toString(), "install", "components", "example-olm", "catalog-source.yaml"));
            var tree = new YAMLMapper().readTree(exampleCatalogSource);
            manifestsImage = tree.get("spec").get("image").asText();
        }
        return manifestsImage;
    }

    private String getCsvName(Path templateDir, String version) throws Exception {
        if (version.equals("1.3") || version.startsWith("0.30")) {
            String enmasseCSV = Files.readString(Paths.get(templateDir.toString(), "install", "olm", productName, "enmasse.clusterserviceversion.yaml"));
            var yaml = new YAMLMapper().readTree(enmasseCSV);
            return yaml.get("metadata").get("name").asText();
        } else {
            String enmasseCSV = Files.readString(Paths.get(templateDir.toString(), "install", "components", "example-olm", "subscription.yaml"));
            var yaml = new YAMLMapper().readTree(enmasseCSV);
            return yaml.get("spec").get("startingCSV").asText();
        }
    }

}
