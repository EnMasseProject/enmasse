/*
 * Copyright 2016-2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.k8s.util;

import java.io.File;
import java.util.Map;

import io.fabric8.kubernetes.api.model.KubernetesList;
import io.fabric8.openshift.client.OpenShiftClient;

public final class Templates {
    private Templates() {
    }

    /**
     * Process a template.
     * @param client The client to use
     * @param templateFile The file to process.
     * @param parameters The parameters to apply, may be {@code null}.
     * @return The list of processed resources.
     */
    public static KubernetesList process(final OpenShiftClient client, final File templateFile, final Map<String,String> parameters) {
        return client.templates().load(templateFile).process(parameters);
    }
}
