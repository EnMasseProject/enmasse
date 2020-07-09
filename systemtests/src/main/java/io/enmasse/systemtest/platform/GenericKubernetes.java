/*
 * Copyright 2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.systemtest.platform;

import io.enmasse.systemtest.Endpoint;
import io.enmasse.systemtest.Environment;
import io.fabric8.kubernetes.api.model.Service;
import io.fabric8.kubernetes.api.model.ServicePort;
import io.fabric8.kubernetes.client.Config;
import io.fabric8.kubernetes.client.ConfigBuilder;
import io.fabric8.kubernetes.client.DefaultKubernetesClient;
import io.fabric8.kubernetes.client.utils.HttpClientUtils;
import okhttp3.OkHttpClient;
import okhttp3.Protocol;

import java.util.Collections;

public class GenericKubernetes extends Kubernetes {

    private static final String OLM_NAMESPACE = "operators";

    protected GenericKubernetes() {
        super(() -> {
            Config config = new ConfigBuilder().build();
            OkHttpClient httpClient = HttpClientUtils.createHttpClient(config);
            // Workaround https://github.com/square/okhttp/issues/3146
            httpClient = httpClient.newBuilder()
                    .protocols(Collections.singletonList(Protocol.HTTP_1_1))
                    .connectTimeout(Environment.getInstance().getKubernetesApiConnectTimeout())
                    .writeTimeout(Environment.getInstance().getKubernetesApiWriteTimeout())
                    .readTimeout(Environment.getInstance().getKubernetesApiReadTimeout())
                    .build();
            return new DefaultKubernetesClient(httpClient, config);
        });
    }

    @Override
    public Endpoint getMasterEndpoint() {
        return new Endpoint(client.getMasterUrl());
    }

    @Override
    public Endpoint getRestEndpoint() {
        return new Endpoint(client.getMasterUrl());
    }

    @Override
    public Endpoint getKeycloakEndpoint() {
        return getExternalEndpoint("standard-authservice");
    }

    @Override
    public Endpoint getExternalEndpoint(String name) {
        return getExternalEndpoint(name, infraNamespace);
    }

    @Override
    public Endpoint getExternalEndpoint(String name, String namespace) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void createExternalEndpoint(String name, String namespace, Service service, ServicePort targetPort) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void deleteExternalEndpoint(String namespace, String name) {
        throw new UnsupportedOperationException();
    }

    @Override
    public String getOlmNamespace() {
        return OLM_NAMESPACE;
    }

    @Override
    public String getClusterExternalImageRegistry() {
        return null;
    }

    @Override
    public String getClusterInternalImageRegistry() {
        return null;
    }
}
