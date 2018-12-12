/*
 * Copyright 2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.admin.model.v1;

import io.enmasse.common.model.CustomResources;
import io.fabric8.kubernetes.api.model.apiextensions.CustomResourceDefinition;
import io.fabric8.kubernetes.internal.KubernetesDeserializer;

public class AdminCrd {

    private static final CustomResourceDefinition ADDRESS_PLAN_CRD;
    private static final CustomResourceDefinition ADDRESS_SPACE_PLAN_CRD;
    private static final CustomResourceDefinition BROKERED_INFRA_CONFIG_CRD;
    private static final CustomResourceDefinition STANDARD_INFRA_CONFIG_CRD;

    static {
        ADDRESS_PLAN_CRD = CustomResources.createCustomResource(AddressPlan.GROUP, AddressPlan.VERSION, AddressPlan.KIND);
        ADDRESS_SPACE_PLAN_CRD = CustomResources.createCustomResource(AddressSpacePlan.GROUP, AddressSpacePlan.VERSION, AddressSpacePlan.KIND);
        BROKERED_INFRA_CONFIG_CRD = CustomResources.createCustomResource(BrokeredInfraConfig.GROUP, BrokeredInfraConfig.VERSION, BrokeredInfraConfig.KIND);
        STANDARD_INFRA_CONFIG_CRD = CustomResources.createCustomResource(StandardInfraConfig.GROUP, StandardInfraConfig.VERSION, StandardInfraConfig.KIND);
    }

    public static void registerCustomCrds() {

        KubernetesDeserializer.registerCustomKind(AddressPlan.API_VERSION, AddressPlan.KIND, AddressPlan.class);
        KubernetesDeserializer.registerCustomKind(AddressPlanList.API_VERSION, AddressPlanList.KIND, AddressPlanList.class);

        KubernetesDeserializer.registerCustomKind(AddressSpacePlan.API_VERSION, AddressSpacePlan.KIND, AddressSpacePlan.class);
        KubernetesDeserializer.registerCustomKind(AddressSpacePlanList.API_VERSION, AddressSpacePlanList.KIND, AddressSpacePlanList.class);

        KubernetesDeserializer.registerCustomKind(BrokeredInfraConfig.API_VERSION, BrokeredInfraConfig.KIND, BrokeredInfraConfig.class);
        KubernetesDeserializer.registerCustomKind(BrokeredInfraConfigList.API_VERSION, BrokeredInfraConfigList.KIND, BrokeredInfraConfigList.class);

        KubernetesDeserializer.registerCustomKind(StandardInfraConfig.API_VERSION, StandardInfraConfig.KIND, StandardInfraConfig.class);
        KubernetesDeserializer.registerCustomKind(StandardInfraConfigList.API_VERSION, StandardInfraConfigList.KIND, StandardInfraConfigList.class);

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
