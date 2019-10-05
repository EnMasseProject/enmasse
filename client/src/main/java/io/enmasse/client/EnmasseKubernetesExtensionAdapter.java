/*
 * Copyright 2019, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.client;

import io.fabric8.kubernetes.client.Client;
import io.fabric8.kubernetes.client.ExtensionAdapter;
import io.fabric8.openshift.client.OpenShiftConfig;
import okhttp3.OkHttpClient;

public class EnmasseKubernetesExtensionAdapter implements ExtensionAdapter<EnmasseKubernetesClient> {

    @Override
    public Class<EnmasseKubernetesClient> getExtensionType() {
        return EnmasseKubernetesClient.class;
    }

    @Override
    public Boolean isAdaptable(Client client) {
        // TODO check that the Enmasse CRDs are installed
        return true;
    }

    @Override
    public EnmasseKubernetesClient adapt(Client client) {
        if (!isAdaptable(client)) {
            throw new EnmasseNotAvailableException("Enmasse CRDs are not available.");
        }
        return new DefaultEnmasseKubernetesClient(client.adapt(OkHttpClient.class), OpenShiftConfig.wrap(client.getConfiguration()));
    }
}
