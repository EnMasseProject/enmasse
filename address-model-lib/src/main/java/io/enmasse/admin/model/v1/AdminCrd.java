/*
 * Copyright 2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.admin.model.v1;

import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.apiextensions.CustomResourceDefinition;
import io.fabric8.kubernetes.api.model.apiextensions.CustomResourceDefinitionBuilder;
import io.fabric8.kubernetes.internal.KubernetesDeserializer;

public class AdminCrd {

    public static void registerCustomCrds() {
        registerCrd(AddressSpacePlan.class);
        registerCrd(AddressPlan.class);
        registerCrd(StandardInfraConfig.class);
        registerCrd(BrokeredInfraConfig.class);
    }

    public static void registerCrd(Class<? extends HasMetadata> clazz) {
        KubernetesDeserializer.registerCustomKind("admin.enmasse.io/v1alpha1", kind(clazz), clazz);
    }

    public static <T extends HasMetadata> String kind(Class<T> cls) {
        try {
            return cls.newInstance().getKind();
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
    }

    public static CustomResourceDefinition addressspaceplans() {
        return createCrd("AddressSpacePlan");
    }

    public static CustomResourceDefinition addressplans() {
        return createCrd("AddressPlan");
    }

    public static CustomResourceDefinition brokeredinfraconfigs() {
        return createCrd("BrokeredInfraConfig");
    }

    public static CustomResourceDefinition standardinfraconfigs() {
        return createCrd("StandardInfraConfig");
    }

    private static CustomResourceDefinition createCrd(String kind) {
        String singular = kind.toLowerCase();
        String listKind = kind + "List";
        String plural = singular + "s";
        return new CustomResourceDefinitionBuilder()
                .editOrNewMetadata()
                .withName(plural + ".admin.enmassse.io")
                .addToLabels("app", "enmasse")
                .endMetadata()
                .editOrNewSpec()
                .withGroup("admin.enmasse.io")
                .withVersion("v1alpha1")
                .withScope("Namespaced")
                .editOrNewNames()
                .withKind(kind)
                .withListKind(listKind)
                .withPlural(plural)
                .withSingular(singular)
                .endNames()
                .endSpec()
                .build();
    }
}
