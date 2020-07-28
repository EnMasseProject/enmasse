/*
 * Copyright 2018-2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package io.enmasse.iot.model.v1;

import io.enmasse.common.model.CustomResources;
import io.fabric8.kubernetes.api.model.apiextensions.CustomResourceDefinition;
import io.fabric8.kubernetes.internal.KubernetesDeserializer;

public class IoTCrd {

    public static final String VERSION = "v1";
    public static final String GROUP = "iot.enmasse.io";
    public static final String API_VERSION = GROUP + "/" + VERSION;

    private static final CustomResourceDefinition TENANT_CRD;
    private static final CustomResourceDefinition CONFIG_CRD;

    static {
        TENANT_CRD = CustomResources.createCustomResource(GROUP, VERSION, IoTTenant.KIND);
        CONFIG_CRD = CustomResources.createCustomResource(GROUP, VERSION, IoTConfig.KIND);
    }

    public static void registerCustomCrds() {
        KubernetesDeserializer.registerCustomKind(API_VERSION, IoTTenant.KIND, IoTTenant.class);
        KubernetesDeserializer.registerCustomKind(API_VERSION, IoTTenantList.KIND, IoTTenantList.class);
        KubernetesDeserializer.registerCustomKind(API_VERSION, IoTConfig.KIND, IoTConfig.class);
    }

    public static CustomResourceDefinition tenant() {
        return TENANT_CRD;
    }

    public static CustomResourceDefinition config() {
        return CONFIG_CRD;
    }

}
