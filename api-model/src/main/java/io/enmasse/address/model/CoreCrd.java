/*
 * Copyright 2018-2019, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.address.model;

import io.enmasse.api.model.*;
import io.enmasse.common.model.CustomResources;
import io.fabric8.kubernetes.api.model.apiextensions.CustomResourceDefinition;
import io.fabric8.kubernetes.internal.KubernetesDeserializer;

public class CoreCrd {

    public static final String VERSION = "v1beta1";
    public static final String VERSION_V1 = "v1";
    public static final String GROUP = "enmasse.io";
    public static final String API_VERSION = GROUP + "/" + VERSION;
    public static final String API_VERSION_V1 = GROUP + "/" + VERSION_V1;

    private static final CustomResourceDefinition ADDRESS_CRD;
    private static final CustomResourceDefinition ADDRESS_SPACE_CRD;
    private static final CustomResourceDefinition ADDRESS_SPACE_SCHEMA_CRD;
    private static final CustomResourceDefinition MESSAGING_INFRA_CRD;
    private static final CustomResourceDefinition MESSAGING_PROJECT_CRD;
    private static final CustomResourceDefinition MESSAGING_ADDRESS_CRD;
    private static final CustomResourceDefinition MESSAGING_ENDPOINT_CRD;
    private static final CustomResourceDefinition MESSAGING_PLAN_CRD;
    private static final CustomResourceDefinition MESSAGING_ADDRESS_PLAN_CRD;

    static {
        ADDRESS_CRD = CustomResources.createCustomResource(GROUP, VERSION, Address.KIND);
        ADDRESS_SPACE_CRD = CustomResources.createCustomResource(GROUP, VERSION, AddressSpace.KIND);
        ADDRESS_SPACE_SCHEMA_CRD = CustomResources.createCustomResource(GROUP, VERSION, AddressSpaceSchema.KIND, "Cluster");
        MESSAGING_INFRA_CRD = CustomResources.createCustomResource(GROUP, VERSION_V1, "MessagingInfrastructure");
        MESSAGING_PROJECT_CRD = CustomResources.createCustomResource(GROUP, VERSION_V1, "MessagingProject");
        MESSAGING_ADDRESS_CRD= CustomResources.createCustomResource(GROUP, VERSION_V1, "MessagingAddress");
        MESSAGING_ENDPOINT_CRD= CustomResources.createCustomResource(GROUP, VERSION_V1, "MessagingEndpoint");
        MESSAGING_PLAN_CRD= CustomResources.createCustomResource(GROUP, VERSION_V1, "MessagingPlan");
        MESSAGING_ADDRESS_PLAN_CRD= CustomResources.createCustomResource(GROUP, VERSION_V1, "MessagingAddressPlan");
    }

    public static void registerCustomCrds() {
        KubernetesDeserializer.registerCustomKind(API_VERSION, Address.KIND, Address.class);
        KubernetesDeserializer.registerCustomKind(API_VERSION, AddressList.KIND, AddressList.class);

        KubernetesDeserializer.registerCustomKind(API_VERSION, AddressSpace.KIND, AddressSpace.class);
        KubernetesDeserializer.registerCustomKind(API_VERSION, AddressSpaceList.KIND, AddressSpaceList.class);

        KubernetesDeserializer.registerCustomKind(API_VERSION, AddressSpaceSchema.KIND, AddressSpaceSchema.class);
        KubernetesDeserializer.registerCustomKind(API_VERSION, AddressSpaceSchemaList.KIND, AddressSpaceSchemaList.class);

        KubernetesDeserializer.registerCustomKind(API_VERSION_V1, "MessagingInfrastructure", MessagingInfrastructure.class);
        KubernetesDeserializer.registerCustomKind(API_VERSION_V1, "MessagingInfrastructureList", MessagingInfrastructureList.class);

        KubernetesDeserializer.registerCustomKind(API_VERSION_V1, "MessagingProject", MessagingProject.class);
        KubernetesDeserializer.registerCustomKind(API_VERSION_V1, "MessagingProjectList", MessagingProjectList.class);

        KubernetesDeserializer.registerCustomKind(API_VERSION_V1, "MessagingAddress", MessagingAddress.class);
        KubernetesDeserializer.registerCustomKind(API_VERSION_V1, "MessagingAddressList", MessagingAddressList.class);

        KubernetesDeserializer.registerCustomKind(API_VERSION_V1, "MessagingEndpoint", MessagingEndpoint.class);
        KubernetesDeserializer.registerCustomKind(API_VERSION_V1, "MessagingEndpointList", MessagingEndpointList.class);

        KubernetesDeserializer.registerCustomKind(API_VERSION_V1, "MessagingPlan", MessagingPlan.class);
        KubernetesDeserializer.registerCustomKind(API_VERSION_V1, "MessagingPlanList", MessagingPlanList.class);

        KubernetesDeserializer.registerCustomKind(API_VERSION_V1, "MessagingAddressPlan", MessagingAddressPlan.class);
        KubernetesDeserializer.registerCustomKind(API_VERSION_V1, "MessagingAddressPlanList", MessagingAddressPlanList.class);
    }
    public static CustomResourceDefinition addresses() {
        return ADDRESS_CRD;
    }

    public static CustomResourceDefinition addressSpaces() {
        return ADDRESS_SPACE_CRD;
    }

    public static CustomResourceDefinition addresseSpaceSchemas() {
        return ADDRESS_SPACE_SCHEMA_CRD;
    }

    public static CustomResourceDefinition messagingInfras() {
        return MESSAGING_INFRA_CRD;
    }

    public static CustomResourceDefinition messagingProjects() {
        return MESSAGING_PROJECT_CRD;
    }

    public static CustomResourceDefinition messagingAddresses() {
        return MESSAGING_ADDRESS_CRD;
    }

    public static CustomResourceDefinition messagingEndpoints() {
        return MESSAGING_ENDPOINT_CRD;
    }

    public static CustomResourceDefinition messagingPlans() {
        return MESSAGING_PLAN_CRD;
    }

    public static CustomResourceDefinition messagingAddressPlans() {
        return MESSAGING_ADDRESS_PLAN_CRD;
    }
}
