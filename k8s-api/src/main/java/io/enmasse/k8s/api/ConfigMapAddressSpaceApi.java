/*
 * Copyright 2017-2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.k8s.api;

import java.io.Closeable;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.time.Clock;
import java.time.Duration;
import java.util.*;
import java.util.stream.Collectors;

import io.enmasse.address.model.AddressSpaceList;
import io.fabric8.kubernetes.api.model.ListMeta;
import io.fabric8.kubernetes.client.Watcher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.ObjectMapper;

import io.enmasse.address.model.AddressSpace;
import io.enmasse.address.model.AddressSpaceBuilder;
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
 * Implementation of the AddressSpace API towards Kubernetes
 */
public class ConfigMapAddressSpaceApi implements AddressSpaceApi, ListerWatcher<ConfigMap, ConfigMapList> {
    protected final Logger log = LoggerFactory.getLogger(getClass().getName());
    private final NamespacedKubernetesClient client;
    private final ObjectMapper mapper = new ObjectMapper();

    public ConfigMapAddressSpaceApi(NamespacedKubernetesClient client) {
        this.client = client;
    }

    private static String getConfigMapName(String namespace, String name) {
        return namespace + "." + name;
    }

    private final WorkQueue<ConfigMap> cache = new EventCache<>(new HasMetadataFieldExtractor<>());

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
    public void createAddressSpace(AddressSpace addressSpace) {
        String name = getConfigMapName(addressSpace.getMetadata().getNamespace(), addressSpace.getMetadata().getName());
        ConfigMap map = create(addressSpace);
        client.configMaps().withName(name).create(map);
    }

    @Override
    public boolean replaceAddressSpace(AddressSpace addressSpace) {
        ConfigMap newMap = null;
        try {
            String name = getConfigMapName(addressSpace.getMetadata().getNamespace(), addressSpace.getMetadata().getName());
            newMap = create(addressSpace);
            ConfigMap result;
            if (addressSpace.getMetadata().getResourceVersion() != null) {
                result = client.configMaps()
                        .withName(name)
                        .lockResourceVersion(addressSpace.getMetadata().getResourceVersion())
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

    private ConfigMap create(AddressSpace addressSpace) {
        Map<String, String> labels = addressSpace.getMetadata().getLabels();
        if ( labels == null )  {
            labels = new HashMap<>();
            addressSpace.getMetadata().setLabels(labels);
        }
        String name = getConfigMapName(addressSpace.getMetadata().getNamespace(), addressSpace.getMetadata().getName());
        labels.put(LabelKeys.TYPE, "address-space");
        labels.put(LabelKeys.NAMESPACE, addressSpace.getMetadata().getNamespace());
        ConfigMapBuilder builder = new ConfigMapBuilder()
                .editOrNewMetadata()
                .withName(name)
                .addToLabels(labels)
                .addToAnnotations(addressSpace.getMetadata().getAnnotations())
                .endMetadata();

        if (addressSpace.getMetadata().getResourceVersion() != null) {
            builder.editOrNewMetadata()
                    .withResourceVersion(addressSpace.getMetadata().getResourceVersion())
                    .endMetadata();
        }

        try {
            // Reset resource version to avoid unneeded extra writes
            AddressSpace newAddressSpace = new AddressSpaceBuilder(addressSpace).editOrNewMetadata().withResourceVersion(null).endMetadata().build();
            builder.addToData("config.json", mapper.writeValueAsString(newAddressSpace));
            return builder.build();
        } catch (IOException e) {
            log.info("Error serializing addressspace for {}", addressSpace, e);
            throw new UncheckedIOException(e);
        }
    }

    @Override
    public boolean deleteAddressSpace(AddressSpace addressSpace) {
        String name = getConfigMapName(addressSpace.getMetadata().getNamespace(), addressSpace.getMetadata().getName());
        Boolean deleted = client.configMaps().withName(name).delete();
        return deleted != null && deleted;
    }

    @Override
    public AddressSpaceList listAddressSpaces(String namespace, Map<String, String> labels) {
        Map<String, String> copy = new LinkedHashMap<>(labels == null ? Collections.emptyMap() : labels);
        copy.put(LabelKeys.TYPE, "address-space");
        copy.put(LabelKeys.NAMESPACE, namespace);
        return listAddressSpacesMatching(copy);
    }

    @Override
    public Set<AddressSpace> listAddressSpaces(String namespace) {
        return new LinkedHashSet<>(listAddressSpaces(namespace, null).getItems());
    }

    @Override
    public Set<AddressSpace> listAddressSpacesWithLabels(String namespace, Map<String, String> labels) {
        return new LinkedHashSet<>(listAddressSpaces(namespace, labels).getItems());
    }

    @Override
    public AddressSpaceList listAllAddressSpaces(Map<String, String> labels) {
        Map<String, String> copy = new LinkedHashMap<>(labels == null ? Collections.emptyMap() : labels);
        copy.put(LabelKeys.TYPE, "address-space");
        return listAddressSpacesMatching(copy);
    }

    @Override
    public Set<AddressSpace> listAllAddressSpaces() {
        return new LinkedHashSet<>(listAddressSpacesMatching(null).getItems());
    }

    @Override
    public Set<AddressSpace> listAllAddressSpacesWithLabels(Map<String, String> labels) {
        return new LinkedHashSet<>(listAddressSpacesMatching(labels).getItems());
    }

    private AddressSpaceList listAddressSpacesMatching(Map<String, String> labels) {
        Set<AddressSpace> instances = new LinkedHashSet<>();
        ConfigMapList l = client.configMaps().withLabels(labels).list();
        for (ConfigMap map : l.getItems()) {
            instances.add(getAddressSpaceFromConfig(map));
        }
        AddressSpaceList addressSpaceList = new AddressSpaceList(instances);
        ListMeta metadata = addressSpaceList.getMetadata();
        if (l.getMetadata() != null) {
            metadata.setResourceVersion(l.getMetadata().getResourceVersion());
        }
        return addressSpaceList;
    }

    @Override
    public void deleteAddressSpaces(String namespace) {
        Map<String, String> labels = new LinkedHashMap<>();
        labels.put(LabelKeys.TYPE, "address-space");
        labels.put(LabelKeys.NAMESPACE, namespace);
        client.configMaps().withLabels(labels).delete();
    }

    private AddressSpace getAddressSpaceFromConfig(ConfigMap map) {
        try {
            AddressSpace addressSpace = mapper.readValue(map.getData().get("config.json"), AddressSpace.class);
            AddressSpaceBuilder builder = new AddressSpaceBuilder(addressSpace);
            MetadataNested<AddressSpaceBuilder> metadataBuilder = builder.editOrNewMetadata();

            if (addressSpace.getMetadata().getUid() == null) {
                metadataBuilder.withUid(map.getMetadata().getUid());
            }

            metadataBuilder.withResourceVersion(map.getMetadata().getResourceVersion());

            if (addressSpace.getMetadata().getCreationTimestamp() == null) {
                metadataBuilder.withCreationTimestamp(map.getMetadata().getCreationTimestamp());
            }

            if (addressSpace.getMetadata().getSelfLink() == null) {
                metadataBuilder.withSelfLink("/apis/enmasse.io/v1beta1/namespaces/" + addressSpace.getMetadata().getNamespace() + "/addressspaces/" + addressSpace.getMetadata().getName());
            }

            // commit changes
            metadataBuilder.endMetadata();

            return builder.build();
        } catch (IOException e) {
            log.error("Error decoding address space from configmap : {}", map, e);
            throw new UncheckedIOException(e);
        }
    }

    @Override
    public Watch watchAddressSpaces(CacheWatcher<AddressSpace> watcher, Duration resyncInterval) {
        Reflector.Config<ConfigMap, ConfigMapList> config = new Reflector.Config<>();
        watcher.onInit(() -> cache.list().stream()
                .map(ConfigMapAddressSpaceApi.this::getAddressSpaceFromConfig)
                .collect(Collectors.toList()));
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
    public Closeable watch(Watcher<AddressSpace> watcher, String namespace, String resourceVersion, Map<String, String> userLabels) {
        Map<String, String> labels = new LinkedHashMap<>();
        if (userLabels != null) {
            labels.putAll(userLabels);
        }
        labels.put(LabelKeys.TYPE, "address-space");
        if (namespace != null) {
            labels.put(LabelKeys.NAMESPACE, namespace);
        }

        return client.configMaps()
                .withLabels(labels)
                .withResourceVersion(resourceVersion)
                .watch(new Watcher<>() {
                           @Override
                           public void eventReceived(Action action, ConfigMap resource) {
                               watcher.eventReceived(action, getAddressSpaceFromConfig(resource));
                           }

                           @Override
                           public void onClose(KubernetesClientException cause) {
                               watcher.onClose(cause);
                           }
                       }
                );

    }

    @Override
    public AddressApi withAddressSpace(AddressSpace addressSpace) {
        return new ConfigMapAddressApi(client, addressSpace.getAnnotation(AnnotationKeys.INFRA_UUID));
    }

    @SuppressWarnings("unused")
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
