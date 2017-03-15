/*
 * Copyright 2016 Red Hat Inc.
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

package enmasse.address.controller.admin;

import enmasse.address.controller.model.Destination;
import enmasse.address.controller.model.DestinationGroup;
import enmasse.address.controller.model.InstanceId;
import enmasse.address.controller.openshift.DestinationCluster;
import enmasse.config.AddressDecoder;
import enmasse.config.AddressEncoder;
import enmasse.config.LabelKeys;
import io.fabric8.kubernetes.api.model.*;
import io.fabric8.openshift.client.OpenShiftClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Wraps the OpenShift client and adds some helper methods.
 */
public class OpenShiftHelper {
    private static final Logger log = LoggerFactory.getLogger(OpenShiftHelper.class.getName());
    private final OpenShiftClient client;
    private final InstanceId instance;

    public OpenShiftHelper(InstanceId instance, OpenShiftClient client) {
        this.instance = instance;
        this.client = client;
    }

    public static String nameSanitizer(String name) {
        return name.toLowerCase().replaceAll("[^a-z0-9]", "-");
    }

    public List<DestinationCluster> listClusters() {
        Map<String, List<HasMetadata>> resourceMap = new HashMap<>();
        Map<String, DestinationGroup> groupMap = new HashMap<>();

        // Add other resources part of a destination cluster
        List<HasMetadata> objects = new ArrayList<>();
        objects.addAll(client.deploymentConfigs().list().getItems());
        objects.addAll(client.extensions().deployments().list().getItems());
        objects.addAll(client.persistentVolumeClaims().list().getItems());
        objects.addAll(client.configMaps().list().getItems());
        objects.addAll(client.replicationControllers().list().getItems());

        for (HasMetadata config : objects) {
            Map<String, String> labels = config.getMetadata().getLabels();

            if (labels != null && labels.containsKey(LabelKeys.GROUP_ID)) {
                String groupId = labels.get(LabelKeys.GROUP_ID);

                // First time we encounter this group, fetch the address config for it
                if (!resourceMap.containsKey(groupId)) {
                    String addressConfig = labels.get(LabelKeys.ADDRESS_CONFIG);
                    if (addressConfig == null) {
                        log.info("Encounted grouped resource without address config: " + config);
                        continue;
                    }
                    Map<String, String> addressConfigMap = client.configMaps().withName(addressConfig).get().getData();

                    Set<Destination> destinations = new HashSet<>();
                    for (Map.Entry<String, String> entry : addressConfigMap.entrySet()) {
                        Destination.Builder destBuilder = new Destination.Builder(entry.getKey(), groupId);
                        AddressDecoder addressDecoder = new AddressDecoder(entry.getValue());
                        destBuilder.storeAndForward(addressDecoder.storeAndForward());
                        destBuilder.multicast(addressDecoder.multicast());
                        destBuilder.flavor(addressDecoder.flavor());
                        destBuilder.uuid(addressDecoder.uuid());
                        destinations.add(destBuilder.build());
                    }

                    DestinationGroup destinationGroup = new DestinationGroup(groupId, destinations);
                    resourceMap.put(groupId, new ArrayList<>());
                    groupMap.put(groupId, destinationGroup);
                }
                resourceMap.get(groupId).add(config);
            }
        }

        return resourceMap.entrySet().stream()
                .map(entry -> {
                    KubernetesList list = new KubernetesList();
                    list.setItems(entry.getValue());
                    return new DestinationCluster(this, groupMap.get(entry.getKey()), list);
                }).collect(Collectors.toList());
    }

    public void create(KubernetesList resources) {
        client.lists().create(resources);
    }

    public void delete(KubernetesList resources) {
        client.lists().delete(resources);
    }

    public void updateDestinations(DestinationGroup destinationGroup) {
        client.configMaps().createOrReplace(createAddressConfig(destinationGroup));
    }

    public ConfigMap createAddressConfig(DestinationGroup destinationGroup) {
        String name = nameSanitizer("address-config-" + instance.toString() + "-" + destinationGroup.getGroupId());
        ConfigMapBuilder builder = new ConfigMapBuilder()
                .withNewMetadata()
                .withName(name)
                .addToLabels(LabelKeys.GROUP_ID, nameSanitizer(destinationGroup.getGroupId()))
                .addToLabels(LabelKeys.ADDRESS_CONFIG, name)
                .addToLabels("type", "address-config")
                .addToLabels("instance", nameSanitizer(instance.toString()))
                .endMetadata();

        for (Destination destination : destinationGroup.getDestinations()) {
            AddressEncoder encoder = new AddressEncoder();
            encoder.encode(destination.storeAndForward(), destination.multicast(), destination.flavor(), destination.uuid());
            builder.addToData(destination.address(), encoder.toJson());
        }
        return builder.build();
    }
}
