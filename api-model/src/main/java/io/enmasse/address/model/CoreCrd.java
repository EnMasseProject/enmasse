/*
 * Copyright 2018-2019, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.address.model;

import io.enmasse.common.model.CustomResources;
import io.fabric8.kubernetes.api.model.apiextensions.CustomResourceDefinition;
import io.fabric8.kubernetes.internal.KubernetesDeserializer;

public class CoreCrd {

    public static final String VERSION = "v1beta1";
    public static final String GROUP = "enmasse.io";
    public static final String API_VERSION = GROUP + "/" + VERSION;

    private static final CustomResourceDefinition ADDRESS_CRD;
    private static final CustomResourceDefinition ADDRESS_SPACE_CRD;
    private static final CustomResourceDefinition ADDRESS_SPACE_SCHEMA_CRD;

    static {
        ADDRESS_CRD = CustomResources.createCustomResource(GROUP, VERSION, Address.KIND);
        ADDRESS_SPACE_CRD = CustomResources.createCustomResource(GROUP, VERSION, AddressSpace.KIND);
        ADDRESS_SPACE_SCHEMA_CRD = CustomResources.createCustomResource(GROUP, VERSION, AddressSpaceSchema.KIND, "Cluster");
    }

    public static void registerCustomCrds() {
        KubernetesDeserializer.registerCustomKind(API_VERSION, Address.KIND, Address.class);
        KubernetesDeserializer.registerCustomKind(API_VERSION, AddressList.KIND, AddressList.class);

        KubernetesDeserializer.registerCustomKind(API_VERSION, AddressSpace.KIND, AddressSpace.class);
        KubernetesDeserializer.registerCustomKind(API_VERSION, AddressSpaceList.KIND, AddressSpaceList.class);

        KubernetesDeserializer.registerCustomKind(API_VERSION, AddressSpaceSchema.KIND, AddressSpaceSchema.class);
        KubernetesDeserializer.registerCustomKind(API_VERSION, AddressSpaceSchemaList.KIND, AddressSpaceSchemaList.class);
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
}
