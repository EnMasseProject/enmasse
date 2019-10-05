/*
 * Copyright 2019, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.client;

import io.fabric8.kubernetes.client.Client;
import io.fabric8.kubernetes.client.ExtensionAdapter;
import io.fabric8.openshift.client.OpenShiftConfig;
import io.fabric8.openshift.client.OpenshiftAdapterSupport;
import okhttp3.OkHttpClient;

public class EnmasseOpenShiftExtensionAdapter implements ExtensionAdapter<EnmasseOpenShiftClient> {

    @Override
    public Class<EnmasseOpenShiftClient> getExtensionType() {
        return EnmasseOpenShiftClient.class;
    }

    @Override
    public Boolean isAdaptable(Client client) {
        return new OpenshiftAdapterSupport().isAdaptable(client);
    }

    @Override
    public EnmasseOpenShiftClient adapt(Client client) {
        if (!isAdaptable(client)) {
            throw new EnmasseNotAvailableException("Enmasse CRDs are not available.");
        }
        return new DefaultEnmasseOpenShiftClient(client.adapt(OkHttpClient.class), OpenShiftConfig.wrap(client.getConfiguration()));
    }
}
