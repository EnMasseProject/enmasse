/*
 * Copyright 2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.systemtest.manager;

import io.enmasse.address.model.Address;
import io.enmasse.address.model.AddressSpace;
import io.enmasse.address.model.AddressSpaceBuilder;
import io.enmasse.admin.model.v1.AddressPlan;
import io.enmasse.admin.model.v1.AddressSpacePlan;
import io.enmasse.admin.model.v1.AuthenticationService;
import io.enmasse.admin.model.v1.BrokeredInfraConfig;
import io.enmasse.admin.model.v1.InfraConfig;
import io.enmasse.admin.model.v1.StandardInfraConfig;
import io.enmasse.config.AnnotationKeys;
import io.enmasse.iot.model.v1.IoTConfig;
import io.enmasse.iot.model.v1.IoTProject;
import io.enmasse.systemtest.Environment;
import io.enmasse.systemtest.UserCredentials;
import io.enmasse.systemtest.amqp.AmqpClient;
import io.enmasse.systemtest.amqp.AmqpClientFactory;
import io.enmasse.systemtest.bases.ThrowableRunner;
import io.enmasse.systemtest.iot.IoTConstants;
import io.enmasse.systemtest.logs.CustomLogger;
import io.enmasse.systemtest.logs.GlobalLogCollector;
import io.enmasse.systemtest.model.address.AddressType;
import io.enmasse.systemtest.model.addressplan.DestinationPlan;
import io.enmasse.systemtest.model.addressspace.AddressSpaceType;
import io.enmasse.systemtest.mqtt.MqttClientFactory;
import io.enmasse.systemtest.platform.Kubernetes;
import io.enmasse.systemtest.platform.apps.SystemtestsKubernetesApps;
import io.enmasse.systemtest.time.SystemtestsOperation;
import io.enmasse.systemtest.time.TimeMeasuringSystem;
import io.enmasse.systemtest.time.TimeoutBudget;
import io.enmasse.systemtest.utils.AddressSpaceUtils;
import io.enmasse.systemtest.utils.AddressUtils;
import io.enmasse.systemtest.utils.IoTUtils;
import io.enmasse.systemtest.utils.TestUtils;
import io.enmasse.systemtest.utils.UserUtils;
import io.enmasse.user.model.v1.Operation;
import io.enmasse.user.model.v1.User;
import io.enmasse.user.model.v1.UserAuthenticationType;
import io.enmasse.user.model.v1.UserBuilder;
import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.client.dsl.MixedOperation;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.slf4j.Logger;

import java.nio.file.Path;
import java.util.List;
import java.util.Objects;
import java.util.Random;
import java.util.Stack;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static io.enmasse.systemtest.time.TimeoutBudget.ofDuration;
import static java.time.Duration.ofMinutes;

/**
 * Managing resources used in test
 */
public class ResourceManager {

    private static final Logger LOGGER = CustomLogger.getLogger();

    private static Stack<ThrowableRunner> classResources = new Stack<>();
    private static Stack<ThrowableRunner> methodResources = new Stack<>();
    private static Stack<ThrowableRunner> pointerResources = classResources;
    private Kubernetes kubeClient = Kubernetes.getInstance();
    private final Environment environment = Environment.getInstance();
    private AmqpClientFactory amqpClientFactory = null;
    private MqttClientFactory mqttClientFactory = null;
    private final GlobalLogCollector logCollector = new GlobalLogCollector(kubeClient, environment.testLogDir());

    private IoTConfig defaultIoTConfig;
    private IoTProject defaultIoTProject;
    private AddressSpace defaultAddressSpace;

    private static ResourceManager instance;

    public static synchronized ResourceManager getInstance() {
        if (instance == null) {
            instance = new ResourceManager();
        }
        return instance;
    }

    public Stack<ThrowableRunner> getPointerResources() {
        return pointerResources;
    }

    public void setMethodResources() {
        LOGGER.info("Setting pointer to method resources");
        pointerResources = methodResources;
        initFactories(null);
    }

    public void setClassResources() {
        LOGGER.info("Setting pointer to class resources");
        pointerResources = classResources;
    }

    //------------------------------------------------------------------------------------------------
    // Client factories
    //------------------------------------------------------------------------------------------------

    public void initFactories(AddressSpace addressSpace) {
        amqpClientFactory = new AmqpClientFactory(addressSpace, environment.getSharedDefaultCredentials());
        mqttClientFactory = new MqttClientFactory(addressSpace, environment.getSharedDefaultCredentials());
    }

    public void initFactories(AddressSpace addressSpace, UserCredentials cred) {
        amqpClientFactory = new AmqpClientFactory(addressSpace, cred);
        mqttClientFactory = new MqttClientFactory(addressSpace, cred);
    }

    public void initFactories(IoTProject project, UserCredentials credentials) {
        String addSpaceName = project.getSpec().getDownstreamStrategy().getManagedStrategy().getAddressSpace().getName();
        initFactories(kubeClient.getAddressSpaceClient(project.getMetadata()
                .getNamespace()).withName(addSpaceName).get(), credentials);
    }

    public void closeAmqpFactory() throws Exception {
        if (amqpClientFactory != null) {
            amqpClientFactory.close();
            amqpClientFactory = null;
        }
    }

    public void closeMqttFactory() {
        if (mqttClientFactory != null) {
            mqttClientFactory.close();
            mqttClientFactory = null;
        }
    }

    public void closeFactories() throws Exception {
        closeAmqpFactory();
        closeMqttFactory();
    }

    public AmqpClientFactory getAmqpClientFactory() {
        return amqpClientFactory;
    }

    public MqttClientFactory getMqttClientFactory() {
        return mqttClientFactory;
    }

    //------------------------------------------------------------------------------------------------
    // Common resource methods
    //------------------------------------------------------------------------------------------------

    public <T extends HasMetadata> void createResource(T... resources) throws Exception {
        for (T resource : resources) {
            switch (resource.getKind()) {
                case StandardInfraConfig.KIND:
                    createInfraConfig((StandardInfraConfig) resource);
                    break;
                case BrokeredInfraConfig.KIND:
                    createInfraConfig((BrokeredInfraConfig) resource);
                    break;
                case AuthenticationService.KIND:
                    createAuthService((AuthenticationService) resource);
                    break;
                case AddressPlan.KIND:
                    createAddressPlan((AddressPlan) resource);
                    break;
                case AddressSpacePlan.KIND:
                    createAddressSpacePlan((AddressSpacePlan) resource);
                    break;
                case AddressSpace.KIND:
                    createAddressSpace((AddressSpace) resource);
                    break;
                case Address.KIND:
                    appendAddresses((Address) resource);
                    break;
                case IoTConfig.KIND:
                    IoTUtils.createIoTConfig((IoTConfig) resource);
                    scheduleDeletion(kubeClient.getIoTConfigClient(), (IoTConfig) resource);
                    break;
                case IoTProject.KIND:
                    IoTUtils.createIoTProject((IoTProject) resource);
                    scheduleDeletion(kubeClient.getIoTProjectClient(resource.getMetadata().getNamespace()), (IoTProject) resource);
                    break;
            }
        }
    }

    public <T extends HasMetadata> void removeResource(T... resources) throws Exception {
        for (T resource : resources) {
            switch (resource.getKind()) {
                case StandardInfraConfig.KIND:
                    removeInfraConfig((StandardInfraConfig) resource);
                    break;
                case BrokeredInfraConfig.KIND:
                    removeInfraConfig((BrokeredInfraConfig) resource);
                    break;
                case AuthenticationService.KIND:
                    removeAuthService((AuthenticationService) resource);
                    break;
                case AddressPlan.KIND:
                    removeAddressPlan((AddressPlan) resource);
                    break;
                case AddressSpacePlan.KIND:
                    removeAddressSpacePlan((AddressSpacePlan) resource);
                    break;
                case AddressSpace.KIND:
                    deleteAddressSpace((AddressSpace) resource);
                    break;
                case Address.KIND:
                    deleteAddresses((Address) resource);
                    break;
                case IoTConfig.KIND:
                    IoTUtils.deleteIoTConfigAndWait(kubeClient, (IoTConfig) resource);
                    break;
                case IoTProject.KIND:
                    IoTUtils.deleteIoTProjectAndWait(kubeClient, (IoTProject) resource);
                    break;
                case User.KIND:
                    removeUser((User) resource);
                    break;
            }
        }
    }

    public <T extends HasMetadata> T scheduleDeletion(MixedOperation<T, ?, ?, ?> operation, T resource) {
        LOGGER.info("Scheduled deletion of {} {} in namespace {}",
                resource.getKind(), resource.getMetadata().getName(), resource.getMetadata().getNamespace() == null ? "(not set)" : resource.getMetadata().getNamespace());
        switch (resource.getKind()) {
            case StandardInfraConfig.KIND:
                pointerResources.push(() -> {
                    removeInfraConfig((StandardInfraConfig) resource);
                    waitForDeletion((StandardInfraConfig) resource);
                });
                break;
            case BrokeredInfraConfig.KIND:
                pointerResources.push(() -> {
                    removeInfraConfig((BrokeredInfraConfig) resource);
                    waitForDeletion((BrokeredInfraConfig) resource);
                });
                break;
            case AddressPlan.KIND:
                pointerResources.push(() -> {
                    removeAddressPlan((AddressPlan) resource);
                    waitForDeletion((AddressPlan) resource);
                });
                break;
            case AddressSpacePlan.KIND:
                pointerResources.push(() -> {
                    LOGGER.info("Deleting {} {} in namespace {}",
                            resource.getKind(), resource.getMetadata().getName(), resource.getMetadata().getNamespace());
                    removeAddressSpacePlan((AddressSpacePlan) resource);
                    waitForDeletion((AddressSpacePlan) resource);
                });
                break;
            case AuthenticationService.KIND:
                pointerResources.push(() -> {
                    LOGGER.info("Deleting {} {} in namespace {}",
                            resource.getKind(), resource.getMetadata().getName(), resource.getMetadata().getNamespace());
                    removeAuthService((AuthenticationService) resource);
                });
                break;
            case Address.KIND:
                pointerResources.push(() -> {
                    LOGGER.info("Deleting {} {} in namespace {}",
                            resource.getKind(), resource.getMetadata().getName(), resource.getMetadata().getNamespace());
                    deleteAddresses((Address) resource);
                });
                break;
            case AddressSpace.KIND:
                pointerResources.add(() -> {
                    LOGGER.info("Deleting {} {} in namespace {}",
                            resource.getKind(), resource.getMetadata().getName(), resource.getMetadata().getNamespace());
                    deleteAddressSpace((AddressSpace) resource);
                });
                break;
            case IoTProject.KIND:
                pointerResources.add(() -> {
                    LOGGER.info("Deleting {} {} in namespace {}",
                            resource.getKind(), resource.getMetadata().getName(), resource.getMetadata().getNamespace());
                    IoTUtils.deleteIoTProjectAndWait(kubeClient, (IoTProject) resource);
                });
                break;
            case IoTConfig.KIND:
                pointerResources.add(() -> {
                    LOGGER.info("Deleting {} {} in namespace {}",
                            resource.getKind(), resource.getMetadata().getName(), resource.getMetadata().getNamespace());
                    IoTUtils.deleteIoTConfigAndWait(kubeClient, (IoTConfig) resource);
                });
                break;
            default:
                pointerResources.push(() -> {
                    LOGGER.info("Deleting {} {} in namespace {}",
                            resource.getKind(), resource.getMetadata().getName(), resource.getMetadata().getNamespace());
                    operation.inNamespace(resource.getMetadata().getNamespace())
                            .withName(resource.getMetadata().getName())
                            .cascading(true)
                            .delete();
                });
        }
        return resource;
    }

    private void waitForDeletion(InfraConfig infraConfig) {
        var client = infraConfig.getKind().equals(StandardInfraConfig.KIND) ? kubeClient.getStandardInfraConfigClient() : kubeClient.getBrokeredInfraConfigClient();
        TestUtils.waitUntilCondition(String.format("Deleting %s with name %s", infraConfig.getKind(), infraConfig.getMetadata().getName()), waitPhase ->
                        client.withName(infraConfig.getMetadata().getName()).get() == null,
                new TimeoutBudget(5, TimeUnit.MINUTES));
    }

    private void waitForDeletion(AddressPlan plan) {
        TestUtils.waitUntilCondition(String.format("Deleting %s with name %s", plan.getKind(), plan.getMetadata().getName()), waitPhase ->
                        kubeClient.getAddressPlanClient().withName(plan.getMetadata().getName()).get() == null,
                new TimeoutBudget(5, TimeUnit.MINUTES));
    }

    private void waitForDeletion(AddressSpacePlan plan) {
        TestUtils.waitUntilCondition(String.format("Deleting %s with name %s", plan.getKind(), plan.getMetadata().getName()), waitPhase ->
                        kubeClient.getAddressSpacePlanClient().withName(plan.getMetadata().getName()).get() == null,
                new TimeoutBudget(5, TimeUnit.MINUTES));
    }

    public void deleteClassResources() throws Exception {
        LOGGER.info("Going to clear all class resources");
        while (!classResources.empty()) {
            classResources.pop().run();
        }
        classResources.clear();
    }

    public void deleteMethodResources() throws Exception {
        LOGGER.info("Going to clear all method resources");
        while (!methodResources.empty()) {
            methodResources.pop().run();
        }
        methodResources.clear();
        pointerResources = classResources;
    }

    public void tearDown(ExtensionContext context) throws Exception {
        closeFactories();
        if (environment.skipCleanup()) {
            LOGGER.info("Skip cleanup is set, no cleanup process");
        } else {
            try {
                if (context.getExecutionException().isPresent()) {
                    Path path = TestUtils.getFailedTestLogsPath(context);
                    SystemtestsKubernetesApps.collectInfinispanServerLogs(path);
                }
                SystemtestsKubernetesApps.deleteInfinispanServer();
                SystemtestsKubernetesApps.deletePostgresqlServer();
                SystemtestsKubernetesApps.deleteH2Server();
            } catch (Exception e) {
                LOGGER.error("Error tearing down iot test: {}", e.getMessage());
                throw e;
            }
        }
    }


    //------------------------------------------------------------------------------------------------
    // Address plans
    //------------------------------------------------------------------------------------------------

    public void createAddressPlan(AddressPlan addressPlan) {
        LOGGER.info("Address plan {} will be created {}", addressPlan.getMetadata().getName(), addressPlan);
        var client = Kubernetes.getInstance().getAddressPlanClient();
        client.createOrReplace(addressPlan);
        scheduleDeletion(kubeClient.getAddressPlanClient(), addressPlan);
    }

    public void removeAddressPlan(AddressPlan addressPlan) throws Exception {
        Kubernetes.getInstance().getAddressPlanClient().withName(addressPlan.getMetadata().getName()).cascading(true).delete();
    }

    public AddressPlan getAddressPlan(String name) throws Exception {
        return Kubernetes.getInstance().getAddressPlanClient().withName(name).get();
    }

    //------------------------------------------------------------------------------------------------
    // Address space plans
    //------------------------------------------------------------------------------------------------

    public void createAddressSpacePlan(AddressSpacePlan addressSpacePlan) {
        createAddressSpacePlan(addressSpacePlan, true);
    }

    public void createAddressSpacePlan(AddressSpacePlan addressSpacePlan, boolean wait) {
        LOGGER.info("AddressSpace plan {} will be created {}", addressSpacePlan.getMetadata().getName(), addressSpacePlan);
        if (addressSpacePlan.getMetadata().getNamespace() == null || addressSpacePlan.getMetadata().getNamespace().equals("")) {
            addressSpacePlan.getMetadata().setNamespace(Kubernetes.getInstance().getInfraNamespace());
        }
        var client = Kubernetes.getInstance().getAddressSpacePlanClient();
        client.create(addressSpacePlan);
        if (wait) {
            TestUtils.waitForSchemaInSync(addressSpacePlan.getMetadata().getName());
        }
        scheduleDeletion(kubeClient.getAddressSpacePlanClient(), addressSpacePlan);
    }

    public void removeAddressSpacePlan(AddressSpacePlan addressSpacePlan) {
        Kubernetes.getInstance().getAddressSpacePlanClient().withName(addressSpacePlan.getMetadata().getName()).cascading(true).delete();
    }

    public AddressSpacePlan getAddressSpacePlan(String config) throws Exception {
        return Kubernetes.getInstance().getAddressSpacePlanClient().withName(config).get();
    }

    //------------------------------------------------------------------------------------------------
    // Infra configs
    //------------------------------------------------------------------------------------------------

    public BrokeredInfraConfig getBrokeredInfraConfig(String name) throws Exception {
        return Kubernetes.getInstance().getBrokeredInfraConfigClient().withName(name).get();
    }

    public StandardInfraConfig getStandardInfraConfig(String name) throws Exception {
        return Kubernetes.getInstance().getStandardInfraConfigClient().withName(name).get();
    }

    public void createInfraConfig(InfraConfig infraConfig) {
        LOGGER.info("InfraConfig {} will be created {}", infraConfig.getMetadata().getName(), infraConfig);
        if (infraConfig.getKind().equals(StandardInfraConfig.KIND)) {
            kubeClient.getStandardInfraConfigClient().createOrReplace((StandardInfraConfig) infraConfig);
            scheduleDeletion(kubeClient.getStandardInfraConfigClient(), (StandardInfraConfig) infraConfig);
        } else {
            kubeClient.getBrokeredInfraConfigClient().createOrReplace((BrokeredInfraConfig) infraConfig);
            scheduleDeletion(kubeClient.getBrokeredInfraConfigClient(), (BrokeredInfraConfig) infraConfig);
        }
    }

    public void removeInfraConfig(InfraConfig infraConfig) {
        LOGGER.info("InfraConfig {} will be deleted {}", infraConfig.getMetadata().getName(), infraConfig);
        if (infraConfig.getKind().equals(StandardInfraConfig.KIND)) {
            kubeClient.getStandardInfraConfigClient().withName(infraConfig.getMetadata().getName()).delete();
        } else {
            kubeClient.getBrokeredInfraConfigClient().withName(infraConfig.getMetadata().getName()).delete();
        }
    }

    //------------------------------------------------------------------------------------------------
    // Authentication services
    //------------------------------------------------------------------------------------------------

    public AuthenticationService getAuthService(String name) {
        return Kubernetes.getInstance().getAuthenticationServiceClient().withName(name).get();
    }

    public void createAuthService(AuthenticationService authenticationService) throws Exception {
        createAuthService(authenticationService, true);
    }

    public void createAuthService(AuthenticationService authenticationService, boolean wait) throws Exception {
        var client = Kubernetes.getInstance().getAuthenticationServiceClient();
        LOGGER.info("AuthService {} will be created {}", authenticationService.getMetadata().getName(), authenticationService);
        client.createOrReplace(authenticationService);
        if (wait) {
            waitForAuthPods(authenticationService);
        }
        scheduleDeletion(kubeClient.getAuthenticationServiceClient(), authenticationService);
    }

    public void waitForAuthPods(AuthenticationService authenticationService) {
        String desiredPodName = authenticationService.getMetadata().getName();
        TestUtils.waitUntilCondition("Auth service is deployed: " + desiredPodName, phase -> {
                    List<Pod> pods = TestUtils.listReadyPods(Kubernetes.getInstance(), authenticationService.getMetadata().getNamespace());
                    long matching = pods.stream().filter(pod ->
                            pod.getMetadata().getName().contains(desiredPodName)).count();
                    if (matching != 1) {
                        List<String> podNames = pods.stream().map(p -> p.getMetadata().getName()).collect(Collectors.toList());
                        LOGGER.info("Still awaiting pod with name : {}, matching : {}, current pods  {}",
                                desiredPodName, matching, podNames);
                    }

                    return matching == 1;
                },
                new TimeoutBudget(5, TimeUnit.MINUTES));
    }

    public void removeAuthService(AuthenticationService authService) {
        Kubernetes.getInstance().getAuthenticationServiceClient().withName(authService.getMetadata().getName()).cascading(true).delete();
        TestUtils.waitUntilCondition("Auth service is deleted: " + authService.getMetadata().getName(), (phase) ->
                        TestUtils.listReadyPods(Kubernetes.getInstance(), authService.getMetadata().getNamespace()).stream().noneMatch(pod ->
                                pod.getMetadata().getName().contains(authService.getMetadata().getName())),
                new TimeoutBudget(1, TimeUnit.MINUTES));
    }

    //------------------------------------------------------------------------------------------------
    // Address Space
    //------------------------------------------------------------------------------------------------

    public void createAddressSpace(AddressSpace addressSpace) throws Exception {
        createAddressSpace(addressSpace, true);
    }

    public void createAddressSpace(AddressSpace addressSpace, boolean waitUntilReady) throws Exception {
        String operationID = TimeMeasuringSystem.startOperation(SystemtestsOperation.CREATE_ADDRESS_SPACE);
        if (!AddressSpaceUtils.existAddressSpace(addressSpace.getMetadata().getNamespace(), addressSpace.getMetadata().getName())) {
            LOGGER.info("Address space '{}' doesn't exist and will be created.", addressSpace);
            kubeClient.getAddressSpaceClient(addressSpace.getMetadata().getNamespace()).createOrReplace(addressSpace);
            if (waitUntilReady) {
                AddressSpaceUtils.waitForAddressSpaceReady(addressSpace);
            }
        } else {
            if (waitUntilReady) {
                AddressSpaceUtils.waitForAddressSpaceReady(addressSpace);
            }
            LOGGER.info("Address space '" + addressSpace + "' already exists.");
        }
        AddressSpaceUtils.syncAddressSpaceObject(addressSpace);
        TimeMeasuringSystem.stopOperation(operationID);
        scheduleDeletion(kubeClient.getAddressSpaceClient(), addressSpace);
    }

    public void createAddressSpace(AddressSpace... addressSpaces) throws Exception {
        String operationID = TimeMeasuringSystem.startOperation(SystemtestsOperation.CREATE_ADDRESS_SPACE);
        for (AddressSpace addressSpace : addressSpaces) {
            if (!AddressSpaceUtils.existAddressSpace(addressSpace.getMetadata().getNamespace(), addressSpace.getMetadata().getName())) {
                LOGGER.info("Address space '{}' doesn't exist and will be created.", addressSpace);
                kubeClient.getAddressSpaceClient(addressSpace.getMetadata().getNamespace()).createOrReplace(addressSpace);
            } else {
                LOGGER.info("Address space '" + addressSpace + "' already exists.");
            }
        }
        for (AddressSpace addressSpace : addressSpaces) {
            AddressSpaceUtils.waitForAddressSpaceReady(addressSpace);
            AddressSpaceUtils.syncAddressSpaceObject(addressSpace);
            scheduleDeletion(kubeClient.getAddressSpaceClient(), addressSpace);
        }
        TimeMeasuringSystem.stopOperation(operationID);
    }

    public void waitForAddressSpaceReady(AddressSpace addressSpace) throws Exception {
        LOGGER.info("Waiting for address space ready");
        String operationID = TimeMeasuringSystem.startOperation(SystemtestsOperation.CREATE_ADDRESS_SPACE);
        AddressSpaceUtils.waitForAddressSpaceReady(addressSpace);
        AddressSpaceUtils.syncAddressSpaceObject(addressSpace);
        TimeMeasuringSystem.stopOperation(operationID);
    }

    public void deleteAddressSpace(AddressSpace addressSpace) throws Exception {
        if (AddressSpaceUtils.existAddressSpace(addressSpace.getMetadata().getNamespace(), addressSpace.getMetadata().getName())) {
            AddressSpaceUtils.deleteAddressSpaceAndWait(addressSpace, logCollector);
        } else {
            LOGGER.info("Address space '" + addressSpace.getMetadata().getName() + "' doesn't exists!");
        }
    }

    public void deleteAddressSpaceWithoutWait(AddressSpace addressSpace) throws Exception {
        if (AddressSpaceUtils.existAddressSpace(addressSpace.getMetadata().getNamespace(), addressSpace.getMetadata().getName())) {
            AddressSpaceUtils.deleteAddressSpace(addressSpace, logCollector);
        } else {
            LOGGER.info("Address space '" + addressSpace.getMetadata().getName() + "' doesn't exists!");
        }
    }

    public void replaceAddressSpace(AddressSpace addressSpace) throws Exception {
        replaceAddressSpace(addressSpace, true, new TimeoutBudget(10, TimeUnit.MINUTES));
    }

    public void replaceAddressSpace(AddressSpace addressSpace, boolean waitForConfigApplied, TimeoutBudget waitBudget) throws Exception {
        String operationID = TimeMeasuringSystem.startOperation(SystemtestsOperation.UPDATE_ADDRESS_SPACE);
        var client = kubeClient.getAddressSpaceClient(addressSpace.getMetadata().getNamespace());
        if (AddressSpaceUtils.existAddressSpace(addressSpace.getMetadata().getNamespace(), addressSpace.getMetadata().getName())) {
            LOGGER.info("Address space '{}' exists and will be updated.", addressSpace);
            final AddressSpace current = client.withName(addressSpace.getMetadata().getName()).get();
            final String currentResourceVersion = current.getMetadata().getResourceVersion();
            final String currentConfig = current.getAnnotation(AnnotationKeys.APPLIED_CONFIGURATION);
            client.createOrReplace(addressSpace);
            Thread.sleep(10_000);
            TestUtils.waitForChangedResourceVersion(ofDuration(ofMinutes(5)), addressSpace.getMetadata().getNamespace(), addressSpace.getMetadata().getName(), currentResourceVersion);
            if (waitForConfigApplied) {
                AddressSpaceUtils.waitForAddressSpaceConfigurationApplied(addressSpace, currentConfig);
            }
            if (waitBudget == null) {
                AddressSpaceUtils.waitForAddressSpaceReady(addressSpace);
            } else {
                AddressSpaceUtils.waitForAddressSpaceReady(addressSpace, waitBudget);
            }

            AddressSpaceUtils.syncAddressSpaceObject(addressSpace);
        } else {
            LOGGER.info("Address space '{}' does not exists.", addressSpace.getMetadata().getName());
        }
        LOGGER.info("Address space updated: {}", addressSpace);
        TimeMeasuringSystem.stopOperation(operationID);
    }

    public void addToAddressSpaces(AddressSpace addressSpace) {
        scheduleDeletion(kubeClient.getAddressSpaceClient(), addressSpace);
    }

    public AddressSpace getAddressSpace(String namespace, String addressSpaceName) {
        return kubeClient.getAddressSpaceClient(namespace).withName(addressSpaceName).get();
    }

    public AddressSpace getAddressSpace(String addressSpaceName) {
        return kubeClient.getAddressSpaceClient().withName(addressSpaceName).get();
    }


    //================================================================================================
    //====================================== Address methods =========================================
    //================================================================================================

    public void deleteAddresses(Address... destinations) {
        logCollector.collectConfigMaps();
        logCollector.collectRouterState("deleteAddresses");
        AddressUtils.delete(destinations);
    }

    public void deleteAddresses(AddressSpace addressSpace) throws Exception {
        LOGGER.info("Addresses in " + addressSpace.getMetadata().getName() + " will be deleted!");
        logCollector.collectConfigMaps();
        logCollector.collectRouterState("deleteAddresses");
        AddressUtils.delete(addressSpace);
    }


    public void appendAddresses(Address... destinations) throws Exception {
        TimeoutBudget budget = new TimeoutBudget(15, TimeUnit.MINUTES);
        appendAddresses(budget, destinations);
    }

    public void appendAddresses(TimeoutBudget timeout, Address... destinations) throws Exception {
        appendAddresses(true, timeout, destinations);
    }

    public void appendAddresses(boolean wait, Address... destinations) throws Exception {
        TimeoutBudget budget = new TimeoutBudget(15, TimeUnit.MINUTES);
        appendAddresses(wait, budget, destinations);
    }

    private void appendAddresses(boolean wait, TimeoutBudget timeout, Address... destinations) throws Exception {
        AddressUtils.appendAddresses(timeout, wait, destinations);
        for (Address addr : destinations) {
            scheduleDeletion(kubeClient.getAddressClient(), addr);
        }
        logCollector.collectConfigMaps();
    }

    public void setAddresses(TimeoutBudget budget, Address... addresses) throws Exception {
        logCollector.collectRouterState("setAddresses");
        AddressUtils.setAddresses(budget, true, addresses);
    }

    public void setAddresses(Address... addresses) throws Exception {
        setAddresses(new TimeoutBudget(15, TimeUnit.MINUTES), addresses);
        for (Address addr : addresses) {
            scheduleDeletion(kubeClient.getAddressClient(), addr);
        }
    }

    public void replaceAddress(Address destination) throws Exception {
        AddressUtils.replaceAddress(destination, true, new TimeoutBudget(10, TimeUnit.MINUTES));
    }

    public Address getAddress(String namespace, Address destination) {
        return kubeClient.getAddressClient().inNamespace(namespace).withName(destination.getMetadata().getName()).get();
    }

    //================================================================================================
    //======================================= User methods ===========================================
    //================================================================================================

    public User createOrUpdateUser(AddressSpace addressSpace, UserCredentials credentials) throws Exception {
        return createOrUpdateUser(addressSpace, credentials, true);
    }

    public User createOrUpdateUser(AddressSpace addressSpace, UserCredentials credentials, boolean wait) throws Exception {
        Objects.requireNonNull(addressSpace);
        Objects.requireNonNull(credentials);
        User user = new UserBuilder()
                .withNewMetadata()
                .withName(addressSpace.getMetadata().getName() + "." + credentials.getUsername())
                .endMetadata()
                .withNewSpec()
                .withUsername(credentials.getUsername())
                .withNewAuthentication()
                .withType(UserAuthenticationType.password)
                .withNewPassword(UserUtils.passwordToBase64(credentials.getPassword()))
                .endAuthentication()
                .addNewAuthorization()
                .withAddresses("*")
                .addToOperations(Operation.send)
                .addToOperations(Operation.recv)
                .endAuthorization()
                .addNewAuthorization()
                .addToOperations(Operation.manage)
                .endAuthorization()
                .endSpec()
                .build();
        return createOrUpdateUser(addressSpace, user, wait);
    }

    public User createOrUpdateUser(AddressSpace addressSpace, User user) throws Exception {
        return createOrUpdateUser(addressSpace, user, true);
    }

    public User createOrUpdateUser(AddressSpace addressSpace, User user, boolean wait) throws Exception {
        if (user.getMetadata().getName() == null || !user.getMetadata().getName().contains(addressSpace.getMetadata().getName())) {
            user.getMetadata().setName(addressSpace.getMetadata().getName() + "." + user.getSpec().getUsername());
        }
        if (user.getMetadata().getNamespace() == null) {
            user.getMetadata().setNamespace(addressSpace.getMetadata().getNamespace());
        }
        LOGGER.info("User {} in address space {} will be created/replaced", user, addressSpace.getMetadata().getName());
        User existing = kubeClient.getUserClient(user.getMetadata().getNamespace()).withName(user.getMetadata().getName()).get();
        if (existing != null) {
            existing.setSpec(user.getSpec());
            user = existing;
        }
        kubeClient.getUserClient(user.getMetadata().getNamespace()).createOrReplace(user);
        if (wait) {
            return UserUtils.waitForUserActive(user, new TimeoutBudget(1, TimeUnit.MINUTES));
        }
        User createdUser = kubeClient.getUserClient(user.getMetadata().getNamespace()).withName(user.getMetadata().getName()).get();
        scheduleDeletion(kubeClient.getUserClient(), createdUser);
        return createdUser;
    }

    public User createUserServiceAccount(AddressSpace addressSpace, UserCredentials cred) throws Exception {
        LOGGER.info("ServiceAccount user {} in address space {} will be created", cred.getUsername(), addressSpace.getMetadata().getName());
        String serviceaccountName = kubeClient.createServiceAccount(cred.getUsername(), addressSpace.getMetadata().getNamespace());
        User user = new UserBuilder()
                .withNewMetadata()
                .withName(String.format("%s.%s", addressSpace.getMetadata().getName(),
                        Pattern.compile(".*:").matcher(serviceaccountName).replaceAll("")))
                .endMetadata()
                .withNewSpec()
                .withUsername(serviceaccountName)
                .withNewAuthentication()
                .withType(UserAuthenticationType.serviceaccount)
                .endAuthentication()
                .addNewAuthorization()
                .withAddresses("*")
                .addToOperations(Operation.send)
                .addToOperations(Operation.recv)
                .endAuthorization()
                .endSpec()
                .build();
        return createOrUpdateUser(addressSpace, user);
    }

    public void removeUser(AddressSpace addressSpace, User user) throws InterruptedException {
        Objects.requireNonNull(addressSpace);
        Objects.requireNonNull(user);
        LOGGER.info("User {} in address space {} will be removed", user.getMetadata().getName(), addressSpace.getMetadata().getName());
        kubeClient.getUserClient(addressSpace.getMetadata().getNamespace()).withName(user.getMetadata().getName()).cascading(true).delete();
        UserUtils.waitForUserDeleted(user.getMetadata().getNamespace(), user.getMetadata().getName(), new TimeoutBudget(1, TimeUnit.MINUTES));
    }

    public void removeUser(AddressSpace addressSpace, String userName) throws InterruptedException {
        Objects.requireNonNull(addressSpace);
        LOGGER.info("User {} in address space {} will be removed", userName, addressSpace.getMetadata().getName());
        String name = String.format("%s.%s", addressSpace.getMetadata().getName(), userName);
        kubeClient.getUserClient(addressSpace.getMetadata().getNamespace()).withName(name).cascading(true).delete();
        UserUtils.waitForUserDeleted(addressSpace.getMetadata().getNamespace(), name, new TimeoutBudget(1, TimeUnit.MINUTES));
    }

    public void removeUser(User user) throws InterruptedException {
        LOGGER.info("User {} will be removed", user.getMetadata().getName());
        kubeClient.getUserClient(user.getMetadata().getNamespace()).withName(user.getMetadata().getName()).cascading(true).delete();
        UserUtils.waitForUserDeleted(user.getMetadata().getNamespace(), user.getMetadata().getName(), new TimeoutBudget(1, TimeUnit.MINUTES));
    }

    public User getUser(AddressSpace addressSpace, String username) {
        String id = String.format("%s.%s", addressSpace.getMetadata().getName(), username);
        List<User> response = kubeClient.getUserClient(addressSpace.getMetadata().getNamespace()).list().getItems();
        LOGGER.info("User list for {}: {}", addressSpace.getMetadata().getName(), response);
        for (User user : response) {
            if (user.getMetadata().getName().equals(id)) {
                LOGGER.info("User {} in addressspace {} already exists", username, addressSpace.getMetadata().getName());
                return user;
            }
        }
        return null;
    }

    //================================================================================================
    //======================================= Default messaging ======================================
    //================================================================================================

    public void createDefaultIoT() throws Exception {
        kubeClient.createNamespace(IoTConstants.IOT_PROJECT_NAMESPACE);
        UserCredentials credentials = new UserCredentials(UUID.randomUUID().toString(), UUID.randomUUID().toString());
        defaultIoTConfig = IoTUtils.getDefaultIoTConfig();
        createResource(defaultIoTConfig);
        defaultIoTProject = IoTUtils.getBasicIoTProjectObject("iot-project", "iot-" + new Random().nextInt(900) + 100,
                IoTConstants.IOT_PROJECT_NAMESPACE, IoTConstants.IOT_DEFAULT_ADDRESS_SPACE_PLAN);
        createResource(defaultIoTProject);
        createOrUpdateUser(getIotAddressSpace(defaultIoTProject), credentials);
        initFactories(defaultIoTProject, credentials);
    }

    public void createDefaultMessaging(AddressSpaceType type, String plan) throws Exception {
        defaultAddressSpace = new AddressSpaceBuilder()
                .withNewMetadata()
                .withName("default-address-space")
                .withNamespace(Kubernetes.getInstance().getInfraNamespace())
                .endMetadata()
                .withNewSpec()
                .withType(type.toString())
                .withPlan(plan)
                .withNewAuthenticationService()
                .withName("standard-authservice")
                .endAuthenticationService()
                .endSpec()
                .build();
        createAddressSpace(defaultAddressSpace);
        createOrUpdateUser(defaultAddressSpace, environment.getSharedDefaultCredentials());
        initFactories(defaultAddressSpace);
    }

    public AddressSpace getIotAddressSpace(IoTProject project) {
        String addSpaceName = project.getSpec().getDownstreamStrategy().getManagedStrategy().getAddressSpace().getName();
        return getAddressSpace(project.getMetadata().getNamespace(), addSpaceName);
    }

    public AddressSpace getDefaultIotAddressSpace() {
        String addSpaceName = defaultIoTProject.getSpec().getDownstreamStrategy().getManagedStrategy().getAddressSpace().getName();
        return getAddressSpace(defaultIoTProject.getMetadata().getNamespace(), addSpaceName);
    }

    public IoTConfig getDefaultIoTConfig() {
        return defaultIoTConfig;
    }

    public IoTProject getDefaultIoTProject() {
        return defaultIoTProject;
    }

    public AddressSpace getDefaultAddressSpace() {
        return defaultAddressSpace;
    }

    public String getDefaultTenantId() {
        return IoTUtils.getTenantId(defaultIoTProject);
    }

    public AmqpClient getAmqpClient() {
        try {
            return amqpClientFactory.createQueueClient();
        } catch (Exception ex) {
            LOGGER.error("Cannot create amqp client", ex);
            return null;
        }
    }

    public String getTenantId(IoTProject ioTProject) {
        return IoTUtils.getTenantId(ioTProject);
    }

    public String getDefaultAddressPlan(AddressType addressType) {
        if (getAddressSpaceType().equals(AddressSpaceType.BROKERED)) {
            switch (addressType) {
                case QUEUE:
                    return DestinationPlan.BROKERED_QUEUE;
                case TOPIC:
                    return DestinationPlan.BROKERED_TOPIC;
            }
        } else {
            switch (addressType) {
                case QUEUE:
                    return DestinationPlan.STANDARD_SMALL_QUEUE;
                case TOPIC:
                    return DestinationPlan.STANDARD_SMALL_TOPIC;
                case ANYCAST:
                    return DestinationPlan.STANDARD_SMALL_ANYCAST;
                case MULTICAST:
                    return DestinationPlan.STANDARD_SMALL_MULTICAST;
            }
        }
        return null;
    }

    public AddressSpaceType getAddressSpaceType() {
        return defaultAddressSpace.getSpec().getType().equals(AddressSpaceType.BROKERED.toString()) ? AddressSpaceType.BROKERED : AddressSpaceType.STANDARD;
    }
}

