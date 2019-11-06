/*
 * Copyright 2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.common.model;

import io.fabric8.kubernetes.api.model.apiextensions.CustomResourceDefinition;
import io.fabric8.kubernetes.api.model.apiextensions.CustomResourceDefinitionBuilder;

public final class CustomResources {

    private CustomResources() {}

    public static CustomResourceDefinition createCustomResource(final String group, final String version, final String kind) {
        return createCustomResource(group, version, kind, "Namespaced");
    }

    public static CustomResourceDefinition createCustomResource(final String group, final String version, final String kind, final String scope) {
        String singular = kind.toLowerCase();
        String listKind = kind + "List";
        String plural = singular + "s";
        if (singular.endsWith("s")) {
            plural = singular + "es";
        } else if (singular.endsWith("y")) {
            plural = singular.substring(0, singular.length() - 1) + "ies";
        }
        return new CustomResourceDefinitionBuilder()
                        .editOrNewMetadata()
                        .withName(plural + "." + group)
                        .addToLabels("app", "enmasse")
                        .endMetadata()
                        .editOrNewSpec()
                        .withGroup(group)
                        .withVersion(version)
                        .withScope(scope)
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
