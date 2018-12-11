/*
 * Copyright 2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.common.api.model;

import static io.fabric8.kubernetes.internal.KubernetesDeserializer.registerCustomKind;

import java.util.Optional;

import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.apiextensions.CustomResourceDefinition;
import io.fabric8.kubernetes.api.model.apiextensions.CustomResourceDefinitionBuilder;

public final class CustomResources {
    private CustomResources() {
    }

    public static String getKind(Class<?> clazz) {
        final CustomResource custom = clazz.getAnnotation(CustomResource.class);
        if (custom != null && custom.kind() != null && !custom.kind().isEmpty()) {
            return custom.kind();
        } else {
            return clazz.getSimpleName();
        }
    }

    public static CustomResourceDefinition createFromClass(final Class<?> clazz) {

        final String kind = getKind(clazz);
        final CustomResource customResource = clazz.getAnnotation(CustomResource.class);
        final String apiVersion = clazz.getAnnotation(ApiVersion.class).value();

        // get singular, default to variation of "kind"

        String singular = Optional.ofNullable(clazz.getAnnotation(CustomResource.Singular.class))
                .map(CustomResource.Singular::value)
                .orElse(kind.toLowerCase());

        // get plural, default to none

        String plural = Optional.ofNullable(clazz.getAnnotation(CustomResource.Plural.class))
                .map(CustomResource.Plural::value)
                .orElse(null);

        // if no explicit plural is specified, and the singular is not set to "none"

        if (plural == null && !singular.isEmpty()) {
            // then derive the plural from the singular
            plural = singular + "s";
        }

        // if the plural is set to "none"

        if (plural != null && plural.isEmpty()) {
            // then set it to null
            plural = null;
        }

        // if the plural is still null

        if (plural == null) {
            // set it to a variation of "kind"
            plural = kind.toLowerCase() + "s";
        }

        // if the singular is set to "none"

        if (singular != null && singular.isEmpty()) {
            // set it to null
            singular = null;
        }

        return new CustomResourceDefinitionBuilder()
                .withApiVersion("apiextensions.k8s.io/v1beta1")

                .withNewMetadata()
                .withName(plural + "." + customResource.group())
                .endMetadata()

                .withNewSpec()
                .withGroup(customResource.group())
                .withVersion(apiVersion)
                .withScope(customResource.scope().name())

                .withNewNames()
                .withKind(kind)
                .withShortNames(customResource.shortNames())
                .withPlural(plural)
                .withSingular(singular)
                .endNames()

                .endSpec()

                .build();
    }

    public static CustomResourceDefinition fromClass(final Class<? extends HasMetadata> clazz) {

        final CustomResourceDefinition result = createFromClass(clazz);

        registerCustomKind(
                result.getSpec().getGroup() + "/" + result.getSpec().getVersion(),
                result.getSpec().getNames().getKind(),
                clazz);

        return result;
    }
}
