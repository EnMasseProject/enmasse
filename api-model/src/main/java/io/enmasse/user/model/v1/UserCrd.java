/*
 * Copyright 2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.user.model.v1;

import io.fabric8.kubernetes.client.dsl.base.CustomResourceDefinitionContext;
import io.fabric8.kubernetes.internal.KubernetesDeserializer;

public class UserCrd {

    public static final String VERSION = "v1beta1";
    public static final String GROUP = "user.enmasse.io";
    public static final String API_VERSION = GROUP +"/" + VERSION;

    public static void registerCustomCrds() {
        KubernetesDeserializer.registerCustomKind(API_VERSION, User.KIND, User.class);
        KubernetesDeserializer.registerCustomKind(API_VERSION, UserList.KIND, UserList.class);
    }

    public static CustomResourceDefinitionContext messagingUser() {
        return CustomResourceDefinitionContext.fromCustomResourceType(User.class);
    }

}
