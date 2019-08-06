/*
 * Copyright 2017-2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.k8s.api;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.time.Clock;
import java.time.Duration;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import io.fabric8.kubernetes.api.model.OwnerReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.ObjectMapper;

import io.enmasse.address.model.Address;
import io.enmasse.address.model.AddressBuilder;
import io.enmasse.common.model.AbstractHasMetadataFluent.MetadataNested;
import io.enmasse.config.AnnotationKeys;
import io.enmasse.config.LabelKeys;
import io.enmasse.k8s.api.cache.CacheWatcher;
import io.enmasse.k8s.api.cache.Controller;
import io.enmasse.k8s.api.cache.EventCache;
import io.enmasse.k8s.api.cache.HasMetadataFieldExtractor;
import io.enmasse.k8s.api.cache.ListOptions;
import io.enmasse.k8s.api.cache.ListerWatcher;
import io.enmasse.k8s.api.cache.Reflector;
import io.enmasse.k8s.api.cache.WorkQueue;
import io.fabric8.kubernetes.api.model.ConfigMap;
import io.fabric8.kubernetes.api.model.ConfigMapBuilder;
import io.fabric8.kubernetes.api.model.ConfigMapList;
import io.fabric8.kubernetes.client.KubernetesClientException;
import io.fabric8.kubernetes.client.NamespacedKubernetesClient;
import io.fabric8.kubernetes.client.RequestConfig;
import io.fabric8.kubernetes.client.RequestConfigBuilder;

/**
 * Implements the AddressApi using config maps.
 */
public class ConfigMapAddressApi implements AddressApi, ListerWatcher<ConfigMap, ConfigMapList> {

    private static final Logger log = LoggerFactory.getLogger(ConfigMapAddressApi.class);
    private final NamespacedKubernetesClient client;
    private final String infraUuid;
    private final WorkQueue<ConfigMap> cache = new EventCache<>(new HasMetadataFieldExtractor<>());
    private ObjectMapper mapper = new ObjectMapper();
    private final OwnerReference ownerReference;
    private final String version;

    public ConfigMapAddressApi(NamespacedKubernetesClient client, String infraUuid, OwnerReference ownerReference, String version) {
        this.client = client;
        this.infraUuid = infraUuid;
        this.ownerReference = ownerReference;
        this.version = version;
    }

    @Override
    public Optional<Address> getAddressWithName(String namespace, String name) {
        ConfigMap map = client.configMaps().withName(getConfigMapName(namespace, name)).get();
        if (map == null) {
            return Optional.empty();
        } else {
            return Optional.of(getAddressFromConfig(map));
        }
    }

    private Address getAddressFromConfig(ConfigMap map) {
        Map<String, String> data = map.getData();

        try {
            Address address = mapper.readValue(data.get("config.json"), Address.class);
            AddressBuilder builder = new AddressBuilder(address);
            MetadataNested<AddressBuilder> metadataBuilder = builder.editOrNewMetadata();

            if (address.getMetadata().getUid() == null) {
                metadataBuilder.withUid(map.getMetadata().getUid());
            }

            metadataBuilder.withResourceVersion(map.getMetadata().getResourceVersion());

            if (address.getMetadata().getCreationTimestamp() == null) {
                metadataBuilder.withCreationTimestamp(map.getMetadata().getCreationTimestamp());
            }

            if (address.getMetadata().getSelfLink() == null) {
                metadataBuilder.withSelfLink("/apis/enmasse.io/v1beta1/namespaces/" + address.getMetadata().getNamespace() + "/addressspaces/" + Address.extractAddressSpace(address));
            }

            // commit changes to metadata
            metadataBuilder.endMetadata();

            return builder.build();
        } catch (IOException e) {
            log.error("Error decoding address from configmap : {}", map, e);
            throw new UncheckedIOException(e);
        }
    }

    @Override
    public Set<Address> listAddresses(String namespace) {
        return listAddressesWithLabels(namespace, Collections.emptyMap());
    }

    @Override
    public Set<Address> listAddressesWithLabels(String namespace, Map<String, String> labelSelector) {
        Map<String, String> labels = new LinkedHashMap<>(labelSelector);
        labels.put(LabelKeys.TYPE, "address-config");
        labels.put(LabelKeys.INFRA_UUID, infraUuid);

        Set<Address> addresses = new LinkedHashSet<>();
        ConfigMapList list = client.configMaps().withLabels(labels).list();
        for (ConfigMap config : list.getItems()) {
            Address address = getAddressFromConfig(config);
            if (namespace.equals(address.getMetadata().getNamespace())) {
                addresses.add(address);
            }
        }
        return addresses;
    }

    @Override
    public void deleteAddresses(String namespace) {
        Map<String, String> labels = new LinkedHashMap<>();
        labels.put(LabelKeys.TYPE, "address-config");
        labels.put(LabelKeys.INFRA_UUID, infraUuid);
        labels.put(LabelKeys.NAMESPACE, namespace);

        client.configMaps().withLabels(labels).withPropagationPolicy("Background").delete();
    }

    @Override
    public void createAddress(Address address) {
        String name = getConfigMapName(address.getMetadata().getNamespace(), address.getMetadata().getName());
        ConfigMap map = create(address);
        client.configMaps().withName(name).create(map);
    }

    @Override
    public boolean replaceAddress(Address address) {
        ConfigMap newMap = null;
        try {
            String name = getConfigMapName(address.getMetadata().getNamespace(), address.getMetadata().getName());
            newMap = create(address);
            ConfigMap result;
            if (address.getMetadata().getResourceVersion() != null) {
                result = client.configMaps()
                        .withName(name)
                        .lockResourceVersion(address.getMetadata().getResourceVersion())
                        .replace(newMap);

            } else {
                result = client.configMaps()
                        .withName(name)
                        .replace(newMap);
            }
            cache.replace(newMap);
            return result != null;
        } catch (KubernetesClientException e) {
            if (e.getStatus().getCode() == 404) {
                return false;
            } else {
                throw e;
            }
        }
    }

    private String getConfigMapName(String namespace, String name) {
        return namespace + "." + name;
    }

    private ConfigMap create(Address address) {
        String addressSpaceName = Address.extractAddressSpace(address);
        ConfigMapBuilder builder = new ConfigMapBuilder()
                .editOrNewMetadata()
                .withName(getConfigMapName(address.getMetadata().getNamespace(), address.getMetadata().getName()))
                .addToLabels(address.getMetadata().getLabels())
                .addToLabels(LabelKeys.TYPE, "address-config")
                .addToLabels(LabelKeys.INFRA_UUID, infraUuid)

                .addToAnnotations(address.getMetadata().getAnnotations())
                // TODO: Support other ways of doing this
                .addToAnnotations(AnnotationKeys.ADDRESS_SPACE, addressSpaceName)
                .endMetadata();

        if (ownerReference != null) {
            builder.editMetadata()
                .addToOwnerReferences(ownerReference)
                .endMetadata();
        }

        if (version != null) {
            address.putAnnotation(AnnotationKeys.VERSION, version);
        }

        if (address.getMetadata().getResourceVersion() != null) {
            builder.editOrNewMetadata()
                    .withResourceVersion(address.getMetadata().getResourceVersion())
                    .endMetadata();
        }

        try {
            // Reset resource version to avoid unneeded extra writes
            final Address newAddress = new AddressBuilder(address).editOrNewMetadata().withResourceVersion(null).endMetadata().build();
            builder.addToData("config.json", mapper.writeValueAsString(newAddress));
            return builder.build();
        } catch (IOException e) {
            log.info("Error serializing address for {}", address, e);
            throw new UncheckedIOException(e);
        }
    }

    @Override
    public boolean deleteAddress(Address address) {
        Boolean deleted = client.configMaps().withName(getConfigMapName(address.getMetadata().getNamespace(), address.getMetadata().getName())).cascading(true).delete();
        return deleted != null && deleted;
    }

    @Override
    public Watch watchAddresses(CacheWatcher<Address> watcher, Duration resyncInterval) {
        watcher.onInit(() -> cache.list().stream()
                .map(ConfigMapAddressApi.this::getAddressFromConfig)
                .collect(Collectors.toList()));

        Reflector.Config<ConfigMap, ConfigMapList> config = new Reflector.Config<>();
        config.setClock(Clock.systemUTC());
        config.setExpectedType(ConfigMap.class);
        config.setListerWatcher(this);
        config.setResyncInterval(resyncInterval);
        config.setWorkQueue(cache);
        config.setProcessor(map -> {
                    if (cache.hasSynced()) {
                        watcher.onUpdate();
                    }
                });

        Reflector<ConfigMap, ConfigMapList> reflector = new Reflector<>(config);
        Controller controller = new Controller(reflector);
        controller.start();
        return controller;
    }

    @Override
    public ConfigMapList list(ListOptions listOptions) {
        return client.configMaps()
                        .withLabel(LabelKeys.TYPE, "address-config")
                        .withLabel(LabelKeys.INFRA_UUID, infraUuid)
                        .list();
    }

    @Override
    public io.fabric8.kubernetes.client.Watch watch(io.fabric8.kubernetes.client.Watcher<ConfigMap> watcher, ListOptions listOptions) {
        RequestConfig requestConfig = new RequestConfigBuilder()
                .withRequestTimeout(listOptions.getTimeoutSeconds())
                .build();
        return client.withRequestConfig(requestConfig).call(c ->
                c.configMaps()
                        .withLabel(LabelKeys.TYPE, "address-config")
                        .withLabel(LabelKeys.INFRA_UUID, infraUuid)
                        .withResourceVersion(listOptions.getResourceVersion())
                        .watch(watcher));
    }
}
