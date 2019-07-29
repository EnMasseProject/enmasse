/*
 * Copyright 2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.systemtest;

import io.enmasse.admin.model.v1.AddressPlan;
import io.enmasse.admin.model.v1.AddressSpacePlan;
import io.enmasse.admin.model.v1.AuthenticationService;
import io.enmasse.admin.model.v1.BrokeredInfraConfig;
import io.enmasse.admin.model.v1.InfraConfig;
import io.enmasse.admin.model.v1.StandardInfraConfig;
import io.enmasse.systemtest.ability.SharedAddressSpaceManager;
import io.enmasse.systemtest.utils.TestUtils;
import io.fabric8.kubernetes.api.model.Pod;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public class AdminResourcesManager {

    private static AdminResourcesManager managerInstance = null;
    private static Logger LOGGER;
    private ArrayList<AddressPlan> addressPlans;
    private ArrayList<AddressSpacePlan> addressSpacePlans;
    private ArrayList<StandardInfraConfig> standardInfraConfigs;
    private ArrayList<BrokeredInfraConfig> brokeredInfraConfigs;
    private ArrayList<AuthenticationService> authServices;
    private SharedAddressSpaceEnv sharedAddressSpaceEnv = null;

    private AdminResourcesManager() {
        LOGGER = CustomLogger.getLogger();
        addressPlans = new ArrayList<>();
        addressSpacePlans = new ArrayList<>();
        standardInfraConfigs = new ArrayList<>();
        brokeredInfraConfigs = new ArrayList<>();
        authServices = new ArrayList<>();
    }

    //------------------------------------------------------------------------------------------------
    // Singleton handling
    //------------------------------------------------------------------------------------------------


    public static synchronized AdminResourcesManager getInstance() {
        if (managerInstance == null) {
            managerInstance = new AdminResourcesManager();
        }
        return managerInstance;
    }

    public void tearDown() throws Exception {
        if (!Environment.getInstance().skipCleanup()) {
            for (AddressSpacePlan addressSpacePlan : addressSpacePlans) {
                if(!sharedAddressSpaceEnv.getAddressSpacePlanList().contains(addressSpacePlan)) {
                Kubernetes.getInstance().getAddressSpacePlanClient()
                        .withName(addressSpacePlan.getMetadata().getName()).cascading(true).delete();
                    LOGGER.info("AddressSpace plan {} deleted", addressSpacePlan.getMetadata().getName());
                }
            }

            for (AddressPlan addressPlan : addressPlans) {
                Kubernetes.getInstance().getAddressPlanClient().withName(addressPlan.getMetadata().getName()).cascading(true).delete();
                LOGGER.info("Address plan {} deleted", addressPlan.getMetadata().getName());
            }

            for (StandardInfraConfig infraConfigDefinition : standardInfraConfigs) {
                if (!infraConfigDefinition.getMetadata().getName()
                        .equals(sharedAddressSpaceEnv.getStandardInfra())) {
                Kubernetes.getInstance().getStandardInfraConfigClient()
                        .withName(infraConfigDefinition.getMetadata().getName()).cascading(true).delete();
                    LOGGER.info("Standardinfraconfig {} deleted", infraConfigDefinition.getMetadata().getName());
                }
            }

            for (BrokeredInfraConfig infraConfigDefinition : brokeredInfraConfigs) {
                if (!infraConfigDefinition.getMetadata().getName()
                        .equals(sharedAddressSpaceEnv.getBrokeredInfra())) {
                Kubernetes.getInstance().getBrokeredInfraConfigClient()
                        .withName(infraConfigDefinition.getMetadata().getName()).cascading(true).delete();
                    LOGGER.info("Brokeredinfraconfig {} deleted", infraConfigDefinition.getMetadata().getName());
                }
            }

            for (AuthenticationService authService : authServices) {
                Kubernetes.getInstance().getAuthenticationServiceClient().withName(authService.getMetadata().getName()).cascading(true).delete();
                TestUtils.waitForNReplicas(0, false, Map.of("name", authService.getMetadata().getName()), Collections.emptyMap(), new TimeoutBudget(1, TimeUnit.MINUTES), 5000);
                LOGGER.info("AuthService {} deleted", authService.getMetadata().getName());
            }

            addressPlans.clear();
            addressSpacePlans.clear();
        }

    }

    //------------------------------------------------------------------------------------------------
    // Address plans
    //------------------------------------------------------------------------------------------------

    public void createAddressPlan(AddressPlan addressPlan) throws Exception {
        createAddressPlan(addressPlan, false);
    }

    public void createAddressPlan(AddressPlan addressPlan, boolean replaceExisting) throws Exception {
        LOGGER.info("Address plan {} will be created {}", addressPlan.getMetadata().getName(), addressPlan);
        var client = Kubernetes.getInstance().getAddressPlanClient();
        if (replaceExisting) {
            client.createOrReplace(addressPlan);
        } else {
            client.create(addressPlan);
        }
        addressPlans.add(addressPlan);
        Thread.sleep(1000);
    }

    public void removeAddressPlan(AddressPlan addressPlan) throws Exception {
        Kubernetes.getInstance().getAddressPlanClient().withName(addressPlan.getMetadata().getName()).cascading(true).delete();
        addressPlans.removeIf(addressPlanIter -> addressPlanIter.getMetadata().getName().equals(addressPlan.getMetadata().getName()));
    }

    public void replaceAddressPlan(AddressPlan plan) throws Exception {
        Kubernetes.getInstance().getAddressPlanClient().createOrReplace(plan);
    }

    public AddressPlan getAddressPlan(String name) throws Exception {
        return Kubernetes.getInstance().getAddressPlanClient().withName(name).get();
    }

    //------------------------------------------------------------------------------------------------
    // Address space plans
    //------------------------------------------------------------------------------------------------

    public void createAddressSpacePlan(AddressSpacePlan addressSpacePlan) throws Exception {
        createAddressSpacePlan(addressSpacePlan, false);
    }

    public void createAddressSpacePlan(AddressSpacePlan addressSpacePlan, boolean replaceExisting) throws Exception {
        LOGGER.info("AddressSpace plan {} will be created {}", addressSpacePlan.getMetadata().getName(), addressSpacePlan);
        var client = Kubernetes.getInstance().getAddressSpacePlanClient();
        if (replaceExisting) {
            client.createOrReplace(addressSpacePlan);
        } else {
            client.create(addressSpacePlan);
        }
        addressSpacePlans.add(addressSpacePlan);
        Thread.sleep(1000);
    }

    public void removeAddressSpacePlan(AddressSpacePlan addressSpacePlan) throws Exception {
        Kubernetes.getInstance().getAddressSpacePlanClient().withName(addressSpacePlan.getMetadata().getName()).cascading(true).delete();
        addressSpacePlans.removeIf(spacePlanIter -> spacePlanIter.getMetadata().getName().equals(addressSpacePlan.getMetadata().getName()));
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

    public void createInfraConfig(InfraConfig infraConfigDefinition) throws Exception {
        LOGGER.info("InfraConfig {} will be created {}", infraConfigDefinition.getMetadata().getName(), infraConfigDefinition);
        if (infraConfigDefinition instanceof StandardInfraConfig) {
            var client = Kubernetes.getInstance().getStandardInfraConfigClient();
            client.createOrReplace((StandardInfraConfig) infraConfigDefinition);
            standardInfraConfigs.add((StandardInfraConfig) infraConfigDefinition);
        } else {
            var client = Kubernetes.getInstance().getBrokeredInfraConfigClient();
            client.createOrReplace((BrokeredInfraConfig) infraConfigDefinition);
            brokeredInfraConfigs.add((BrokeredInfraConfig) infraConfigDefinition);
        }
        Thread.sleep(1000);
    }

    public void removeInfraConfig(InfraConfig infraConfigDefinition) throws Exception {
        if (infraConfigDefinition instanceof StandardInfraConfig) {
            var client = Kubernetes.getInstance().getStandardInfraConfigClient();
            client.withName(infraConfigDefinition.getMetadata().getName()).cascading(true).delete();
            standardInfraConfigs.removeIf(infraId -> infraId.getMetadata().getName().equals(infraConfigDefinition.getMetadata().getName()));
        } else {
            var client = Kubernetes.getInstance().getBrokeredInfraConfigClient();
            client.withName(infraConfigDefinition.getMetadata().getName()).cascading(true).delete();
            brokeredInfraConfigs.removeIf(infraId -> infraId.getMetadata().getName().equals(infraConfigDefinition.getMetadata().getName()));
        }
    }

    //------------------------------------------------------------------------------------------------
    // Authentication services
    //------------------------------------------------------------------------------------------------

    public AuthenticationService getAuthService(String name) throws Exception {
        return Kubernetes.getInstance().getAuthenticationServiceClient().withName(name).get();
    }

    public void createAuthService(AuthenticationService authService) throws Exception {
        createAuthService(authService, false);
    }

    public void createAuthService(AuthenticationService authenticationService, boolean replaceExisting) throws Exception {
        LOGGER.info("AuthService {} will be created {}", authenticationService.getMetadata().getName(), authenticationService);
        var client = Kubernetes.getInstance().getAuthenticationServiceClient();
        if (replaceExisting) {
            client.createOrReplace(authenticationService);
        } else {
            client.create(authenticationService);
            authServices.add(authenticationService);
        }
        String desiredPodName = authenticationService.getMetadata().getName();
        TestUtils.waitUntilCondition("Auth service is deployed: " + desiredPodName, phase -> {
                    List<Pod> pods = TestUtils.listReadyPods(Kubernetes.getInstance());
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

    public void removeAuthService(AuthenticationService authService) throws Exception {
        Kubernetes.getInstance().getAuthenticationServiceClient().withName(authService.getMetadata().getName()).cascading(true).delete();
        authServices.removeIf(authserviceId -> authserviceId.getMetadata().getName().equals(authService.getMetadata().getName()));
        TestUtils.waitUntilCondition("Auth service is deleted: " + authService.getMetadata().getName(), (phase) ->
                        TestUtils.listReadyPods(Kubernetes.getInstance()).stream().noneMatch(pod ->
                                pod.getMetadata().getName().contains(authService.getMetadata().getName())),
                new TimeoutBudget(1, TimeUnit.MINUTES));
    }

    //------------------------------------------------------------------------------------------------
    // Shared address space custom config
    //------------------------------------------------------------------------------------------------

    public void deploySharedAddressSpaceEnv() throws Exception {
        sharedAddressSpaceEnv = new SharedAddressSpaceEnv();
        sharedAddressSpaceEnv.setupSharedAddressSpaceEnv();

        createInfraConfig(sharedAddressSpaceEnv.getBrokeredInfraConfig());
        createInfraConfig(sharedAddressSpaceEnv.getStandardInfraConfig());
        for (AddressSpacePlan addressSpacePlan : sharedAddressSpaceEnv.getAddressSpacePlanList()) {
            createAddressSpacePlan(addressSpacePlan);
        }
        Thread.sleep(20000);
    }

    public void tearDownSharedEnv() throws Exception {
        LOGGER.warn("Next tag is shared? " + SharedAddressSpaceManager.getInstance().isNextTagShared());
        if(!SharedAddressSpaceManager.getInstance().isNextTagShared()) {
            LOGGER.info("Shared env will be deleted");
            for (AddressSpacePlan addressSpacePlan : sharedAddressSpaceEnv.getAddressSpacePlanList()) {
                LOGGER.info("Address space plan " + addressSpacePlan.getDisplayName() + "is going to be deleted!");
                removeAddressSpacePlan(addressSpacePlan);
            }
            LOGGER.info("Infra config " + sharedAddressSpaceEnv.getStandardInfra() + "is going to be deleted!");
            removeInfraConfig(sharedAddressSpaceEnv.getStandardInfraConfig());
            LOGGER.info("Infra config " + sharedAddressSpaceEnv.getBrokeredInfra() + "is going to be deleted!");
            removeInfraConfig(sharedAddressSpaceEnv.getBrokeredInfraConfig());
        }
    }

    public SharedAddressSpaceEnv getSharedAddressSpaceEnv() {
        return sharedAddressSpaceEnv;
    }

}
