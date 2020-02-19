/*
 * Copyright 2019, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.k8s.api;

import java.time.Clock;
import java.time.Duration;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.enmasse.address.model.AddressSpace;
import io.enmasse.address.model.AddressSpaceBuilder;
import io.enmasse.address.model.AddressSpaceList;
import io.enmasse.address.model.CoreCrd;
import io.enmasse.address.model.DoneableAddressSpace;
import io.enmasse.k8s.api.cache.CacheWatcher;
import io.enmasse.k8s.api.cache.Controller;
import io.enmasse.k8s.api.cache.EventCache;
import io.enmasse.k8s.api.cache.HasMetadataFieldExtractor;
import io.enmasse.k8s.api.cache.ListOptions;
import io.enmasse.k8s.api.cache.ListerWatcher;
import io.enmasse.k8s.api.cache.Reflector;
import io.enmasse.k8s.api.cache.WorkQueue;
import io.fabric8.kubernetes.api.model.Status;
import io.fabric8.kubernetes.api.model.apiextensions.CustomResourceDefinition;
import io.fabric8.kubernetes.client.KubernetesClientException;
import io.fabric8.kubernetes.client.NamespacedKubernetesClient;
import io.fabric8.kubernetes.client.RequestConfig;
import io.fabric8.kubernetes.client.RequestConfigBuilder;
import io.fabric8.kubernetes.client.dsl.MixedOperation;
import io.fabric8.kubernetes.client.dsl.Resource;
import io.fabric8.kubernetes.client.utils.URLUtils;
import okhttp3.*;

public class KubeAddressSpaceApi implements AddressSpaceApi, ListerWatcher<AddressSpace, AddressSpaceList> {
    private static final ObjectMapper mapper = new ObjectMapper();
    private final NamespacedKubernetesClient kubernetesClient;
    private final MixedOperation<AddressSpace, AddressSpaceList, DoneableAddressSpace, Resource<AddressSpace, DoneableAddressSpace>> client;
    private final String namespace;
    private final CustomResourceDefinition customResourceDefinition;
    private final WorkQueue<AddressSpace> cache = new EventCache<>(new HasMetadataFieldExtractor<>());
    private final String version;

    private KubeAddressSpaceApi(NamespacedKubernetesClient kubeClient, String namespace, CustomResourceDefinition customResourceDefinition, String version) {
        this.kubernetesClient = kubeClient;
        this.namespace = namespace;
        this.version = version;
        this.client = kubeClient.customResources(customResourceDefinition, AddressSpace.class, AddressSpaceList.class, DoneableAddressSpace.class);
        this.customResourceDefinition = customResourceDefinition;
    }

    public static AddressSpaceApi create(NamespacedKubernetesClient kubernetesClient, String namespace, String version) {
        return new KubeAddressSpaceApi(kubernetesClient, namespace, CoreCrd.addressSpaces(), version);
    }

    @Override
    public Optional<AddressSpace> getAddressSpaceWithName(String namespace, String id) {
        return Optional.ofNullable(client.inNamespace(namespace).withName(id).get());
    }

    @Override
    public void createAddressSpace(AddressSpace addressSpace) throws Exception {
        client.inNamespace(addressSpace.getMetadata().getNamespace()).create(addressSpace);
    }

    @Override
    public boolean replaceAddressSpace(AddressSpace addressSpace) throws Exception {
        boolean exists = client.inNamespace(addressSpace.getMetadata().getNamespace()).withName(addressSpace.getMetadata().getName()).get() != null;
        if (!exists) {
            return false;
        }

        // Make a copy, so that no one else makes modifications to our instance.
        // This is important as we do put this instance into your cache.
        addressSpace = new AddressSpaceBuilder(addressSpace).build();

        try {
            AddressSpace result = client
                    .inNamespace(addressSpace.getMetadata().getNamespace())
                    .withName(addressSpace.getMetadata().getName())
                    .lockResourceVersion(addressSpace.getMetadata().getResourceVersion())
                    .replace(addressSpace);
            cache.replace(addressSpace);
            return result != null;
        } catch (KubernetesClientException e) {
            if (e.getStatus().getCode() == 404) {
                return false;
            } else {
                throw e;
            }
        }
    }

    @Override
    public boolean replaceAddressSpaceStatus(AddressSpace addressSpace) throws Exception {
        boolean exists = client.inNamespace(addressSpace.getMetadata().getNamespace()).withName(addressSpace.getMetadata().getName()).get() != null;
        if (!exists) {
            return false;
        }

        // Make a copy, so that no one else makes modifications to our instance.
        // This is important as we do put this instance into your cache.
        addressSpace = new AddressSpaceBuilder(addressSpace).build();

        try {

            final String statusUri = URLUtils.join(kubernetesClient.getMasterUrl().toString(), "apis", "enmasse.io", "v1beta1", "namespaces",
                    namespace, addressSpace.getMetadata().getNamespace(), addressSpace.getMetadata().getName(), "status");
            final RequestBody requestBody = RequestBody.create(MediaType.parse("application/json"), mapper.writeValueAsBytes(addressSpace));
            OkHttpClient baseClient = kubernetesClient.adapt(OkHttpClient.class);
            try (Response response = baseClient.newCall(new Request.Builder()
                    .method("PUT", requestBody)
                    .url(statusUri)
                    .build())
                    .execute()) {
                if (response.code() < 200 || response.code() >= 300) {
                    return false;
                }
                cache.replace(addressSpace);
            }
            return true;
        } catch (KubernetesClientException e) {
            if (e.getStatus().getCode() == 404) {
                return false;
            } else {
                throw e;
            }
        }
    }

    @Override
    public boolean deleteAddressSpace(AddressSpace addressSpace) {
        boolean exists = client.inNamespace(addressSpace.getMetadata().getNamespace()).withName(addressSpace.getMetadata().getName()).get() != null;
        if (!exists) {
            return false;
        }
        client.inNamespace(addressSpace.getMetadata().getNamespace()).delete(addressSpace);
        return true;
    }

    @Override
    public Set<AddressSpace> listAddressSpaces(String namespace) {
        return new HashSet<>(client.inNamespace(namespace).list().getItems());
    }

    @Override
    public Set<AddressSpace> listAddressSpacesWithLabels(String namespace, Map<String, String> labels) {
        return new HashSet<>(client.inNamespace(namespace).withLabels(labels).list().getItems());
    }

    @Override
    public Set<AddressSpace> listAllAddressSpaces() {
        return new HashSet<>(client.inAnyNamespace().list().getItems());
    }

    @Override
    public Set<AddressSpace> listAllAddressSpacesWithLabels(Map<String, String> labels) {
        return new HashSet<>(client.inAnyNamespace().withLabels(labels).list().getItems());
    }

    @Override
    public void deleteAddressSpaces(String namespace) {
        client.inNamespace(namespace).delete();
    }

    @Override
    public Watch watchAddressSpaces(CacheWatcher<AddressSpace> watcher, Duration resyncInterval) throws Exception {
        Reflector.Config<AddressSpace, AddressSpaceList> config = new Reflector.Config<>();
        watcher.onInit(() -> cache.list());
        config.setClock(Clock.systemUTC());
        config.setExpectedType(AddressSpace.class);
        config.setListerWatcher(this);
        config.setResyncInterval(resyncInterval);
        config.setWorkQueue(cache);
        config.setProcessor(map -> {
            if (cache.hasSynced()) {
                watcher.onUpdate();
            }
        });

        Reflector<AddressSpace, AddressSpaceList> reflector = new Reflector<>(config);
        Controller controller = new Controller(reflector);
        controller.start();
        return controller;
    }

    @Override
    public AddressApi withAddressSpace(AddressSpace addressSpace) {
        return KubeAddressApi.create(kubernetesClient, namespace, version);
    }

    @Override
    public AddressSpaceList list(ListOptions listOptions) {
        if (namespace != null) {
            return client.inNamespace(namespace).list();
        } else {
            return client.inAnyNamespace().list();
        }
    }

    @Override
    public io.fabric8.kubernetes.client.Watch watch(io.fabric8.kubernetes.client.Watcher<AddressSpace> watcher, ListOptions listOptions) {
        RequestConfig requestConfig = new RequestConfigBuilder()
                .withRequestTimeout(listOptions.getTimeoutSeconds())
                .build();
        if (namespace != null) {
            return kubernetesClient.withRequestConfig(requestConfig).call(c -> c.customResources(customResourceDefinition, AddressSpace.class, AddressSpaceList.class, DoneableAddressSpace.class)
                    .inNamespace(namespace)
                    .withResourceVersion(listOptions.getResourceVersion())
                    .watch(watcher));
        } else {
            return kubernetesClient.withRequestConfig(requestConfig).call(c -> c.customResources(customResourceDefinition, AddressSpace.class, AddressSpaceList.class, DoneableAddressSpace.class)
                    .inAnyNamespace()
                    .withResourceVersion(listOptions.getResourceVersion())
                    .watch(watcher));
        }
    }

}
