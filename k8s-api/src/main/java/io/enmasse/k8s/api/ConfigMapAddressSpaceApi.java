/*
 * Copyright 2017-2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.k8s.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.enmasse.address.model.v1.CodecV1;
import io.enmasse.config.AnnotationKeys;
import io.enmasse.config.LabelKeys;
import io.enmasse.address.model.AddressSpace;
import io.enmasse.k8s.api.cache.*;
import io.fabric8.kubernetes.api.model.ConfigMap;
import io.fabric8.kubernetes.api.model.ConfigMapList;
import io.fabric8.kubernetes.api.model.DoneableConfigMap;
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
 * Implementation of the AddressSpace API towards Kubernetes
 */
public class ConfigMapAddressSpaceApi implements AddressSpaceApi, ListerWatcher<ConfigMap, ConfigMapList> {
    protected final Logger log = LoggerFactory.getLogger(getClass().getName());
    private final NamespacedOpenShiftClient client;
    private final ObjectMapper mapper = CodecV1.getMapper();

    public ConfigMapAddressSpaceApi(NamespacedOpenShiftClient client) {
        this.client = client;
    }

    private static String getConfigMapName(String namespace, String name) {
        return namespace + name;
    }

    @Override
    public Optional<AddressSpace> getAddressSpaceWithName(String namespace, String name) {

        ConfigMap map = client.configMaps().withName(getConfigMapName(namespace, name)).get();
        if (map == null) {
            return Optional.empty();
        } else {
            return Optional.of(getAddressSpaceFromConfig(map));
        }
    }

    @Override
    public void createAddressSpace(AddressSpace addressSpace) throws Exception {
        try {
            create(client.configMaps().createNew(), addressSpace);
        } catch (Exception e) {
            log.error("Error creating {}", addressSpace.getName());
            throw e;
        }
    }

    @Override
    public void replaceAddressSpace(AddressSpace addressSpace) throws Exception {
        ConfigMap previous = client.configMaps().withName(getConfigMapName(addressSpace.getNamespace(), addressSpace.getName())).get();
        if (previous == null) {
            return;
        }
        try {
            create(client.configMaps().createOrReplaceWithNew(), addressSpace);
        } catch (Exception e) {
            log.error("Error replacing {}", addressSpace.getName());
            throw e;
        }
    }

    private void create(DoneableConfigMap config, AddressSpace addressSpace) throws Exception {
        Map<String, String> labels = new HashMap<>(addressSpace.getLabels());
        String name = getConfigMapName(addressSpace.getNamespace(), addressSpace.getName());
        labels.put(LabelKeys.TYPE, "address-space");
        config.withNewMetadata()
                .withName(name)
                .addToLabels(labels)
                .withResourceVersion(addressSpace.getResourceVersion())
                .addToAnnotations(addressSpace.getAnnotations())
                .endMetadata()
                .addToData("config.json", mapper.writeValueAsString(addressSpace))
                .done();
    }

    @Override
    public void deleteAddressSpace(AddressSpace addressSpace) {
        String name = getConfigMapName(addressSpace.getNamespace(), addressSpace.getName());
        client.configMaps().withName(name).delete();
    }

    @Override
    public Set<AddressSpace> listAddressSpaces(String namespace) {
        Set<AddressSpace> instances = new LinkedHashSet<>();
        for (ConfigMap map : list(namespace).getItems()) {
            instances.add(getAddressSpaceFromConfig(map));
        }
        return instances;
    }

    @Override
    public Set<AddressSpace> listAddressSpacesWithLabels(String namespace, Map<String, String> labels) {
        Set<AddressSpace> instances = new LinkedHashSet<>();
        labels = new LinkedHashMap<>(labels);
        labels.put(LabelKeys.TYPE, "address-space");
        labels.put(LabelKeys.NAMESPACE, namespace);
        ConfigMapList list = client.configMaps().withLabels(labels).list();
        for (ConfigMap map : list.getItems()) {
            instances.add(getAddressSpaceFromConfig(map));
        }
        return instances;
    }


    private AddressSpace getAddressSpaceFromConfig(ConfigMap map) {
        try {
            AddressSpace addressSpace = mapper.readValue(map.getData().get("config.json"), AddressSpace.class);
            AddressSpace.Builder builder = new AddressSpace.Builder(addressSpace);
            if (addressSpace.getUid() == null) {
                builder.setUid(map.getMetadata().getUid());
            }

            if (addressSpace.getResourceVersion() == null) {
                builder.setResourceVersion(map.getMetadata().getResourceVersion());
            }

            if (addressSpace.getCreationTimestamp() == null) {
                builder.setCreationTimestamp(map.getMetadata().getCreationTimestamp());
            }

            if (addressSpace.getSelfLink() == null) {
                builder.setSelfLink("/apis/enmasse.io/v1alpha1/namespaces/" + addressSpace.getNamespace() + "/addressspaces/" + addressSpace.getName());
            }

            return builder.build();
        } catch (Exception e) {
            log.error("Error decoding address space", e);
            throw new RuntimeException(e);
        }
    }

    @Override
    public Watch watchAddressSpaces(Watcher<AddressSpace> watcher, Duration resyncInterval) {
        WorkQueue<ConfigMap> queue = new FifoQueue<>(config -> config.getMetadata().getName());
        Reflector.Config<ConfigMap, ConfigMapList> config = new Reflector.Config<>();
        config.setClock(Clock.systemUTC());
        config.setExpectedType(ConfigMap.class);
        config.setListerWatcher(this);
        config.setResyncInterval(resyncInterval);
        config.setWorkQueue(queue);
        config.setProcessor(map -> {
            if (queue.hasSynced()) {
                watcher.onUpdate(queue.list().stream()
                        .map(this::getAddressSpaceFromConfig)
                        .collect(Collectors.toSet()));
            }
        });

        Reflector<ConfigMap, ConfigMapList> reflector = new Reflector<>(config);
        Controller controller = new Controller(reflector);
        controller.start();
        return controller;
    }

    @Override
    public AddressApi withAddressSpace(AddressSpace addressSpace) {
        return new ConfigMapAddressApi(client, addressSpace.getAnnotation(AnnotationKeys.NAMESPACE));
    }

    private ConfigMapList list(String namespace) {
        return client.configMaps()
                .inNamespace(client.getNamespace())
                .withLabel(LabelKeys.TYPE, "address-space")
                .withLabel(LabelKeys.NAMESPACE, namespace)
                .list();
    }

    @Override
    public ConfigMapList list(ListOptions listOptions) {
        return client.configMaps()
                .inNamespace(client.getNamespace())
                .withLabel(LabelKeys.TYPE, "address-space")
                .list();
    }

    @Override
    public io.fabric8.kubernetes.client.Watch watch(io.fabric8.kubernetes.client.Watcher<ConfigMap> watcher, ListOptions listOptions) {
        RequestConfig requestConfig = new RequestConfigBuilder()
                .withRequestTimeout(listOptions.getTimeoutSeconds())
                .build();
        return client.withRequestConfig(requestConfig).call(c ->
                c.configMaps()
                        .inNamespace(client.getNamespace())
                        .withLabel(LabelKeys.TYPE, "address-space")
                        .withResourceVersion(listOptions.getResourceVersion())
                        .watch(watcher));
    }
}
