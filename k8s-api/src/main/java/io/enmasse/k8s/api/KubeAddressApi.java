/*
 * Copyright 2019, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.k8s.api;

import io.enmasse.address.model.Address;
import io.enmasse.address.model.AddressBuilder;
import io.enmasse.address.model.AddressList;
import io.enmasse.address.model.CoreCrd;
import io.enmasse.address.model.DoneableAddress;
import io.enmasse.config.AnnotationKeys;
import io.enmasse.k8s.api.cache.*;
import io.fabric8.kubernetes.api.model.apiextensions.CustomResourceDefinition;
import io.fabric8.kubernetes.client.KubernetesClientException;
import io.fabric8.kubernetes.client.NamespacedKubernetesClient;
import io.fabric8.kubernetes.client.RequestConfig;
import io.fabric8.kubernetes.client.RequestConfigBuilder;
import io.fabric8.kubernetes.client.dsl.MixedOperation;
import io.fabric8.kubernetes.client.dsl.Resource;

import java.time.Clock;
import java.time.Duration;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;

public class KubeAddressApi implements AddressApi, ListerWatcher<Address, AddressList> {
    private final NamespacedKubernetesClient kubernetesClient;
    private final MixedOperation<Address, AddressList, DoneableAddress, Resource<Address, DoneableAddress>> client;
    private final String namespace;
    private final CustomResourceDefinition customResourceDefinition;
    private final WorkQueue<Address> cache = new EventCache<>(new HasMetadataFieldExtractor<>());
    private final String version;


    private KubeAddressApi(NamespacedKubernetesClient kubeClient, String namespace, CustomResourceDefinition customResourceDefinition, String version) {
        this.kubernetesClient = kubeClient;
        this.namespace = namespace;
        this.version = version;
        this.client = kubeClient.customResources(customResourceDefinition, Address.class, AddressList.class, DoneableAddress.class);
        this.customResourceDefinition = customResourceDefinition;
    }

    public static AddressApi create(NamespacedKubernetesClient kubernetesClient, String namespace, String version) {
        return new KubeAddressApi(kubernetesClient, namespace, CoreCrd.addresses(), version);
    }

    @Override
    public Optional<Address> getAddressWithName(String namespace, String id) {
        return Optional.ofNullable(client.inNamespace(namespace).withName(id).get());
    }

    @Override
    public void createAddress(Address address) {
        address = setDefaults(address);
        client.inNamespace(address.getMetadata().getNamespace()).create(address);
    }

    @Override
    public boolean replaceAddress(Address address) {
        boolean exists = client.inNamespace(address.getMetadata().getNamespace()).withName(address.getMetadata().getName()).get() != null;
        if (!exists) {
            return false;
        }

        address = setDefaults(address);
        try {
            Address result = client
                    .inNamespace(address.getMetadata().getNamespace())
                    .withName(address.getMetadata().getName())
                    .lockResourceVersion(address.getMetadata().getResourceVersion())
                    .replace(address);

            cache.replace(address);
            return result != null;
        } catch (KubernetesClientException e) {
            if (e.getStatus().getCode() == 404) {
                return false;
            } else {
                throw e;
            }
        }
    }

    private Address setDefaults(Address address) {
        String addressSpaceName = Address.extractAddressSpace(address);
        AddressBuilder builder = new AddressBuilder(address)
                .editOrNewMetadata()
                .addToAnnotations(AnnotationKeys.ADDRESS_SPACE, addressSpaceName)
                .endMetadata();

        if (version != null) {
            builder.editOrNewMetadata()
                    .addToAnnotations(AnnotationKeys.VERSION, version)
                    .endMetadata();
        }

        return builder.build();
    }

    @Override
    public boolean deleteAddress(Address address) {
        return client
                .inNamespace(address.getMetadata().getNamespace())
                .delete(address);
    }

    @Override
    public ContinuationResult<Address> listAddresses(final String namespace, final Integer limit, final ContinuationResult<Address> continueValue,
            final Map<String, String> labels) {

        return ContinuationResult.from(
                this.client.inNamespace(namespace)
                        .withLabels(labels != null ? labels : Collections.emptyMap())
                        .list(limit, continueValue != null ? continueValue.getContinuation() : null));


    }

    public void deleteAddresses(String namespace) {
        client.inNamespace(namespace).delete();
    }

    @Override
    public Watch watchAddresses(CacheWatcher<Address> watcher, Duration resyncInterval) throws Exception {
        Reflector.Config<Address, AddressList> config = new Reflector.Config<>();
        watcher.onInit(() -> cache.list());
        config.setClock(Clock.systemUTC());
        config.setExpectedType(Address.class);
        config.setListerWatcher(this);
        config.setResyncInterval(resyncInterval);
        config.setWorkQueue(cache);
        config.setProcessor(map -> {
            if (cache.hasSynced()) {
                watcher.onUpdate();
            }
        });

        Reflector<Address, AddressList> reflector = new Reflector<>(config);
        Controller controller = new Controller(reflector);
        controller.start();
        return controller;
    }

    @Override
    public AddressList list(ListOptions listOptions) {
        if (namespace != null) {
            return client.inNamespace(namespace).list();
        } else {
            return client.inAnyNamespace().list();
        }
    }

    @Override
    public io.fabric8.kubernetes.client.Watch watch(io.fabric8.kubernetes.client.Watcher<Address> watcher, ListOptions listOptions) {
        RequestConfig requestConfig = new RequestConfigBuilder()
                .withRequestTimeout(listOptions.getTimeoutSeconds())
                .build();
        if (namespace != null) {
            return kubernetesClient.withRequestConfig(requestConfig).call(c -> c.customResources(customResourceDefinition, Address.class, AddressList.class, DoneableAddress.class)
                    .inNamespace(namespace)
                    .withResourceVersion(listOptions.getResourceVersion())
                    .watch(watcher));
        } else {
            return kubernetesClient.withRequestConfig(requestConfig).call(c -> c.customResources(customResourceDefinition, Address.class, AddressList.class, DoneableAddress.class)
                    .inAnyNamespace()
                    .withResourceVersion(listOptions.getResourceVersion())
                    .watch(watcher));
        }
    }
}
