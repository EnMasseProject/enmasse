/*
 * Copyright 2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.admin.model.v1;

import io.enmasse.common.model.CustomResources;
import io.fabric8.kubernetes.api.model.apiextensions.CustomResourceDefinition;
import io.fabric8.kubernetes.internal.KubernetesDeserializer;

public class AdminCrd {

    public static final String VERSION_V1ALPHA1 = "v1alpha1";
    public static final String VERSION_V1BETA1 = "v1beta1";
    public static final String VERSION_V1BETA2 = "v1beta2";
    public static final String GROUP = "admin.enmasse.io";
    public static final String API_VERSION_V1ALPHA1 = GROUP + "/" + VERSION_V1ALPHA1;
    public static final String API_VERSION_V1BETA1 = GROUP + "/" + VERSION_V1BETA1;
    public static final String API_VERSION_V1BETA2 = GROUP + "/" + VERSION_V1BETA2;

    private static final CustomResourceDefinition ADDRESS_PLAN_CRD;
    private static final CustomResourceDefinition ADDRESS_SPACE_PLAN_CRD;
    private static final CustomResourceDefinition BROKERED_INFRA_CONFIG_CRD;
    private static final CustomResourceDefinition STANDARD_INFRA_CONFIG_CRD;
    private static final CustomResourceDefinition AUTHENTICATION_SERVICE_CRD;
    private static final CustomResourceDefinition KAFKA_INFRA_CONFIG_CRD;

    static {
        ADDRESS_PLAN_CRD = CustomResources.createCustomResource(GROUP, VERSION_V1BETA2, AddressPlan.KIND);
        ADDRESS_SPACE_PLAN_CRD = CustomResources.createCustomResource(GROUP, VERSION_V1BETA2, AddressSpacePlan.KIND);
        BROKERED_INFRA_CONFIG_CRD = CustomResources.createCustomResource(GROUP, VERSION_V1BETA1, BrokeredInfraConfig.KIND);
        STANDARD_INFRA_CONFIG_CRD = CustomResources.createCustomResource(GROUP, VERSION_V1BETA1, StandardInfraConfig.KIND);
        AUTHENTICATION_SERVICE_CRD = CustomResources.createCustomResource(GROUP, VERSION_V1BETA1, AuthenticationService.KIND);
        KAFKA_INFRA_CONFIG_CRD = CustomResources.createCustomResource(GROUP, VERSION_V1BETA1, KafkaInfraConfig.KIND);
    }

    public static void registerCustomCrds() {

        KubernetesDeserializer.registerCustomKind(API_VERSION_V1BETA2, AddressPlan.KIND, AddressPlan.class);
        KubernetesDeserializer.registerCustomKind(API_VERSION_V1BETA2, AddressPlanList.KIND, AddressPlanList.class);

        KubernetesDeserializer.registerCustomKind(API_VERSION_V1BETA2, AddressSpacePlan.KIND, AddressSpacePlan.class);
        KubernetesDeserializer.registerCustomKind(API_VERSION_V1BETA2, AddressSpacePlanList.KIND, AddressSpacePlanList.class);

        KubernetesDeserializer.registerCustomKind(API_VERSION_V1BETA1, BrokeredInfraConfig.KIND, BrokeredInfraConfig.class);
        KubernetesDeserializer.registerCustomKind(API_VERSION_V1BETA1, BrokeredInfraConfigList.KIND, BrokeredInfraConfigList.class);

        KubernetesDeserializer.registerCustomKind(API_VERSION_V1BETA1, StandardInfraConfig.KIND, StandardInfraConfig.class);
        KubernetesDeserializer.registerCustomKind(API_VERSION_V1BETA1, StandardInfraConfigList.KIND, StandardInfraConfigList.class);

        KubernetesDeserializer.registerCustomKind(API_VERSION_V1BETA1, KafkaInfraConfig.KIND, KafkaInfraConfig.class);
        KubernetesDeserializer.registerCustomKind(API_VERSION_V1BETA1, KafkaInfraConfigList.KIND, KafkaInfraConfigList.class);

        KubernetesDeserializer.registerCustomKind(API_VERSION_V1BETA1, AuthenticationService.KIND, AuthenticationService.class);
        KubernetesDeserializer.registerCustomKind(API_VERSION_V1BETA1, AuthenticationServiceList.KIND, AuthenticationServiceList.class);

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

    public static CustomResourceDefinition authenticationServices() {
        return AUTHENTICATION_SERVICE_CRD;
    }

    public static CustomResourceDefinition kafkaInfraConfigs() {
        return KAFKA_INFRA_CONFIG_CRD;
    }
}
