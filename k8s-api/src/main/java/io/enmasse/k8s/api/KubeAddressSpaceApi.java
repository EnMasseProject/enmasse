/*
 * Copyright 2019, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.k8s.api;

import io.enmasse.address.model.AddressSpace;
import io.enmasse.address.model.AddressSpaceList;
import io.enmasse.address.model.CoreCrd;
import io.enmasse.address.model.DoneableAddressSpace;
import io.enmasse.k8s.api.cache.*;
import io.fabric8.kubernetes.api.model.OwnerReference;
import io.fabric8.kubernetes.api.model.OwnerReferenceBuilder;
import io.fabric8.kubernetes.api.model.apiextensions.CustomResourceDefinition;
import io.fabric8.kubernetes.client.*;
import io.fabric8.kubernetes.client.dsl.MixedOperation;
import io.fabric8.kubernetes.client.dsl.Resource;

import java.time.Clock;
import java.time.Duration;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public class KubeAddressSpaceApi implements AddressSpaceApi, ListerWatcher<AddressSpace, AddressSpaceList> {
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
        OwnerReference ownerReference = new OwnerReferenceBuilder()
                .withApiVersion("enmasse.io/v1beta1")
                .withKind("AddressSpace")
                .withBlockOwnerDeletion(true)
                .withController(true)
                .withName(addressSpace.getMetadata().getName())
                .withUid(addressSpace.getMetadata().getUid())
                .build();
        return KubeAddressApi.create(kubernetesClient, namespace, ownerReference, version);
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
