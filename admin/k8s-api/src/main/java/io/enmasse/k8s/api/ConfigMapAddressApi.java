/*
 * Copyright 2017 Red Hat Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.enmasse.k8s.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.enmasse.config.LabelKeys;
import io.enmasse.config.AnnotationKeys;
import io.enmasse.address.model.Address;
import io.enmasse.address.model.v1.CodecV1;
import io.fabric8.kubernetes.api.model.ConfigMap;
import io.fabric8.kubernetes.api.model.ConfigMapList;
import io.fabric8.kubernetes.api.model.DoneableConfigMap;
import io.fabric8.openshift.client.OpenShiftClient;
import io.vertx.core.Vertx;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 * Implements the AddressApi using config maps.
 */
public class ConfigMapAddressApi implements AddressApi {

    private static final Logger log = LoggerFactory.getLogger(ConfigMapAddressApi.class);
    private final Vertx vertx;
    private final OpenShiftClient client;
    private final String namespace;

    // TODO: Parameterize
    private static final ObjectMapper mapper = CodecV1.getMapper();

    public ConfigMapAddressApi(Vertx vertx, OpenShiftClient client, String namespace) {
        this.vertx = vertx;
        this.client = client;
        this.namespace = namespace;
    }

    @Override
    public Optional<Address> getAddressWithName(String name) {
        ConfigMap map = client.configMaps().inNamespace(namespace).withName(KubeUtil.sanitizeName("address-config-" + name)).get();
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
            return mapper.readValue(data.get("config.json"), Address.class);
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
    public void createAddress(Address destination) {
        createOrReplace(destination);
    }

    @Override
    public void replaceAddress(Address address) {
        String name = KubeUtil.sanitizeName("address-config-" + address.getName());
        ConfigMap previous = client.configMaps().inNamespace(namespace).withName(name).get();
        if (previous == null) {
            return;
        }
        createOrReplace(address);
    }

    private void createOrReplace(Address address) {
        String name = KubeUtil.sanitizeName("address-config-" + address.getName());
        DoneableConfigMap builder = client.configMaps().inNamespace(namespace).withName(name).createOrReplaceWithNew()
                .withNewMetadata()
                .withName(name)
                .addToLabels(LabelKeys.TYPE, "address-config")
                // TODO: Support other ways of doing this
                .addToAnnotations(AnnotationKeys.CLUSTER_ID, name)
                .addToAnnotations(AnnotationKeys.ADDRESS_SPACE, address.getAddressSpace())
                .endMetadata();

        try {
            builder.addToData("config.json", mapper.writeValueAsString(address));
            builder.done();
        } catch (Exception e) {
            log.info("Error serializing address for {}", address, e);
        }
    }

    @Override
    public void deleteAddress(Address address) {
        String name = KubeUtil.sanitizeName("address-config-" + address.getName());
        client.configMaps().inNamespace(namespace).withName(name).delete();
    }

    @Override
    public Watch watchAddresses(Watcher<Address> watcher) throws Exception {
        Map<String, String> labels = new LinkedHashMap<>();
        labels.put(LabelKeys.TYPE, "address-config");
        WatcherVerticle<Address> verticle = new WatcherVerticle<>(new Resource<Address>() {
            @Override
            public io.fabric8.kubernetes.client.Watch watchResources(io.fabric8.kubernetes.client.Watcher w) {
                return client.configMaps().inNamespace(namespace).withLabels(labels).watch(w);
            }

            @Override
            public Set<Address> listResources() {
                return listAddresses();
            }
        }, watcher);

        CompletableFuture<String> promise = new CompletableFuture<>();
        vertx.deployVerticle(verticle, result -> {
            if (result.succeeded()) {
                promise.complete(result.result());
            } else {
                log.error("Error deploying verticle: {}", result.cause());
                promise.completeExceptionally(result.cause());
            }
        });

        String id = promise.get(1, TimeUnit.MINUTES);
        return () -> vertx.undeploy(id);
    }
}
