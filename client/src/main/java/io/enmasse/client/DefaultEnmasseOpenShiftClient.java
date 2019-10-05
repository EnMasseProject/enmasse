/*
 * Copyright 2019, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.client;

import io.fabric8.kubernetes.client.Config;
import io.fabric8.kubernetes.client.KubernetesClientException;
import io.fabric8.openshift.client.DefaultOpenShiftClient;
import io.fabric8.openshift.client.OpenShiftConfig;
import okhttp3.OkHttpClient;

public class DefaultEnmasseOpenShiftClient
        extends DefaultOpenShiftClient
        implements EnmasseOpenShiftClient {

    public DefaultEnmasseOpenShiftClient() throws KubernetesClientException {
    }

    public DefaultEnmasseOpenShiftClient(String masterUrl) throws KubernetesClientException {
        super(masterUrl);
    }

    public DefaultEnmasseOpenShiftClient(Config config) throws KubernetesClientException {
        super(config);
    }

    public DefaultEnmasseOpenShiftClient(OkHttpClient httpClient, Config config) throws KubernetesClientException {
        super(httpClient, OpenShiftConfig.wrap(config));
    }

    @Override
    public EnmasseOpenShiftAPIGroupDSL enmasse() {
        return adapt(EnmasseOpenShiftAPIGroupDSL.class);
    }

}
