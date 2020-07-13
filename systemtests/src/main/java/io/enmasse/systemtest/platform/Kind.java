/*
 * Copyright 2018-2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.systemtest.platform;

import io.enmasse.systemtest.Endpoint;
import io.enmasse.systemtest.Environment;
import io.enmasse.systemtest.framework.LoggerUtils;
import io.fabric8.kubernetes.api.model.NodeAddress;
import io.fabric8.kubernetes.api.model.Service;
import io.fabric8.kubernetes.api.model.ServiceBuilder;
import io.fabric8.kubernetes.api.model.ServicePort;
import io.fabric8.kubernetes.client.Config;
import io.fabric8.kubernetes.client.ConfigBuilder;
import io.fabric8.kubernetes.client.DefaultKubernetesClient;
import io.fabric8.kubernetes.client.utils.HttpClientUtils;
import okhttp3.OkHttpClient;
import okhttp3.Protocol;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;


public class Kind extends Kubernetes{

    private static final String OLM_NAMESPACE = "operators";

    protected Kind() {
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
    public void createExternalEndpoint(String name, String namespace, Service service, ServicePort targetPort) {
        name = getName(name);
        ServiceBuilder builder = new ServiceBuilder()
                .editOrNewMetadata()
                .withName(name)
                .withNamespace(namespace)
                .endMetadata()
                .editOrNewSpec()
                .withType("LoadBalancer")
                .addToPorts(targetPort)
                .withSelector(service.getSpec().getSelector())
                .endSpec();

        client.services().inNamespace(namespace).withName(name).createOrReplace(builder.build());
    }

    @Override
    public void deleteExternalEndpoint(String namespace, String name) {
        name = getName(name);
        client.services().inNamespace(name).withName(name).cascading(true).delete();
    }

    @Override
    public String getOlmNamespace() {
        return OLM_NAMESPACE;
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
        return null;
    }

    @Override
    public Endpoint getExternalEndpoint(String name) {
        return getExternalEndpoint(name, infraNamespace);
    }

    @Override
    public Endpoint getExternalEndpoint(String name, String namespace) {
        String externalName = getName(name);

        Service service = Kubernetes.getInstance().client.services().inNamespace(namespace).list()
                .getItems().stream().filter(p -> p.getMetadata().getName().equals(externalName)).findAny().orElseThrow();

        Endpoint endpoint = new Endpoint(this.getHost(), service.getSpec().getPorts().get(0).getNodePort());
        return endpoint;
    }

    @Override
    public String getClusterExternalImageRegistry() {
        return null;
    }

    @Override
    public String getClusterInternalImageRegistry() {
        return null;
    }

    @Override
    public String getHost() {
        List<NodeAddress> addresses = client.nodes().list().getItems().stream()
                .peek(n -> LoggerUtils.getLogger().info("Found node: {}", n.getMetadata().getName()))
                .flatMap(n -> n.getStatus().getAddresses().stream()
                        .peek(a -> LoggerUtils.getLogger().info("Found address: {}", a))
                        .filter(a -> a.getType().equals("InternalIP")))
                .collect(Collectors.toList());
        if (addresses.isEmpty()) {
            return null;
        }
        return addresses.get(0).getAddress();
    }

    private String getName(String name) {
        String externalName = name;
        if (!name.endsWith("-external")) {
            externalName += "-external";
        }
        return externalName;
    }
}
