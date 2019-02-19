/*
 * Copyright 2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.iot.model.v1;

import io.enmasse.common.model.CustomResources;
import io.fabric8.kubernetes.api.model.apiextensions.CustomResourceDefinition;
import io.fabric8.kubernetes.internal.KubernetesDeserializer;

public class IoTCrd {

    private static final CustomResourceDefinition CRD;

    static {
        CRD = CustomResources.createCustomResource(Version.GROUP, Version.VERSION, IoTProject.KIND);
    }

    public static void registerCustomCrds() {
        KubernetesDeserializer.registerCustomKind(Version.API_VERSION, IoTProject.KIND, IoTProject.class);
        KubernetesDeserializer.registerCustomKind(Version.API_VERSION, IoTProjectList.KIND, IoTProjectList.class);
    }

    public static CustomResourceDefinition project() {
        return CRD;
    }

}
