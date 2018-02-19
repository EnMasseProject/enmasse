/*
 * Copyright 2017-2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.k8s.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.enmasse.address.model.KubeUtil;
import io.enmasse.address.model.v1.CodecV1;
import io.enmasse.config.LabelKeys;
import io.enmasse.config.AnnotationKeys;
import io.enmasse.address.model.Address;
import io.fabric8.kubernetes.api.model.ConfigMap;
import io.fabric8.kubernetes.api.model.ConfigMapBuilder;
import io.fabric8.kubernetes.api.model.ConfigMapList;
import io.fabric8.kubernetes.client.KubernetesClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * Implements the AddressApi using config maps.
 */
public class ConfigMapAddressApi implements AddressApi, Resource<Address> {

    private static final Logger log = LoggerFactory.getLogger(ConfigMapAddressApi.class);
    private final KubernetesClient client;
    private final String namespace;

    private final ObjectMapper mapper = CodecV1.getMapper();

    public ConfigMapAddressApi(KubernetesClient client, String namespace) {
        this.client = client;
        this.namespace = namespace;
    }

    @Override
    public Optional<Address> getAddressWithName(String name) {
        ConfigMap map = client.configMaps().inNamespace(namespace).withName(name).get();
        if (map == null) {
            return Optional.empty();
        } else {
            return Optional.of(getAddressFromConfig(map));
        }
    }

    @Override
    public Optional<Address> getAddressWithUuid(String uuid) {
        Map<String, String> labels = new LinkedHashMap<>();
        labels.put(LabelKeys.TYPE, "address-config");
        labels.put(LabelKeys.UUID, uuid);

        ConfigMapList list = client.configMaps().inNamespace(namespace).withLabels(labels).list();
        if (list.getItems().isEmpty()) {
            return Optional.empty();
        } else {
            return Optional.of(getAddressFromConfig(list.getItems().get(0)));
        }
    }

    @SuppressWarnings("unchecked")
    private Address getAddressFromConfig(ConfigMap configMap) {
        Map<String, String> data = configMap.getData();

        try {
            Address.Builder builder = new Address.Builder(mapper.readValue(data.get("config.json"), Address.class));
            builder.setVersion(configMap.getMetadata().getResourceVersion());
            return builder.build();
        } catch (Exception e) {
            log.warn("Unable to decode address", e);
            throw new RuntimeException(e);
        }
    }

    @Override
    public Set<Address> listAddresses() {
        Map<String, String> labels = new LinkedHashMap<>();
        labels.put(LabelKeys.TYPE, "address-config");

        Set<Address> addresses = new LinkedHashSet<>();
        ConfigMapList list = client.configMaps().inNamespace(namespace).withLabels(labels).list();
        for (ConfigMap config : list.getItems()) {
            addresses.add(getAddressFromConfig(config));
        }
        return addresses;
    }

    @Override
    public void createAddress(Address address) {
        String name = address.getName();
        ConfigMap map = create(address);
        if (map != null) {
            client.configMaps().inNamespace(namespace).withName(name).create(map);
        }
    }

    @Override
    public void replaceAddress(Address address) {
        String name = address.getName();
        ConfigMap previous = client.configMaps().inNamespace(namespace).withName(name).get();
        if (previous == null) {
            return;
        }
        ConfigMap newMap = create(address);
        if (newMap != null) {
            client.configMaps().inNamespace(namespace).withName(name).replace(newMap);
        }
    }

    private ConfigMap create(Address address) {
        String name = address.getName();
        ConfigMapBuilder builder = new ConfigMapBuilder()
                .editOrNewMetadata()
                .withName(name)
                .addToLabels(LabelKeys.TYPE, "address-config")
                // TODO: Support other ways of doing this
                .addToAnnotations(AnnotationKeys.CLUSTER_ID, name)
                .addToAnnotations(AnnotationKeys.ADDRESS_SPACE, address.getAddressSpace())
                .endMetadata();

        if (address.getVersion() != null) {
            builder.editOrNewMetadata()
                    .withResourceVersion(address.getVersion());
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
        String name = address.getName();
        client.configMaps().inNamespace(namespace).withName(name).delete();
    }

    @Override
    public Watch watchAddresses(Watcher<Address> watcher, boolean useEventLoop) throws Exception {
        ResourceController<Address> controller = ResourceController.create(this, watcher, useEventLoop);
        controller.start();
        return controller::stop;
    }

    @Override
    public Watch watchAddresses(Watcher<Address> watcher) throws Exception {
        ResourceController<Address> controller = ResourceController.create(this, watcher);
        controller.start();
        return controller::stop;
    }

    @Override
    public io.fabric8.kubernetes.client.Watch watchResources(io.fabric8.kubernetes.client.Watcher watcher) {
        Map<String, String> labels = new LinkedHashMap<>();
        labels.put(LabelKeys.TYPE, "address-config");
        return client.configMaps().inNamespace(namespace).withLabels(labels).watch(watcher);
    }

    @Override
    public Set<Address> listResources() {
        return listAddresses();
    }
}
