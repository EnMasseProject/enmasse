/*
 * Copyright 2019, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.client;

import io.fabric8.kubernetes.client.Config;
import io.fabric8.kubernetes.client.DefaultKubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClientException;
import okhttp3.OkHttpClient;

public class DefaultEnmasseKubernetesClient
        extends DefaultKubernetesClient
        implements EnmasseKubernetesClient {

    public DefaultEnmasseKubernetesClient() throws KubernetesClientException {
    }

    public DefaultEnmasseKubernetesClient(String masterUrl) throws KubernetesClientException {
        super(masterUrl);
    }

    public DefaultEnmasseKubernetesClient(Config config) throws KubernetesClientException {
        super(config);
    }

    public DefaultEnmasseKubernetesClient(OkHttpClient httpClient, Config config) throws KubernetesClientException {
        super(httpClient, config);
    }

    @Override
    public EnmasseKubernetesAPIGroupDSL enmasse() {
        return adapt(EnmasseAPIGroupClient.class);
    }
}
