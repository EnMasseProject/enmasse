/*
 * Copyright 2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.admin.model.v1;

import io.fabric8.kubernetes.client.dsl.base.CustomResourceDefinitionContext;
import io.fabric8.kubernetes.internal.KubernetesDeserializer;

public class AdminCrd {

    public static final String VERSION_V1ALPHA1 = "v1alpha1";
    public static final String VERSION_V1BETA1 = "v1beta1";
    public static final String VERSION_V1BETA2 = "v1beta2";
    public static final String GROUP = "admin.enmasse.io";
    public static final String API_VERSION_V1ALPHA1 = GROUP + "/" + VERSION_V1ALPHA1;
    public static final String API_VERSION_V1BETA1 = GROUP + "/" + VERSION_V1BETA1;
    public static final String API_VERSION_V1BETA2 = GROUP + "/" + VERSION_V1BETA2;

    public static void registerCustomCrds() {

        KubernetesDeserializer.registerCustomKind(API_VERSION_V1BETA2, AddressPlan.KIND, AddressPlan.class);
        KubernetesDeserializer.registerCustomKind(API_VERSION_V1BETA2, AddressPlanList.KIND, AddressPlanList.class);

        KubernetesDeserializer.registerCustomKind(API_VERSION_V1BETA2, AddressSpacePlan.KIND, AddressSpacePlan.class);
        KubernetesDeserializer.registerCustomKind(API_VERSION_V1BETA2, AddressSpacePlanList.KIND, AddressSpacePlanList.class);

        KubernetesDeserializer.registerCustomKind(API_VERSION_V1BETA1, BrokeredInfraConfig.KIND, BrokeredInfraConfig.class);
        KubernetesDeserializer.registerCustomKind(API_VERSION_V1BETA1, BrokeredInfraConfigList.KIND, BrokeredInfraConfigList.class);

        KubernetesDeserializer.registerCustomKind(API_VERSION_V1BETA1, StandardInfraConfig.KIND, StandardInfraConfig.class);
        KubernetesDeserializer.registerCustomKind(API_VERSION_V1BETA1, StandardInfraConfigList.KIND, StandardInfraConfigList.class);

        KubernetesDeserializer.registerCustomKind(API_VERSION_V1BETA1, AuthenticationService.KIND, AuthenticationService.class);
        KubernetesDeserializer.registerCustomKind(API_VERSION_V1BETA1, AuthenticationServiceList.KIND, AuthenticationServiceList.class);

        KubernetesDeserializer.registerCustomKind(API_VERSION_V1BETA1, ConsoleService.KIND, ConsoleService.class);
        KubernetesDeserializer.registerCustomKind(API_VERSION_V1BETA1, ConsoleServiceList.KIND, ConsoleServiceList.class);
    }

    public static CustomResourceDefinitionContext addressPlans() {
        return CustomResourceDefinitionContext.fromCustomResourceType(AddressPlan.class);
    }

    public static CustomResourceDefinitionContext addressSpacePlans() {
        return CustomResourceDefinitionContext.fromCustomResourceType(AddressSpacePlan.class);
    }

    public static CustomResourceDefinitionContext brokeredInfraConfigs() {
        return CustomResourceDefinitionContext.fromCustomResourceType(BrokeredInfraConfig.class);
    }

    public static CustomResourceDefinitionContext standardInfraConfigs() {
        return CustomResourceDefinitionContext.fromCustomResourceType(StandardInfraConfig.class);
    }

    public static CustomResourceDefinitionContext authenticationServices() {
        return CustomResourceDefinitionContext.fromCustomResourceType(AuthenticationService.class);
    }

    public static CustomResourceDefinitionContext consoleServices() {
        return CustomResourceDefinitionContext.fromCustomResourceType(ConsoleService.class);
    }

}
