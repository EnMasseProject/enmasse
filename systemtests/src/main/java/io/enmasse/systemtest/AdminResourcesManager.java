/*
 * Copyright 2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.systemtest;

import io.enmasse.admin.model.v1.*;
import io.enmasse.systemtest.apiclients.AdminApiClient;
import io.enmasse.systemtest.utils.TestUtils;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.concurrent.TimeUnit;

public class AdminResourcesManager {

    private static Logger log = CustomLogger.getLogger();
    private ArrayList<AddressPlan> addressPlans;
    private ArrayList<AddressSpacePlan> addressSpacePlans;
    private ArrayList<InfraConfig> infraConfigs;
    private ArrayList<AuthenticationService> authServices;
    private final AdminApiClient adminApiClient;

    public AdminResourcesManager(Kubernetes kubernetes) {
        this.adminApiClient = new AdminApiClient(kubernetes);
    }

    public void setUp() {
        addressPlans = new ArrayList<>();
        addressSpacePlans = new ArrayList<>();
        infraConfigs = new ArrayList<>();
        authServices = new ArrayList<>();
    }

    public void tearDown() throws Exception {
        if (!Environment.getInstance().skipCleanup()) {
            for (AddressSpacePlan addressSpacePlan : addressSpacePlans) {
                adminApiClient.deleteAddressSpacePlan(addressSpacePlan);
            }

            for (AddressPlan addressPlan : addressPlans) {
                adminApiClient.deleteAddressPlan(addressPlan);
            }

            for (InfraConfig infraConfigDefinition : infraConfigs) {
                adminApiClient.deleteInfraConfig(infraConfigDefinition);
            }

            for (AuthenticationService authService : authServices) {
                adminApiClient.deleteAuthService(authService);
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
        if (replaceExisting) {
            adminApiClient.replaceAddressPlan(addressPlan);
        } else {
            adminApiClient.createAddressPlan(addressPlan);
        }
        addressPlans.add(addressPlan);
    }

    public void removeAddressPlan(AddressPlan addressPlan) throws Exception {
        adminApiClient.deleteAddressPlan(addressPlan);
        addressPlans.removeIf(addressPlanIter -> addressPlanIter.getMetadata().getName().equals(addressPlan.getMetadata().getName()));
    }

    public void replaceAddressPlan(AddressPlan plan) throws Exception {
        adminApiClient.replaceAddressPlan(plan);
    }

    //------------------------------------------------------------------------------------------------
    // Address space plans
    //------------------------------------------------------------------------------------------------

    public void createAddressSpacePlan(AddressSpacePlan addressSpacePlan) throws Exception {
        createAddressSpacePlan(addressSpacePlan, false);
    }

    public void createAddressSpacePlan(AddressSpacePlan addressSpacePlan, boolean replaceExisting) throws Exception {
        if (replaceExisting) {
            adminApiClient.replaceAddressSpacePlan(addressSpacePlan);
        } else {
            adminApiClient.createAddressSpacePlan(addressSpacePlan);
        }
        addressSpacePlans.add(addressSpacePlan);
    }

    public void removeAddressSpacePlan(AddressSpacePlan addressSpacePlan) throws Exception {
        adminApiClient.deleteAddressSpacePlan(addressSpacePlan);
        addressSpacePlans.removeIf(spacePlanIter -> spacePlanIter.getMetadata().getName().equals(addressSpacePlan.getMetadata().getName()));
    }

    public AddressSpacePlan getAddressSpacePlan(String config) throws Exception {
        return adminApiClient.getAddressSpacePlan(config);
    }

    public AddressPlan getAddressPlan(String name) throws Exception {
        return adminApiClient.getAddressPlan(name);
    }

    //------------------------------------------------------------------------------------------------
    // Infra configs
    //------------------------------------------------------------------------------------------------

    public BrokeredInfraConfig getBrokeredInfraConfig(String name) throws Exception {
        return (BrokeredInfraConfig) adminApiClient.getInfraConfig(AddressSpaceType.BROKERED, name);
    }

    public StandardInfraConfig getStandardInfraConfig(String name) throws Exception {
        return (StandardInfraConfig) adminApiClient.getInfraConfig(AddressSpaceType.STANDARD, name);
    }

    public void createInfraConfig(InfraConfig infraConfigDefinition) throws Exception {
        createInfraConfig(infraConfigDefinition, false);
    }

    public void createInfraConfig(InfraConfig infraConfigDefinition, boolean replaceExisting) throws Exception {
        if (replaceExisting) {
            adminApiClient.replaceInfraConfig(infraConfigDefinition);
        } else {
            adminApiClient.createInfraConfig(infraConfigDefinition);
        }
        infraConfigs.add(infraConfigDefinition);
    }

    public void removeInfraConfig(InfraConfig infraConfigDefinition) throws Exception {
        adminApiClient.deleteInfraConfig(infraConfigDefinition);
        infraConfigs.removeIf(infraId -> infraId.getMetadata().getName().equals(infraConfigDefinition.getMetadata().getName()));
    }

    //------------------------------------------------------------------------------------------------
    // Authentication services
    //------------------------------------------------------------------------------------------------

    public AuthenticationService getAuthService(String name) throws Exception {
        return adminApiClient.getAuthService(name);
    }

    public void createAuthService(AuthenticationService authService) throws Exception {
        createAuthService(authService, false);
    }

    public void createAuthService(AuthenticationService authenticationService, boolean replaceExisting) throws Exception {
        if (replaceExisting) {
            adminApiClient.replaceAuthService(authenticationService);
        } else {
            adminApiClient.createAuthService(authenticationService);
            authServices.add(authenticationService);
        }
        TestUtils.waitUntilCondition("Auth service is deployed: " + authenticationService.getMetadata().getName(), (phase) ->
                        TestUtils.listReadyPods(Kubernetes.getInstance()).stream().filter(pod ->
                                pod.getMetadata().getName().contains(authenticationService.getMetadata().getName())).count() == 1,
                new TimeoutBudget(5, TimeUnit.MINUTES));
    }

    public void removeAuthService(AuthenticationService authService) throws Exception {
        adminApiClient.deleteAuthService(authService);
        authServices.removeIf(authserviceId -> authserviceId.getMetadata().getName().equals(authService.getMetadata().getName()));
        TestUtils.waitUntilCondition("Auth service is deleted: " + authService.getMetadata().getName(), (phase) ->
                        TestUtils.listReadyPods(Kubernetes.getInstance()).stream().noneMatch(pod ->
                                pod.getMetadata().getName().contains(authService.getMetadata().getName())),
                new TimeoutBudget(5, TimeUnit.MINUTES));
    }
}
