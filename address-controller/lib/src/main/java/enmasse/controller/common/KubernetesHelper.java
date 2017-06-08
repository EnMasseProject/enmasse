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

import enmasse.config.AnnotationKeys;
import enmasse.config.LabelKeys;
import enmasse.controller.address.DestinationCluster;
import enmasse.controller.model.InstanceId;
import io.fabric8.kubernetes.api.model.*;
import io.fabric8.kubernetes.api.model.extensions.Deployment;
import io.fabric8.kubernetes.client.dsl.Resource;
import io.fabric8.openshift.api.model.DoneablePolicyBinding;
import io.fabric8.openshift.api.model.PolicyBinding;
import io.fabric8.openshift.client.OpenShiftClient;
import io.fabric8.openshift.client.ParameterValue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Wraps the Kubernetes client and adds some helper methods.
 */
public class KubernetesHelper implements Kubernetes {
    private static final Logger log = LoggerFactory.getLogger(KubernetesHelper.class.getName());
    private static final String TEMPLATE_SUFFIX = ".json";

    private final OpenShiftClient client;
    private final InstanceId instance;
    private final Optional<File> templateDir;

    public KubernetesHelper(InstanceId instance, OpenShiftClient client, Optional<File> templateDir) {
        this.client = client;
        this.instance = instance;
        this.templateDir = templateDir;
    }

    @Override
    public List<DestinationCluster> listClusters() {
        Map<String, List<HasMetadata>> resourceMap = new HashMap<>();

        // Add other resources part of a destination cluster
        List<HasMetadata> objects = new ArrayList<>();
        objects.addAll(client.extensions().deployments().inNamespace(instance.getNamespace()).list().getItems());
        objects.addAll(client.persistentVolumeClaims().inNamespace(instance.getNamespace()).list().getItems());
        objects.addAll(client.configMaps().inNamespace(instance.getNamespace()).list().getItems());
        objects.addAll(client.replicationControllers().inNamespace(instance.getNamespace()).list().getItems());

        for (HasMetadata config : objects) {
            Map<String, String> annotations = config.getMetadata().getAnnotations();

            if (annotations != null && annotations.containsKey(AnnotationKeys.GROUP_ID)) {
                String groupId = annotations.get(AnnotationKeys.GROUP_ID);

                Map<String, String> labels = config.getMetadata().getLabels();

                if (labels != null && !"address-config".equals(labels.get(LabelKeys.TYPE))) {
                    if (!resourceMap.containsKey(groupId)) {
                        resourceMap.put(groupId, new ArrayList<>());
                    }
                    resourceMap.get(groupId).add(config);
                }
            }
        }

        return resourceMap.entrySet().stream()
                .map(entry -> {
                    KubernetesList list = new KubernetesList();
                    list.setItems(entry.getValue());
                    return new DestinationCluster(entry.getKey(), list);
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
    public void delete(HasMetadata... resources) {
        delete(new KubernetesListBuilder()
                .addToItems(resources)
                .build());
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
    public Kubernetes withInstance(InstanceId newInstance) {
        return new KubernetesHelper(newInstance, client, templateDir);
    }

    @Override
    public KubernetesList processTemplate(String templateName, ParameterValue... parameterValues) {
        if (templateDir.isPresent()) {
            File templateFile = new File(templateDir.get(), templateName + TEMPLATE_SUFFIX);
            return client.templates().load(templateFile).processLocally(parameterValues);
        } else {
            return client.templates().withName(templateName).process(parameterValues);
        }
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
    public void deleteNamespace(String namespace) {
        client.namespaces().withName(namespace).delete();
    }

    @Override
    public boolean hasService(String service) {
        return client.services().withName(service).get() != null;
    }

    @Override
    public void createInstanceSecret(String secretName, InstanceId instanceId) {
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
    }

    public Set<Deployment> getReadyDeployments() {
        return client.extensions().deployments().inNamespace(instance.getNamespace()).list().getItems().stream()
                .filter(KubernetesHelper::isReady)
                .collect(Collectors.toSet());
    }

    @Override
    public boolean isDestinationClusterReady(String clusterId) {
        return listClusters().stream()
                .filter(dc -> clusterId.equals(dc.getClusterId()))
                .anyMatch(KubernetesHelper::areAllDeploymentsReady);
    }

    @Override
    public List<Namespace> listNamespaces(Map<String, String> labels) {
        return client.namespaces().withLabels(labels).list().getItems();
    }

    public static boolean isDeployment(HasMetadata res) {
        return res.getKind().equals("Deployment");  // TODO: is there an existing constant for this somewhere?
    }

    private static boolean areAllDeploymentsReady(DestinationCluster dc) {
        return dc.getResources().getItems().stream().filter(KubernetesHelper::isDeployment).allMatch(d -> isReady((Deployment) d));
    }

    private static boolean isReady(Deployment deployment) {
        Integer unavailableReplicas = deployment.getStatus().getUnavailableReplicas();
        return unavailableReplicas == null || unavailableReplicas == 0;
    }
}
