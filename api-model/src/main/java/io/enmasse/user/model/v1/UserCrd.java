/*
 * Copyright 2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.user.model.v1;

import io.enmasse.common.model.CustomResources;
import io.fabric8.kubernetes.api.model.apiextensions.CustomResourceDefinition;
import io.fabric8.kubernetes.internal.KubernetesDeserializer;

public class UserCrd {

    private static final CustomResourceDefinition MESSAGING_USER_CRD;

    static {
        MESSAGING_USER_CRD = CustomResources.createCustomResource(User.GROUP, User.VERSION, User.KIND);
    }

    public static void registerCustomCrds() {
        KubernetesDeserializer.registerCustomKind(User.API_VERSION, User.KIND, User.class);
        KubernetesDeserializer.registerCustomKind(UserList.API_VERSION, UserList.KIND, UserList.class);
    }

    public static CustomResourceDefinition messagingUser() {
        return MESSAGING_USER_CRD;
    }

}
