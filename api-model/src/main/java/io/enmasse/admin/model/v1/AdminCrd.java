/*
 * Copyright 2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.admin.model.v1;

import io.enmasse.common.model.CustomResources;
import io.fabric8.kubernetes.api.model.apiextensions.CustomResourceDefinition;
import io.fabric8.kubernetes.internal.KubernetesDeserializer;

public class AdminCrd {

    public static final String VERSION = "v1beta1";
    public static final String GROUP = "admin.enmasse.io";
    public static final String API_VERSION = GROUP + "/" + VERSION;

    private static final CustomResourceDefinition ADDRESS_PLAN_CRD;
    private static final CustomResourceDefinition ADDRESS_SPACE_PLAN_CRD;
    private static final CustomResourceDefinition BROKERED_INFRA_CONFIG_CRD;
    private static final CustomResourceDefinition STANDARD_INFRA_CONFIG_CRD;

    static {
        ADDRESS_PLAN_CRD = CustomResources.createCustomResource(GROUP, VERSION, AddressPlan.KIND);
        ADDRESS_SPACE_PLAN_CRD = CustomResources.createCustomResource(GROUP, VERSION, AddressSpacePlan.KIND);
        BROKERED_INFRA_CONFIG_CRD = CustomResources.createCustomResource(GROUP, VERSION, BrokeredInfraConfig.KIND);
        STANDARD_INFRA_CONFIG_CRD = CustomResources.createCustomResource(GROUP, VERSION, StandardInfraConfig.KIND);
    }

    public static void registerCustomCrds() {

        KubernetesDeserializer.registerCustomKind(API_VERSION, AddressPlan.KIND, AddressPlan.class);
        KubernetesDeserializer.registerCustomKind(API_VERSION, AddressPlanList.KIND, AddressPlanList.class);

        KubernetesDeserializer.registerCustomKind(API_VERSION, AddressSpacePlan.KIND, AddressSpacePlan.class);
        KubernetesDeserializer.registerCustomKind(API_VERSION, AddressSpacePlanList.KIND, AddressSpacePlanList.class);

        KubernetesDeserializer.registerCustomKind(API_VERSION, BrokeredInfraConfig.KIND, BrokeredInfraConfig.class);
        KubernetesDeserializer.registerCustomKind(API_VERSION, BrokeredInfraConfigList.KIND, BrokeredInfraConfigList.class);

        KubernetesDeserializer.registerCustomKind(API_VERSION, StandardInfraConfig.KIND, StandardInfraConfig.class);
        KubernetesDeserializer.registerCustomKind(API_VERSION, StandardInfraConfigList.KIND, StandardInfraConfigList.class);

    }

    public static CustomResourceDefinition addressPlans() {
        return ADDRESS_PLAN_CRD;
    }

    public static CustomResourceDefinition addressSpacePlans() {
        return ADDRESS_SPACE_PLAN_CRD;
    }

    public static CustomResourceDefinition brokeredInfraConfigs() {
        return BROKERED_INFRA_CONFIG_CRD;
    }

    public static CustomResourceDefinition standardInfraConfigs() {
        return STANDARD_INFRA_CONFIG_CRD;
    }

}
