/*
 * Copyright 2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.systemtest;

import io.enmasse.admin.model.v1.*;
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

    private static Logger log = CustomLogger.getLogger();
    private ArrayList<AddressPlan> addressPlans;
    private ArrayList<AddressSpacePlan> addressSpacePlans;
    private ArrayList<StandardInfraConfig> standardInfraConfigs;
    private ArrayList<BrokeredInfraConfig> brokeredInfraConfigs;
    private ArrayList<AuthenticationService> authServices;

    public void setUp() {
        addressPlans = new ArrayList<>();
        addressSpacePlans = new ArrayList<>();
        standardInfraConfigs = new ArrayList<>();
        brokeredInfraConfigs = new ArrayList<>();
        authServices = new ArrayList<>();
    }

    public void tearDown() throws Exception {
        if (!Environment.getInstance().skipCleanup()) {
            for (AddressSpacePlan addressSpacePlan : addressSpacePlans) {
                Kubernetes.getInstance().getAddressSpacePlanClient().withName(addressSpacePlan.getMetadata().getName()).cascading(true).delete();
                log.info("AddressSpace plan {} deleted", addressSpacePlan.getMetadata().getName());
            }

            for (AddressPlan addressPlan : addressPlans) {
                Kubernetes.getInstance().getAddressPlanClient().withName(addressPlan.getMetadata().getName()).cascading(true).delete();
                log.info("Address plan {} deleted", addressPlan.getMetadata().getName());
            }

            for (StandardInfraConfig infraConfigDefinition : standardInfraConfigs) {
                Kubernetes.getInstance().getStandardInfraConfigClient().withName(infraConfigDefinition.getMetadata().getName()).cascading(true).delete();
                log.info("Standardinfraconfig {} deleted", infraConfigDefinition.getMetadata().getName());
            }

            for (BrokeredInfraConfig infraConfigDefinition : brokeredInfraConfigs) {
                Kubernetes.getInstance().getBrokeredInfraConfigClient().withName(infraConfigDefinition.getMetadata().getName()).cascading(true).delete();
                log.info("Brokeredinfraconfig {} deleted", infraConfigDefinition.getMetadata().getName());
            }

            for (AuthenticationService authService : authServices) {
                Kubernetes.getInstance().getAuthenticationServiceClient().withName(authService.getMetadata().getName()).cascading(true).delete();
                TestUtils.waitForNReplicas(0, false, Map.of("name", authService.getMetadata().getName()), Collections.emptyMap(), new TimeoutBudget(1, TimeUnit.MINUTES), 5000);
                log.info("AuthService {} deleted", authService.getMetadata().getName());
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
        log.info("Address plan {} will be created {}", addressPlan.getMetadata().getName(), addressPlan);
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
        log.info("AddressSpace plan {} will be created {}", addressSpacePlan.getMetadata().getName(), addressSpacePlan);
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
        log.info("InfraConfig {} will be created {}", infraConfigDefinition.getMetadata().getName(), infraConfigDefinition);
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
        log.info("AuthService {} will be created {}", authenticationService.getMetadata().getName(), authenticationService);
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
                        log.info("Still awaiting pod with name : {}, matching : {}, current pods  {}",
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
}
