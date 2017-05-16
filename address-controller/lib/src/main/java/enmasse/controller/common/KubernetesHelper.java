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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import enmasse.config.AddressConfigKeys;
import enmasse.config.LabelKeys;
import enmasse.controller.address.DestinationCluster;
import enmasse.controller.model.Destination;
import enmasse.controller.model.Instance;
import enmasse.controller.model.InstanceId;
import io.fabric8.kubernetes.api.model.*;
import io.fabric8.kubernetes.client.dsl.Resource;
import io.fabric8.openshift.api.model.DoneablePolicyBinding;
import io.fabric8.openshift.api.model.PolicyBinding;
import io.fabric8.openshift.client.OpenShiftClient;
import io.fabric8.openshift.client.ParameterValue;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Wraps the Kubernetes client and adds some helper methods.
 */
public class KubernetesHelper implements Kubernetes {
    private static final Logger log = LoggerFactory.getLogger(KubernetesHelper.class.getName());
    private static final String TEMPLATE_SUFFIX = ".json";
    private static final ObjectMapper mapper = new ObjectMapper();

    private final OpenShiftClient client;
    private final InstanceId instance;
    private final File templateDir;

    public KubernetesHelper(InstanceId instance, OpenShiftClient client, File templateDir) {
        this.client = client;
        this.instance = instance;
        this.templateDir = templateDir;
    }

    @Override
    public List<DestinationCluster> listClusters() {
        Map<String, List<HasMetadata>> resourceMap = new HashMap<>();
        Map<String, Set<Destination>> groupMap = new HashMap<>();

        // Add other resources part of a destination cluster
        List<HasMetadata> objects = new ArrayList<>();
        objects.addAll(client.extensions().deployments().inNamespace(instance.getNamespace()).list().getItems());
        objects.addAll(client.persistentVolumeClaims().inNamespace(instance.getNamespace()).list().getItems());
        objects.addAll(client.configMaps().inNamespace(instance.getNamespace()).list().getItems());
        objects.addAll(client.replicationControllers().inNamespace(instance.getNamespace()).list().getItems());

        for (HasMetadata config : objects) {
            Map<String, String> labels = config.getMetadata().getLabels();

            if (labels != null && labels.containsKey(LabelKeys.GROUP_ID)) {
                String groupId = labels.get(LabelKeys.GROUP_ID);
                if (!resourceMap.containsKey(groupId)) {
                    resourceMap.put(groupId, new ArrayList<>());
                }

                if (!groupMap.containsKey(groupId)) {
                    groupMap.put(groupId, new LinkedHashSet<>());
                }

                resourceMap.get(groupId).add(config);

                // Add the destinations for a particular address config
                if ("address-config".equals(labels.get(LabelKeys.TYPE))) {
                    ConfigMap configMap = (ConfigMap) config;
                    Map<String, String> data = configMap.getData();

                    Destination.Builder destBuilder = new Destination.Builder(data.get(AddressConfigKeys.ADDRESS), data.get(AddressConfigKeys.GROUP_ID));
                    destBuilder.storeAndForward(Boolean.parseBoolean(data.get(AddressConfigKeys.STORE_AND_FORWARD)));
                    destBuilder.multicast(Boolean.parseBoolean(data.get(AddressConfigKeys.MULTICAST)));
                    destBuilder.flavor(Optional.ofNullable(data.get(AddressConfigKeys.FLAVOR)));
                    destBuilder.uuid(Optional.ofNullable(data.get(AddressConfigKeys.UUID)));

                    groupMap.get(groupId).add(destBuilder.build());
                }
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
    public void create(HasMetadata... resources) {
        create(new KubernetesListBuilder()
                .addToItems(resources)
                .build());
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
    public void deleteAddressConfig(Destination destination) {
        String name = Kubernetes.sanitizeName("address-config-" + destination.address());
        client.configMaps().inNamespace(instance.getNamespace()).withName(name).delete();
    }

    @Override
    public ConfigMap getInstanceConfig(InstanceId instanceId) {
        return client.configMaps().withName(Kubernetes.sanitizeName("instance-config-" + instanceId.getId())).get();
    }

    @Override
    public ConfigMap createInstanceConfig(Instance instance) {
        String name = Kubernetes.sanitizeName("instance-config-" + instance.id().getId());
        ConfigMapBuilder builder = new ConfigMapBuilder()
                .withNewMetadata()
                .withName(name)
                .addToLabels(LabelKeys.TYPE, "instance-config")
                .endMetadata();
        try {
            builder.addToData("config.json", mapper.writeValueAsString(new enmasse.controller.api.v3.Instance(instance)));
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
        return builder.build();
    }

    @Override
    public void deleteInstanceConfig(Instance instance) {
        String name = Kubernetes.sanitizeName("instance-config-" + instance.id().getId());
        client.configMaps().withName(name).delete();
    }

    @Override
    public ConfigMap createAddressConfig(Destination destination) {
        String name = Kubernetes.sanitizeName("address-config-" + destination.address());
        ConfigMapBuilder builder = new ConfigMapBuilder()
                .withNewMetadata()
                .withName(name)
                .addToLabels(LabelKeys.GROUP_ID, Kubernetes.sanitizeName(destination.group()))
                .addToLabels(LabelKeys.TYPE, "address-config")
                .addToLabels(LabelKeys.INSTANCE, Kubernetes.sanitizeName(instance.getId()))
                .endMetadata();

        builder.addToData(AddressConfigKeys.ADDRESS, destination.address());
        builder.addToData(AddressConfigKeys.GROUP_ID, destination.group());
        builder.addToData(AddressConfigKeys.STORE_AND_FORWARD, String.valueOf(destination.storeAndForward()));
        builder.addToData(AddressConfigKeys.MULTICAST, String.valueOf(destination.multicast()));
        destination.flavor().ifPresent(f -> builder.addToData(AddressConfigKeys.FLAVOR, f));
        destination.uuid().ifPresent(f -> builder.addToData(AddressConfigKeys.UUID, f));
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
    public Kubernetes mutateClient(InstanceId newInstance) {
        return new KubernetesHelper(newInstance, client, templateDir);
    }

    @Override
    public KubernetesList processTemplate(String templateName, ParameterValue... parameterValues) {
        File templateFile = new File(templateDir, templateName + TEMPLATE_SUFFIX);
        return client.templates().load(templateFile).processLocally(parameterValues);
    }

    @Override
    public void addDefaultViewPolicy(InstanceId instance) {
        if (client.isAdaptable(OpenShiftClient.class)) {
            Resource<PolicyBinding, DoneablePolicyBinding> bindingResource = client.policyBindings()
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
        } else {
            // TODO: Add support for Kubernetes RBAC policies
            log.info("No support for Kubernetes RBAC policies yet, won't add any default view policy");
        }
    }


    @Override
    public List<Namespace> listNamespaces(Map<String, String> labelMap) {
        return client.namespaces().withLabels(labelMap).list().getItems();
    }

    @Override
    public List<Route> getRoutes(InstanceId instanceId) {
        if (client.isAdaptable(OpenShiftClient.class)) {
            return client.routes().inNamespace(instanceId.getNamespace()).list().getItems().stream()
                    .map(r -> new Route() {
                        @Override
                        public String getName() {
                            return r.getMetadata().getName();
                        }

                        @Override
                        public String getHostName() {
                            return r.getSpec().getHost();
                        }
                    }).collect(Collectors.toList());
        } else {
            return client.extensions().ingresses().inNamespace(instanceId.getNamespace()).list().getItems().stream()
                    .map(i -> new Route() {
                        @Override
                        public String getName() {
                            return i.getMetadata().getName();
                        }

                        @Override
                        public String getHostName() {
                            return i.getSpec().getRules().get(0).getHost();
                        }
                    }).collect(Collectors.toList());
        }
    }

    @Override
    public void deleteNamespace(String namespace) {
        client.namespaces().withName(namespace).delete();
    }

    @Override
    public boolean hasService(String service) {
        return client.services().withName(service).get() != null;
    }

    @Override
    public String createInstanceSecret(InstanceId instanceId) throws IOException {
        String secretName = instanceId.getId() + "-certs";
        Secret secret = client.secrets().inNamespace(instanceId.getNamespace()).createNew()
                .editOrNewMetadata()
                .withName(secretName)
                .endMetadata()
                .done();
        client.serviceAccounts().inNamespace(instanceId.getNamespace()).withName("default").edit()
                .addToSecrets(new ObjectReferenceBuilder()
                        .withKind(secret.getKind())
                        .withName(secret.getMetadata().getName())
                        .withApiVersion(secret.getApiVersion())
                        .build())
                .done();
        return secretName;
    }
}
