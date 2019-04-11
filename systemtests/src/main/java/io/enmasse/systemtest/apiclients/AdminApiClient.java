/*
 * Copyright 2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.systemtest.apiclients;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.enmasse.admin.model.v1.*;
import io.enmasse.systemtest.AddressSpaceType;
import io.enmasse.systemtest.CustomLogger;
import io.enmasse.systemtest.Kubernetes;
import io.enmasse.systemtest.utils.AuthServiceUtils;
import io.enmasse.systemtest.utils.PlanUtils;
import io.vertx.core.json.JsonObject;
import org.slf4j.Logger;

public class AdminApiClient extends ApiClient {
    protected static Logger log = CustomLogger.getLogger();
    private final String addressSpacePlansPath;
    private final String addressPlansPath;
    private final String brokeredInfraconfigPath;
    private final String standardInfraconfigPath;
    private final String authenticationServicePath;
    private final String consolePath;

    public AdminApiClient(Kubernetes kubernetes) {
        super(kubernetes, kubernetes::getMasterEndpoint, "admin.enmasse.io/" + AdminCrd.VERSION_V1BETA2);
        this.addressSpacePlansPath = String.format("/apis/admin.enmasse.io/%s/namespaces/%s/addressspaceplans", AdminCrd.VERSION_V1BETA2, kubernetes.getNamespace());
        this.addressPlansPath = String.format("/apis/admin.enmasse.io/%s/namespaces/%s/addressplans", AdminCrd.VERSION_V1BETA2, kubernetes.getNamespace());
        this.brokeredInfraconfigPath = String.format("/apis/admin.enmasse.io/%s/namespaces/%s/brokeredinfraconfigs", AdminCrd.VERSION_V1BETA1, kubernetes.getNamespace());
        this.standardInfraconfigPath = String.format("/apis/admin.enmasse.io/%s/namespaces/%s/standardinfraconfigs", AdminCrd.VERSION_V1BETA1, kubernetes.getNamespace());
        this.authenticationServicePath = String.format("/apis/admin.enmasse.io/%s/namespaces/%s/authenticationservices", AdminCrd.VERSION_V1BETA1, kubernetes.getNamespace());
        this.consolePath = String.format("/apis/admin.enmasse.io/%s/namespaces/%s/consoleservices", AdminCrd.VERSION_V1BETA1, kubernetes.getNamespace());
    }

    public void close() {
        client.close();
        vertx.close();
    }

    @Override
    protected String apiClientName() {
        return "AdminApi";
    }

    //////////////////////////////////////////////////////////////
    // AddressSpace plans
    //////////////////////////////////////////////////////////////

    public AddressSpacePlan getAddressSpacePlan(String name) throws Exception {
        JsonObject spacePlan = getResource("address-space-plan", addressSpacePlansPath, name);
        return PlanUtils.jsonToAddressSpacePlan(spacePlan);
    }

    public void createAddressSpacePlan(AddressSpacePlan addressSpacePlan) throws Exception {
        createResource("address-space-plan", addressSpacePlansPath, PlanUtils.addressSpacePlanToJson(addressSpacePlan));
    }

    public void replaceAddressSpacePlan(AddressSpacePlan addressSpacePlan) throws Exception {
        replaceResource("address-space-plan", addressSpacePlansPath, addressSpacePlan.getMetadata().getName(), PlanUtils.addressSpacePlanToJson(addressSpacePlan));
    }

    public void deleteAddressSpacePlan(AddressSpacePlan addressSpacePlan) throws Exception {
        deleteResource("address-space-plan", addressSpacePlansPath, addressSpacePlan.getMetadata().getName());
    }

    //////////////////////////////////////////////////////////////
    // Address Plans
    //////////////////////////////////////////////////////////////

    public AddressPlan getAddressPlan(String name) throws Exception {
        return PlanUtils.jsonToAddressPlan(getResource("address-plan", addressPlansPath, name));
    }

    public void createAddressPlan(AddressPlan addressPlan) throws Exception {
        createResource("address-plan", addressPlansPath, PlanUtils.addressPlanToJson(addressPlan));
    }

    public void replaceAddressPlan(AddressPlan addressPlan) throws Exception {
        replaceResource("address-plan", addressPlansPath, addressPlan.getMetadata().getName(), PlanUtils.addressPlanToJson(addressPlan));
    }

    public void deleteAddressPlan(AddressPlan addressPlan) throws Exception {
        deleteResource("address-plan", addressPlansPath, addressPlan.getMetadata().getName());
    }

    //////////////////////////////////////////////////////////////
    // Infra configs
    //////////////////////////////////////////////////////////////

    public void createInfraConfig(InfraConfig infraConfigDefinition) throws Exception {
        createResource("infra-config", getInfraApiPath(infraConfigDefinition), PlanUtils.infraToJson(infraConfigDefinition));
    }

    public void replaceInfraConfig(InfraConfig infraConfigDefinition) throws Exception {
        replaceResource("infra-config", getInfraApiPath(infraConfigDefinition), infraConfigDefinition.getMetadata().getName(), PlanUtils.infraToJson(infraConfigDefinition));
    }

    public void deleteInfraConfig(InfraConfig infraConfigDefinition) throws Exception {
        deleteResource("infra-config", getInfraApiPath(infraConfigDefinition), infraConfigDefinition.getMetadata().getName());
    }

    public InfraConfig getInfraConfig(AddressSpaceType type, String config) throws Exception {
        return PlanUtils.jsonToInfra(getResource("infra-config", type.equals(AddressSpaceType.STANDARD) ? standardInfraconfigPath : brokeredInfraconfigPath, config));
    }

    private String getInfraApiPath(InfraConfig config) {
        return config.getKind().equals("StandardInfraConfig") ? standardInfraconfigPath : brokeredInfraconfigPath;
    }

    //////////////////////////////////////////////////////////////
    // Authentication Service
    //////////////////////////////////////////////////////////////

    public AuthenticationService getAuthService(String name) throws Exception {
        return AuthServiceUtils.jsonToAuthenticationService(getResource("authentication-service", authenticationServicePath, name));
    }

    public void createAuthService(AuthenticationService authService) throws Exception {
        createResource("authentication-service", authenticationServicePath, AuthServiceUtils.authenticationServiceToJson(authService));
    }

    public void replaceAuthService(AuthenticationService authService) throws Exception {
        replaceResource("authentication-service", authenticationServicePath, authService.getMetadata().getName(), AuthServiceUtils.authenticationServiceToJson(authService));
    }

    public void deleteAuthService(AuthenticationService authService) throws Exception {
        deleteResource("authentication-service", authenticationServicePath, authService.getMetadata().getName());
    }

    //////////////////////////////////////////////////////////////
    // Authentication Service
    //////////////////////////////////////////////////////////////

    public ConsoleService getConsoleService(String name) throws Exception {
        return new ObjectMapper().readValue(getResource("console-service", consolePath, name).toString(), ConsoleService.class);
    }
}
