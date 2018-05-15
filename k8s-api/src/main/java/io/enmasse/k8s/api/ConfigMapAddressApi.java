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
import io.fabric8.kubernetes.client.RequestConfig;
import io.fabric8.kubernetes.client.RequestConfigBuilder;
import io.fabric8.openshift.client.NamespacedOpenShiftClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
    private final String namespace;

    private final ObjectMapper mapper = CodecV1.getMapper();

    public ConfigMapAddressApi(NamespacedOpenShiftClient client, String namespace) {
        this.client = client;
        this.namespace = namespace;
    }

    @Override
    public Optional<Address> getAddressWithName(String namespace, String name) {
        ConfigMap map = client.configMaps().inNamespace(this.namespace).withName(getConfigMapName(name)).get();
        if (map == null) {
            return Optional.empty();
        } else {
            return Optional.of(getAddressFromConfig(map));
        }
    }

    @SuppressWarnings("unchecked")
    private Address getAddressFromConfig(ConfigMap configMap) {
        Map<String, String> data = configMap.getData();

        try {
            Address address = mapper.readValue(data.get("config.json"), Address.class);
            Address.Builder builder = new Address.Builder(address);

            if (address.getUid() == null) {
                builder.setUid(configMap.getMetadata().getUid());
            }

            if (address.getResourceVersion() == null) {
                builder.setResourceVersion(configMap.getMetadata().getResourceVersion());
            }

            if (address.getCreationTimestamp() == null) {
                builder.setCreationTimestamp(configMap.getMetadata().getCreationTimestamp());
            }

            if (address.getSelfLink() == null) {
                builder.setSelfLink("/apis/enmasse.io/v1alpha1/namespaces/" + address.getNamespace() + "/addressspaces/" + address.getAddressSpace());
            }
            return builder.build();
        } catch (Exception e) {
            log.warn("Unable to decode address", e);
            throw new RuntimeException(e);
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

        Set<Address> addresses = new LinkedHashSet<>();
        ConfigMapList list = client.configMaps().inNamespace(this.namespace).withLabels(labels).list();
        for (ConfigMap config : list.getItems()) {
            Address address = getAddressFromConfig(config);
            if (namespace.equals(address.getNamespace())) {
                addresses.add(address);
            }
        }
        return addresses;
    }

    @Override
    public void createAddress(Address address) {
        String name = getConfigMapName(address.getName());
        ConfigMap map = create(address);
        if (map != null) {
            client.configMaps().inNamespace(namespace).withName(name).create(map);
        }
    }

    @Override
    public void replaceAddress(Address address) {
        String name = getConfigMapName(address.getName());
        ConfigMap previous = client.configMaps().inNamespace(namespace).withName(name).get();
        if (previous == null) {
            return;
        }
        ConfigMap newMap = create(address);
        if (newMap != null) {
            client.configMaps().inNamespace(namespace).withName(name).replace(newMap);
        }
    }

    private static String getConfigMapName(String name) {
        return name;
    }

    private ConfigMap create(Address address) {
        ConfigMapBuilder builder = new ConfigMapBuilder()
                .editOrNewMetadata()
                .withName(getConfigMapName(address.getName()))
                .addToLabels(address.getLabels())
                .addToLabels(LabelKeys.TYPE, "address-config")
                .addToAnnotations(address.getAnnotations())
                // TODO: Support other ways of doing this
                .addToAnnotations(AnnotationKeys.CLUSTER_ID, address.getName())
                .addToAnnotations(AnnotationKeys.ADDRESS_SPACE, address.getAddressSpace())
                .endMetadata();

        if (address.getResourceVersion() != null) {
            builder.editOrNewMetadata()
                    .withResourceVersion(address.getResourceVersion());
        }

        try {
            builder.addToData("config.json", mapper.writeValueAsString(address));
            return builder.build();
        } catch (Exception e) {
            log.info("Error serializing address for {}", address, e);
            return null;
        }
    }

    @Override
    public void deleteAddress(Address address) {
        client.configMaps().inNamespace(namespace).withName(getConfigMapName(address.getName())).delete();
    }

    @Override
    public Watch watchAddresses(Watcher<Address> watcher, Duration resyncInterval) {
        WorkQueue<ConfigMap> queue = new FifoQueue<>(config -> config.getMetadata().getName());
        Reflector.Config<ConfigMap, ConfigMapList> config = new Reflector.Config<>();
        config.setClock(Clock.systemUTC());
        config.setExpectedType(ConfigMap.class);
        config.setListerWatcher(this);
        config.setResyncInterval(resyncInterval);
        config.setWorkQueue(queue);
        config.setProcessor(map -> {
                    if (queue.hasSynced()) {
                        long start = System.nanoTime();
                        watcher.onUpdate(queue.list().stream()
                                .map(this::getAddressFromConfig)
                                .collect(Collectors.toSet()));
                        long end = System.nanoTime();
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
                        .inNamespace(namespace)
                        .withLabel(LabelKeys.TYPE, "address-config")
                        .list();
    }

    @Override
    public io.fabric8.kubernetes.client.Watch watch(io.fabric8.kubernetes.client.Watcher<ConfigMap> watcher, ListOptions listOptions) {
        RequestConfig requestConfig = new RequestConfigBuilder()
                .withRequestTimeout(listOptions.getTimeoutSeconds())
                .build();
        return client.withRequestConfig(requestConfig).call(c ->
                c.configMaps()
                        .inNamespace(namespace)
                        .withLabel(LabelKeys.TYPE, "address-config")
                        .withResourceVersion(listOptions.getResourceVersion())
                        .watch(watcher));
    }
}
