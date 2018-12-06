/*
 * Copyright 2017-2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.k8s.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.enmasse.address.model.v1.CodecV1;
import io.enmasse.config.LabelKeys;
import io.enmasse.config.AnnotationKeys;
import io.enmasse.address.model.Address;
import io.enmasse.k8s.api.cache.*;
import io.fabric8.kubernetes.api.model.ConfigMap;
import io.fabric8.kubernetes.api.model.ConfigMapBuilder;
import io.fabric8.kubernetes.api.model.ConfigMapList;
import io.fabric8.kubernetes.client.KubernetesClientException;
import io.fabric8.kubernetes.client.RequestConfig;
import io.fabric8.kubernetes.client.RequestConfigBuilder;
import io.fabric8.openshift.client.NamespacedOpenShiftClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.time.Clock;
import java.time.Duration;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Implements the AddressApi using config maps.
 */
public class ConfigMapAddressApi implements AddressApi, ListerWatcher<ConfigMap, ConfigMapList> {

    private static final Logger log = LoggerFactory.getLogger(ConfigMapAddressApi.class);
    private final NamespacedOpenShiftClient client;
    private final String infraUuid;
    private final WorkQueue<ConfigMap> cache = new EventCache<>(new HasMetadataFieldExtractor<>());

    private final ObjectMapper mapper = CodecV1.getMapper();

    public ConfigMapAddressApi(NamespacedOpenShiftClient client, String infraUuid) {
        this.client = client;
        this.infraUuid = infraUuid;
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

    @SuppressWarnings("unchecked")
    private Address getAddressFromConfig(ConfigMap map) {
        Map<String, String> data = map.getData();

        try {
            Address address = mapper.readValue(data.get("config.json"), Address.class);
            Address.Builder builder = new Address.Builder(address);

            if (address.getUid() == null) {
                builder.setUid(map.getMetadata().getUid());
            }

            builder.setResourceVersion(map.getMetadata().getResourceVersion());

            if (address.getCreationTimestamp() == null) {
                builder.setCreationTimestamp(map.getMetadata().getCreationTimestamp());
            }

            if (address.getSelfLink() == null) {
                builder.setSelfLink("/apis/enmasse.io/v1alpha1/namespaces/" + address.getNamespace() + "/addressspaces/" + address.getAddressSpace());
            }
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
            if (namespace.equals(address.getNamespace())) {
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

        client.configMaps().withLabels(labels).delete();
    }

    @Override
    public void createAddress(Address address) {
        String name = getConfigMapName(address.getNamespace(), address.getName());
        ConfigMap map = create(address);
        client.configMaps().withName(name).create(map);
    }

    @Override
    public boolean replaceAddress(Address address) {
        ConfigMap newMap = null;
        try {
            String name = getConfigMapName(address.getNamespace(), address.getName());
            newMap = create(address);
            ConfigMap result;
            if (address.getResourceVersion() != null) {
                result = client.configMaps()
                        .withName(name)
                        .lockResourceVersion(address.getResourceVersion())
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
            } else if (e.getStatus().getCode() == 409) {
                // Replace locally cached even if there is a conflict to prevent stale address
                cache.replace(newMap);
                throw e;
            } else {
                throw e;
            }
        }
    }

    private String getConfigMapName(String namespace, String name) {
        return namespace + "." + name;
    }

    private ConfigMap create(Address address) {
        ConfigMapBuilder builder = new ConfigMapBuilder()
                .editOrNewMetadata()
                .withName(getConfigMapName(address.getNamespace(), address.getName()))
                .addToLabels(address.getLabels())
                .addToLabels(LabelKeys.TYPE, "address-config")
                .addToLabels(LabelKeys.INFRA_UUID, infraUuid)
                .addToLabels(LabelKeys.INFRA_TYPE, "any")
                .addToAnnotations(address.getAnnotations())
                // TODO: Support other ways of doing this
                .addToAnnotations(AnnotationKeys.ADDRESS_SPACE, address.getAddressSpace())
                .endMetadata();

        if (address.getResourceVersion() != null) {
            builder.editOrNewMetadata()
                    .withResourceVersion(address.getResourceVersion())
                    .endMetadata();
        }

        try {
            // Reset resource version to avoid unneeded extra writes
            builder.addToData("config.json", mapper.writeValueAsString(new Address.Builder(address).setResourceVersion(null).build()));
            return builder.build();
        } catch (IOException e) {
            log.info("Error serializing address for {}", address, e);
            throw new UncheckedIOException(e);
        }
    }

    @Override
    public boolean deleteAddress(Address address) {
        Boolean deleted = client.configMaps().withName(getConfigMapName(address.getNamespace(), address.getName())).delete();
        return deleted != null && deleted;
    }

    @Override
    public Watch watchAddresses(Watcher<Address> watcher, Duration resyncInterval) {
        Reflector.Config<ConfigMap, ConfigMapList> config = new Reflector.Config<>();
        config.setClock(Clock.systemUTC());
        config.setExpectedType(ConfigMap.class);
        config.setListerWatcher(this);
        config.setResyncInterval(resyncInterval);
        config.setWorkQueue(cache);
        config.setProcessor(map -> {
                    if (cache.hasSynced()) {
                        watcher.onUpdate(cache.list().stream()
                                .map(this::getAddressFromConfig)
                                .collect(Collectors.toList()));
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
