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
import io.enmasse.address.model.AddressResolver;
import io.enmasse.config.LabelKeys;
import io.enmasse.address.model.AddressSpace;
import io.enmasse.address.model.v1.CodecV1;
import io.fabric8.kubernetes.api.model.ConfigMap;
import io.fabric8.kubernetes.api.model.ConfigMapList;
import io.fabric8.kubernetes.api.model.DoneableConfigMap;
import io.fabric8.openshift.client.OpenShiftClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * Implementation of the AddressSpace API towards Kubernetes
 */
public class ConfigMapAddressSpaceApi implements AddressSpaceApi {
    protected final Logger log = LoggerFactory.getLogger(getClass().getName());
    private final OpenShiftClient client;
    // TODO: Parameterize
    private static final ObjectMapper mapper = CodecV1.getMapper();

    public ConfigMapAddressSpaceApi(OpenShiftClient client) {
        this.client = client;
    }

    @Override
    public Optional<AddressSpace> getAddressSpaceWithName(String name) {
        ConfigMap map = client.configMaps().withName(KubeUtil.sanitizeName("address-space-" + name)).get();
        if (map == null) {
            return Optional.empty();
        } else {
            return Optional.of(getAddressSpaceFromConfig(map));
        }
    }

    @Override
    public void createAddressSpace(AddressSpace addressSpace) {
        create(client.configMaps().createNew(), addressSpace);
    }

    @Override
    public void replaceAddressSpace(AddressSpace addressSpace) {
        String name = KubeUtil.sanitizeName("address-space-" + addressSpace.getName());
        ConfigMap previous = client.configMaps().withName(name).get();
        if (previous == null) {
            return;
        }
        createOrReplace(addressSpace);
    }

    private void create(DoneableConfigMap config, AddressSpace addressSpace) {
        String name = KubeUtil.sanitizeName("address-space-" + addressSpace.getName());
        try {
            config.withNewMetadata()
                .withName(name)
                .addToLabels(LabelKeys.TYPE, "address-space")
                .endMetadata()
                .addToData("config.json", mapper.writeValueAsString(addressSpace))
                .done();
        } catch (Exception e) {
            log.error("Error createReplace on " + addressSpace);
        }
    }

    public void createOrReplace(AddressSpace addressSpace) {
        create(client.configMaps().createOrReplaceWithNew(), addressSpace);
    }

    @Override
    public void deleteAddressSpace(AddressSpace addressSpace) {
        String name = KubeUtil.sanitizeName("address-space-" + addressSpace.getName());
        client.configMaps().withName(name).delete();
    }

    @Override
    public Set<AddressSpace> listAddressSpaces() {
        Set<AddressSpace> instances = new LinkedHashSet<>();
        ConfigMapList list = client.configMaps().withLabel(LabelKeys.TYPE, "address-space").list();
        for (ConfigMap map : list.getItems()) {
            instances.add(getAddressSpaceFromConfig(map));
        }
        return instances;
    }

    @Override
    public AddressSpace getAddressSpaceFromConfig(ConfigMap map) {
        try {
            return mapper.readValue(map.getData().get("config.json"), AddressSpace.class);
        } catch (Exception e) {
            log.error("Error decoding address space", e);
            throw new RuntimeException(e);
        }
    }

    @Override
    public Watch watchAddressSpaces(Watcher<AddressSpace> watcher) throws Exception {
        Map<String, String> labels = new LinkedHashMap<>();
        labels.put(LabelKeys.TYPE, "address-space");
        ResourceController<AddressSpace> controller = ResourceController.create(new Resource<AddressSpace>() {
            @Override
            public io.fabric8.kubernetes.client.Watch watchResources(io.fabric8.kubernetes.client.Watcher w) {
                return client.configMaps().withLabels(labels).watch(w);
            }

            @Override
            public Set<AddressSpace> listResources() {
                return listAddressSpaces();
            }
        }, watcher);

        controller.start();
        return controller::stop;
    }

    @Override
    public AddressApi withAddressSpace(AddressSpace addressSpace) {
        return new ConfigMapAddressApi(client, new AddressResolver(addressSpace.getType()), addressSpace.getNamespace());
    }
}
