/*
 * Copyright 2019, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.client;

import io.fabric8.kubernetes.client.APIGroupExtensionAdapter;
import io.fabric8.kubernetes.client.Client;
import io.fabric8.kubernetes.client.KubernetesClient;

public class EnmasseKubernetesAPIGroupExtensionAdapter extends APIGroupExtensionAdapter<EnmasseAPIGroupClient> {

    @Override
    protected String getAPIGroupName() {
        return "enmasse";
    }

    @Override
    public Class<EnmasseAPIGroupClient> getExtensionType() {
        return EnmasseAPIGroupClient.class;
    }

    @Override
    protected EnmasseAPIGroupClient newInstance(Client client) {
        return new EnmasseAPIGroupClient((KubernetesClient) client, client.getConfiguration());
    }

}
