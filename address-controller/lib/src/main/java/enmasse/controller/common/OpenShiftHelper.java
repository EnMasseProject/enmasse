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

package enmasse.controller.common;

import enmasse.controller.model.Destination;
import enmasse.controller.model.InstanceId;
import enmasse.controller.address.DestinationCluster;
import enmasse.config.AddressDecoder;
import enmasse.config.AddressEncoder;
import enmasse.config.LabelKeys;
import io.fabric8.kubernetes.api.model.*;
import io.fabric8.kubernetes.client.dsl.ClientResource;
import io.fabric8.openshift.api.model.DoneablePolicyBinding;
import io.fabric8.openshift.api.model.PolicyBinding;
import io.fabric8.openshift.api.model.Route;
import io.fabric8.openshift.client.OpenShiftClient;
import io.fabric8.openshift.client.ParameterValue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Wraps the OpenShift client and adds some helper methods.
 */
public class OpenShiftHelper implements OpenShift {
    private static final Logger log = LoggerFactory.getLogger(OpenShiftHelper.class.getName());
    private final OpenShiftClient client;
    private final InstanceId instance;

    public OpenShiftHelper(InstanceId instance, OpenShiftClient client) {
        this.client = client;
        this.instance = instance;
    }

    @Override
    public List<DestinationCluster> listClusters() {
        Map<String, List<HasMetadata>> resourceMap = new HashMap<>();
        Map<String, Set<Destination>> groupMap = new HashMap<>();

        // Add other resources part of a destination cluster
        List<HasMetadata> objects = new ArrayList<>();
        objects.addAll(client.deploymentConfigs().inNamespace(instance.getNamespace()).list().getItems());
        objects.addAll(client.extensions().deployments().inNamespace(instance.getNamespace()).list().getItems());
        objects.addAll(client.persistentVolumeClaims().inNamespace(instance.getNamespace()).list().getItems());
        objects.addAll(client.configMaps().inNamespace(instance.getNamespace()).list().getItems());
        objects.addAll(client.replicationControllers().inNamespace(instance.getNamespace()).list().getItems());

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
                    Map<String, String> addressConfigMap = client.configMaps().inNamespace(instance.getNamespace()).withName(addressConfig).get().getData();

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

                    resourceMap.put(groupId, new ArrayList<>());
                    groupMap.put(groupId, destinations);
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

    @Override
    public void create(KubernetesList resources) {
        client.lists().inNamespace(instance.getNamespace()).create(resources);
    }

    @Override
    public InstanceId getInstanceId() {
        return instance;
    }

    @Override
    public void delete(KubernetesList resources) {
        client.lists().inNamespace(instance.getNamespace()).delete(resources);
    }

    @Override
    public void updateDestinations(Set<Destination> destinations) {
        client.configMaps().inNamespace(instance.getNamespace()).createOrReplace(createAddressConfig(destinations));
    }

    @Override
    public ConfigMap createAddressConfig(Set<Destination> destinations) {
        String groupId = destinations.iterator().next().group();
        String name = OpenShift.sanitizeName("address-config-" + instance.getId() + "-" + groupId);
        ConfigMapBuilder builder = new ConfigMapBuilder()
                .withNewMetadata()
                .withName(name)
                .addToLabels(LabelKeys.GROUP_ID, OpenShift.sanitizeName(groupId))
                .addToLabels(LabelKeys.ADDRESS_CONFIG, name)
                .addToLabels("type", "address-config")
                .addToLabels("instance", OpenShift.sanitizeName(instance.getId()))
                .endMetadata();

        for (Destination destination : destinations) {
            AddressEncoder encoder = new AddressEncoder();
            encoder.encode(destination.storeAndForward(), destination.multicast(), destination.flavor(), destination.uuid());
            builder.addToData(destination.address(), encoder.toJson());
        }
        return builder.build();
    }

    @Override
    public Namespace createNamespace(InstanceId instance) {
        return client.namespaces().createNew()
                .editOrNewMetadata()
                    .withName(instance.getNamespace())
                    .addToLabels("app", "enmasse")
                    .addToLabels("instance", instance.getId())
                    .addToLabels("type", "instance")
                .endMetadata()
                .done();
    }

    @Override
    public OpenShift mutateClient(InstanceId newInstance) {
        return new OpenShiftHelper(newInstance, client);
    }

    @Override
    public KubernetesList processTemplate(String templateName, ParameterValue... parameterValues) {
        return client.templates().withName(templateName).process(parameterValues);
    }

    @Override
    public void addDefaultViewPolicy(InstanceId instance) {
        ClientResource<PolicyBinding, DoneablePolicyBinding> bindingResource = client.policyBindings()
                .inNamespace(instance.getNamespace())
                .withName(":default");

        DoneablePolicyBinding binding;
        if (bindingResource.get() == null) {
            binding = bindingResource.createNew();
        } else {
            binding = bindingResource.edit();
        }
        binding.editOrNewMetadata()
                    .withName(":default")
                .endMetadata()
                .editOrNewPolicyRef()
                    .withName("default")
                .endPolicyRef()
                .addNewRoleBinding()
                    .withName("view")
                    .editOrNewRoleBinding()
                        .editOrNewMetadata()
                            .withName("view")
                            .withNamespace(instance.getNamespace())
                        .endMetadata()
                        .addToUserNames("system:serviceaccount:" + instance.getNamespace() + ":default")
                        .addNewSubject()
                            .withName("default")
                            .withNamespace(instance.getNamespace())
                            .withKind("ServiceAccount")
                        .endSubject()
                        .withNewRoleRef()
                            .withName("view")
                        .endRoleRef()
                    .endRoleBinding()
                .endRoleBinding()
                .done();
    }

    @Override
    public List<Namespace> listNamespaces(Map<String, String> labelMap) {
        return client.namespaces().withLabels(labelMap).list().getItems();
    }

    @Override
    public List<Route> getRoutes(InstanceId instanceId) {
        return client.routes().inNamespace(instanceId.getNamespace()).list().getItems();
    }

    @Override
    public void deleteNamespace(String namespace) {
        client.namespaces().withName(namespace).delete();
    }

    @Override
    public boolean hasService(String service) {
        return client.services().withName(service).get() != null;
    }
}
