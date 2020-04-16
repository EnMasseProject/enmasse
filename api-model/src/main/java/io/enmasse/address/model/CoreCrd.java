/*
 * Copyright 2018-2019, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.address.model;

import io.enmasse.api.model.MessagingInfra;
import io.enmasse.api.model.MessagingInfraList;
import io.enmasse.api.model.MessagingTenant;
import io.enmasse.api.model.MessagingTenantList;
import io.enmasse.common.model.CustomResources;
import io.fabric8.kubernetes.api.model.apiextensions.CustomResourceDefinition;
import io.fabric8.kubernetes.internal.KubernetesDeserializer;

public class CoreCrd {

    public static final String VERSION = "v1beta1";
    public static final String VERSION_BETA2 = "v1beta2";
    public static final String GROUP = "enmasse.io";
    public static final String API_VERSION = GROUP + "/" + VERSION;
    public static final String API_VERSION_BETA2 = GROUP + "/" + VERSION_BETA2;

    private static final CustomResourceDefinition ADDRESS_CRD;
    private static final CustomResourceDefinition ADDRESS_SPACE_CRD;
    private static final CustomResourceDefinition ADDRESS_SPACE_SCHEMA_CRD;
    private static final CustomResourceDefinition MESSAGING_INFRA_CRD;
    private static final CustomResourceDefinition MESSAGING_TENANT_CRD;

    static {
        ADDRESS_CRD = CustomResources.createCustomResource(GROUP, VERSION, Address.KIND);
        ADDRESS_SPACE_CRD = CustomResources.createCustomResource(GROUP, VERSION, AddressSpace.KIND);
        ADDRESS_SPACE_SCHEMA_CRD = CustomResources.createCustomResource(GROUP, VERSION, AddressSpaceSchema.KIND, "Cluster");
        MESSAGING_INFRA_CRD = CustomResources.createCustomResource(GROUP, VERSION_BETA2, "MessagingInfra");
        MESSAGING_TENANT_CRD = CustomResources.createCustomResource(GROUP, VERSION_BETA2, "MessagingTenant");
    }

    public static void registerCustomCrds() {
        KubernetesDeserializer.registerCustomKind(API_VERSION, Address.KIND, Address.class);
        KubernetesDeserializer.registerCustomKind(API_VERSION, AddressList.KIND, AddressList.class);

        KubernetesDeserializer.registerCustomKind(API_VERSION, AddressSpace.KIND, AddressSpace.class);
        KubernetesDeserializer.registerCustomKind(API_VERSION, AddressSpaceList.KIND, AddressSpaceList.class);

        KubernetesDeserializer.registerCustomKind(API_VERSION, AddressSpaceSchema.KIND, AddressSpaceSchema.class);
        KubernetesDeserializer.registerCustomKind(API_VERSION, AddressSpaceSchemaList.KIND, AddressSpaceSchemaList.class);

        KubernetesDeserializer.registerCustomKind(API_VERSION_BETA2, "MessagingInfra", MessagingInfra.class);
        KubernetesDeserializer.registerCustomKind(API_VERSION_BETA2, "MessagingInfraList", MessagingInfraList.class);

        KubernetesDeserializer.registerCustomKind(API_VERSION_BETA2, "MessagingTenant", MessagingTenant.class);
        KubernetesDeserializer.registerCustomKind(API_VERSION_BETA2, "MessagingTenantList", MessagingTenantList.class);
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

    public static CustomResourceDefinition messagingTenants() {
        return MESSAGING_TENANT_CRD;
    }
}
