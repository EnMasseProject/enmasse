/*
 * Copyright 2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.config;

import java.util.HashMap;

import io.enmasse.address.model.AddressSpace;
import io.fabric8.kubernetes.api.model.HasMetadata;

public class Apps {

    public static void setPartOf(final HasMetadata metadata, final String partOf) {
        if (metadata.getMetadata().getLabels() == null ) {
            metadata.getMetadata().setLabels(new HashMap<>());
        }
        metadata.getMetadata().getLabels().put(LabelKeys.K8S_PART_OF, partOf);
    }

    public static void setPartOf(final HasMetadata metadata, final AddressSpace addressSpace) {
        var partOf = addressSpace.getAnnotation(AnnotationKeys.INFRA_UUID);
        setPartOf(metadata, partOf);
    }

    public static void setConnectsTo(final HasMetadata metadata, final String...others) {
        if ( metadata.getMetadata().getAnnotations() == null ) {
            metadata.getMetadata().setAnnotations(new HashMap<>());
        }
        metadata.getMetadata().getAnnotations().put(AnnotationKeys.OPENSHIFT_CONNECTS_TO, String.join(",", others));
    }

}
