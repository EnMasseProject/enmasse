/*
 * Copyright 2019, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.systemtest.manager;

import io.enmasse.address.model.AddressSpace;
import io.enmasse.admin.model.v1.AddressPlan;
import io.enmasse.admin.model.v1.AddressSpacePlan;
import io.enmasse.admin.model.v1.AuthenticationService;
import io.enmasse.admin.model.v1.BrokeredInfraConfig;
import io.enmasse.admin.model.v1.InfraConfig;
import io.enmasse.admin.model.v1.StandardInfraConfig;
import io.enmasse.systemtest.Environment;
import io.enmasse.systemtest.amqp.AmqpClientFactory;
import io.enmasse.systemtest.logs.CustomLogger;
import io.enmasse.systemtest.mqtt.MqttClientFactory;
import io.enmasse.systemtest.platform.Kubernetes;
import io.enmasse.systemtest.time.SystemtestsOperation;
import io.enmasse.systemtest.time.TimeMeasuringSystem;
import io.enmasse.systemtest.time.TimeoutBudget;
import io.enmasse.systemtest.utils.AddressSpaceUtils;
import io.enmasse.systemtest.utils.TestUtils;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class CommonResourcesManager extends ResourceManager {

    private static CommonResourcesManager managerInstance = null;
    private static Logger LOGGER;
    protected List<AddressSpace> currentAddressSpaces;
    protected AmqpClientFactory amqpClientFactory;
    protected MqttClientFactory mqttClientFactory;
    boolean reuseAddressSpace = false;
    private ArrayList<AddressPlan> addressPlans;
    private ArrayList<AddressSpacePlan> addressSpacePlans;
    private ArrayList<StandardInfraConfig> standardInfraConfigs;
    private ArrayList<BrokeredInfraConfig> brokeredInfraConfigs;
    private ArrayList<AuthenticationService> authServices;

    private CommonResourcesManager() {
        LOGGER = CustomLogger.getLogger();
        addressPlans = new ArrayList<>();
        addressSpacePlans = new ArrayList<>();
        standardInfraConfigs = new ArrayList<>();
        brokeredInfraConfigs = new ArrayList<>();
        authServices = new ArrayList<>();
        currentAddressSpaces = new ArrayList<>();
    }

    //------------------------------------------------------------------------------------------------
    // Singleton handling
    //------------------------------------------------------------------------------------------------

    public List<AddressSpace> getCurrentAddressSpaces() {
        return currentAddressSpaces;
    }

    public static synchronized CommonResourcesManager getInstance() {
        if (managerInstance == null) {
            managerInstance = new CommonResourcesManager();
        }
        return managerInstance;
    }


    @Override
    public void initFactories(AddressSpace addressSpace) {
        amqpClientFactory = new AmqpClientFactory(addressSpace, defaultCredentials);
        mqttClientFactory = new MqttClientFactory(addressSpace, defaultCredentials);
    }

    @Override
    public void setup() {
        if (!reuseAddressSpace) {
            currentAddressSpaces = new ArrayList<>();
            initFactories(null);
        }
    }

    @Override
    public void tearDown(ExtensionContext context) throws Exception {
        LOGGER.info("Reuse addressspace: " + reuseAddressSpace);
        LOGGER.info("Environment cleanup: " + Environment.getInstance().skipCleanup());

        if (!Environment.getInstance().skipCleanup() && !reuseAddressSpace) {
            for (AddressSpacePlan addressSpacePlan : addressSpacePlans) {
                Kubernetes.getInstance().getAddressSpacePlanClient().withName(addressSpacePlan.getMetadata().getName()).cascading(true).delete();
                LOGGER.info("AddressSpace plan {} deleted", addressSpacePlan.getMetadata().getName());
            }
            addressSpacePlans.clear();

            for (AddressPlan addressPlan : addressPlans) {
                Kubernetes.getInstance().getAddressPlanClient().withName(addressPlan.getMetadata().getName()).cascading(true).delete();
                LOGGER.info("Address plan {} deleted", addressPlan.getMetadata().getName());
            }
            addressPlans.clear();

            for (StandardInfraConfig infraConfigDefinition : standardInfraConfigs) {
                Kubernetes.getInstance().getStandardInfraConfigClient().withName(infraConfigDefinition.getMetadata().getName()).cascading(true).delete();
                LOGGER.info("Standardinfraconfig {} deleted", infraConfigDefinition.getMetadata().getName());
            }
            standardInfraConfigs.clear();

            for (BrokeredInfraConfig infraConfigDefinition : brokeredInfraConfigs) {
                Kubernetes.getInstance().getBrokeredInfraConfigClient().withName(infraConfigDefinition.getMetadata().getName()).cascading(true).delete();
                LOGGER.info("Brokeredinfraconfig {} deleted", infraConfigDefinition.getMetadata().getName());
            }
            brokeredInfraConfigs.clear();

            for (AuthenticationService authService : authServices) {
                Kubernetes.getInstance().getAuthenticationServiceClient().withName(authService.getMetadata().getName()).cascading(true).delete();
                TestUtils.waitForNReplicas(0, false, Map.of("name", authService.getMetadata().getName()), Collections.emptyMap(), new TimeoutBudget(1, TimeUnit.MINUTES), 5000);
                LOGGER.info("AuthService {} deleted", authService.getMetadata().getName());
            }
            authServices.clear();


            closeClientFactories(amqpClientFactory, mqttClientFactory);
            amqpClientFactory = null;
            mqttClientFactory = null;
        }

    }


    //------------------------------------------------------------------------------------------------
    // Address plans
    //------------------------------------------------------------------------------------------------

    public void createAddressPlan(AddressPlan addressPlan) throws Exception {
        createAddressPlan(addressPlan, false);
        addressPlans.add(addressPlan);
    }

    public void removeAddressPlan(AddressPlan addressPlan) throws Exception {
        super.removeAddressPlan(addressPlan);
        addressPlans.removeIf(addressPlanIter -> addressPlanIter.getMetadata().getName().equals(addressPlan.getMetadata().getName()));
    }

    public void replaceAddressPlan(AddressPlan plan) throws Exception {
        super.createAddressPlan(plan, true);
    }

    public AddressPlan getAddressPlan(String name) throws Exception {
        return super.getAddressPlan(name);
    }

    //------------------------------------------------------------------------------------------------
    // Address space plans
    //------------------------------------------------------------------------------------------------

    public void createAddressSpacePlan(AddressSpacePlan addressSpacePlan) throws Exception {
        super.createAddressSpacePlan(addressSpacePlan);
        addressSpacePlans.add(addressSpacePlan);
    }

    public void removeAddressSpacePlan(AddressSpacePlan addressSpacePlan) throws Exception {
        super.removeAddressSpacePlan(addressSpacePlan);
        addressSpacePlans.removeIf(spacePlanIter -> spacePlanIter.getMetadata().getName().equals(addressSpacePlan.getMetadata().getName()));
    }

    public AddressSpacePlan getAddressSpacePlan(String config) throws Exception {
        return super.getAddressSpacePlan(config);
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

    public void createInfraConfig(InfraConfig infraConfigDefinition) throws Exception {
        if (infraConfigDefinition instanceof StandardInfraConfig) {
            super.createInfraConfig((StandardInfraConfig) infraConfigDefinition);
            standardInfraConfigs.add((StandardInfraConfig) infraConfigDefinition);
        } else {
            super.createInfraConfig((BrokeredInfraConfig) infraConfigDefinition);
            brokeredInfraConfigs.add((BrokeredInfraConfig) infraConfigDefinition);
        }
    }

    public void removeInfraConfig(InfraConfig infraConfigDefinition) throws Exception {
        if (infraConfigDefinition instanceof StandardInfraConfig) {
            super.removeInfraConfig((StandardInfraConfig) infraConfigDefinition);
            standardInfraConfigs.removeIf(infraId -> infraId.getMetadata().getName().equals(infraConfigDefinition.getMetadata().getName()));
        } else {
            super.removeInfraConfig((BrokeredInfraConfig) infraConfigDefinition);
            brokeredInfraConfigs.removeIf(infraId -> infraId.getMetadata().getName().equals(infraConfigDefinition.getMetadata().getName()));
        }
    }

    //------------------------------------------------------------------------------------------------
    // Authentication services
    //------------------------------------------------------------------------------------------------

    public AuthenticationService getAuthService(String name) throws Exception {
        return Kubernetes.getInstance().getAuthenticationServiceClient().withName(name).get();
    }

    public void replaceAuthService(AuthenticationService authService) throws Exception {
        replaceAuthService(authService, false);
    }

    public void replaceAuthService(AuthenticationService authenticationService, boolean replaceExisting) throws Exception {
        if (replaceExisting) {
            super.replaceAuthService(authenticationService);
        } else {
            super.createAuthService(authenticationService);
            authServices.add(authenticationService);
        }
    }

    public void createAuthService(AuthenticationService authenticationService) throws Exception {
        super.createAuthService(authenticationService);
        authServices.add(authenticationService);
    }


    public void removeAuthService(AuthenticationService authService) throws Exception {
        super.removeAuthService(authService);
        authServices.removeIf(authserviceId -> authserviceId.getMetadata().getName().equals(authService.getMetadata().getName()));
    }

    public void createAddressSpace(AddressSpace addressSpace) throws Exception {
        LOGGER.warn("Address space " + addressSpace);
        if (!AddressSpaceUtils.existAddressSpace(addressSpace.getMetadata().getNamespace(), addressSpace.getMetadata().getName())) {
            super.createAddressSpace(addressSpace);
            currentAddressSpaces.add(addressSpace);
        } else {
            super.waitForAddressSpaceReady(addressSpace);
        }
    }

    public void setReuseAddressSpace() {
        reuseAddressSpace = true;
    }

    public void unsetReuseAddressSpace() {
        reuseAddressSpace = false;
    }

    //================================================================================================
    //==================================== AddressSpace methods ======================================
    //================================================================================================

    public void createAddressSpaceList(AddressSpace... addressSpaces) throws Exception {
        String operationID = TimeMeasuringSystem.startOperation(SystemtestsOperation.CREATE_ADDRESS_SPACE);
        ArrayList<AddressSpace> spaces = new ArrayList<>();
        for (AddressSpace addressSpace : addressSpaces) {
            if (!AddressSpaceUtils.existAddressSpace(addressSpace.getMetadata().getNamespace(), addressSpace.getMetadata().getName())) {
                LOGGER.info("Address space '" + addressSpace + "' doesn't exist and will be created.");
                spaces.add(addressSpace);
            } else {
                LOGGER.warn("Address space '" + addressSpace + "' already exists.");
            }
        }
        currentAddressSpaces.addAll(spaces);
        super.createAddressSpaces(spaces, operationID);
    }

    public void replaceAddressSpace(AddressSpace addressSpace) throws Exception {
        super.replaceAddressSpace(addressSpace, true, currentAddressSpaces);
    }

    public void replaceAddressSpace(AddressSpace addressSpace, boolean replaceExisting) throws Exception {
        super.replaceAddressSpace(addressSpace, replaceExisting, currentAddressSpaces);
    }

    public void deleteAddressspacesFromList() throws Exception {
        LOGGER.info("All addressspaces will be removed");
        for (AddressSpace addressSpace : currentAddressSpaces) {
            deleteAddressSpace(addressSpace);
        }
        currentAddressSpaces.clear();
    }

    public void addToAddressSpaces(AddressSpace addressSpace) {
        this.currentAddressSpaces.add(addressSpace);
    }

    public void deleteAddressSpaceCreatedBySC(AddressSpace addressSpace) throws Exception {
        TestUtils.deleteAddressSpaceCreatedBySC(kubernetes, addressSpace, logCollector);
    }

    public AmqpClientFactory getAmqpClientFactory() {
        return amqpClientFactory;
    }

    @Override
    public void setAmqpClientFactory(AmqpClientFactory amqpClientFactory) {
        this.amqpClientFactory = amqpClientFactory;
    }

    public MqttClientFactory getMqttClientFactory() {
        return mqttClientFactory;
    }

    @Override
    public void setMqttClientFactory(MqttClientFactory mqttClientFactory) {
        this.mqttClientFactory = mqttClientFactory;
    }
}
