/*
 * Copyright 2018-2019, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.address.model;

import io.fabric8.kubernetes.client.dsl.base.CustomResourceDefinitionContext;
import io.fabric8.kubernetes.internal.KubernetesDeserializer;

public class CoreCrd {

    public static final String VERSION = "v1beta1";
    public static final String GROUP = "enmasse.io";
    public static final String API_VERSION = GROUP + "/" + VERSION;

    public static void registerCustomCrds() {
        KubernetesDeserializer.registerCustomKind(API_VERSION, Address.KIND, Address.class);
        KubernetesDeserializer.registerCustomKind(API_VERSION, AddressList.KIND, AddressList.class);

        KubernetesDeserializer.registerCustomKind(API_VERSION, AddressSpace.KIND, AddressSpace.class);
        KubernetesDeserializer.registerCustomKind(API_VERSION, AddressSpaceList.KIND, AddressSpaceList.class);

        KubernetesDeserializer.registerCustomKind(API_VERSION, AddressSpaceSchema.KIND, AddressSpaceSchema.class);
        KubernetesDeserializer.registerCustomKind(API_VERSION, AddressSpaceSchemaList.KIND, AddressSpaceSchemaList.class);
    }

    public static CustomResourceDefinitionContext addresses() {
        return CustomResourceDefinitionContext.fromCustomResourceType(Address.class);
    }

    public static CustomResourceDefinitionContext addressSpaces() {
        return CustomResourceDefinitionContext.fromCustomResourceType(AddressSpace.class);
    }

    public static CustomResourceDefinitionContext addressSpaceSchemas() {
        return CustomResourceDefinitionContext.fromCustomResourceType(AddressSpaceSchema.class);
    }
}
