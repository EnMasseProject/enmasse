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
import io.enmasse.address.model.AuthenticationServiceResolver;
import io.enmasse.address.model.AuthenticationServiceType;
import io.enmasse.address.model.Endpoint;
import io.fabric8.kubernetes.api.model.*;
import io.fabric8.kubernetes.api.model.extensions.Deployment;
import io.fabric8.kubernetes.api.model.extensions.DoneableIngress;
import io.fabric8.kubernetes.client.dsl.Resource;
import io.fabric8.openshift.api.model.DoneablePolicyBinding;
import io.fabric8.openshift.api.model.DoneableRoute;
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
    private final String namespace;
    private final Optional<File> templateDir;

    public KubernetesHelper(String namespace, OpenShiftClient client, Optional<File> templateDir) {
        this.client = client;
        this.namespace = namespace;
        this.templateDir = templateDir;
    }

    @Override
    public List<AddressCluster> listClusters() {
        Map<String, List<HasMetadata>> resourceMap = new HashMap<>();

        // Add other resources part of a destination cluster
        List<HasMetadata> objects = new ArrayList<>();
        objects.addAll(client.extensions().deployments().inNamespace(namespace).list().getItems());
        objects.addAll(client.persistentVolumeClaims().inNamespace(namespace).list().getItems());
        objects.addAll(client.configMaps().inNamespace(namespace).list().getItems());
        objects.addAll(client.replicationControllers().inNamespace(namespace).list().getItems());

        for (HasMetadata config : objects) {
            Map<String, String> annotations = config.getMetadata().getAnnotations();

            if (annotations != null && annotations.containsKey(AnnotationKeys.CLUSTER_ID)) {
                String groupId = annotations.get(AnnotationKeys.CLUSTER_ID);

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
                    return new AddressCluster(entry.getKey(), list);
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
        client.lists().inNamespace(namespace).create(resources);
    }

    @Override
    public void create(KubernetesList resources, String namespace) {
        client.lists().inNamespace(namespace).create(resources);
    }

    @Override
    public String getNamespace() {
        return namespace;
    }

    @Override
    public void delete(KubernetesList resources) {
        client.lists().inNamespace(namespace).delete(resources);
    }

    @Override
    public void delete(HasMetadata... resources) {
        delete(new KubernetesListBuilder()
                .addToItems(resources)
                .build());
    }

    @Override
    public Namespace createNamespace(String name, String namespace) {
        return client.namespaces().createNew()
                .editOrNewMetadata()
                    .withName(namespace)
                    .addToLabels("app", "enmasse")
                    .addToLabels(LabelKeys.TYPE, "address-space")
                    .addToAnnotations(AnnotationKeys.ADDRESS_SPACE, name)
                .endMetadata()
                .done();
    }

    @Override
    public Kubernetes withNamespace(String namespace) {
        return new KubernetesHelper(namespace, client, templateDir);
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
    public void addDefaultViewPolicy(String namespace) {
        if (client.isAdaptable(OpenShiftClient.class)) {
            Resource<PolicyBinding, DoneablePolicyBinding> bindingResource = client.policyBindings()
                    .inNamespace(namespace)
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
                    .withNamespace(namespace)
                    .endMetadata()
                    .addToUserNames("system:serviceaccount:" + namespace + ":default")
                    .addNewSubject()
                    .withName("default")
                    .withNamespace(namespace)
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
        return client.services().inNamespace(namespace).withName(service).get() != null;
    }

    @Override
    public void createSecretWithDefaultPermissions(String secretName, String namespace) {
        Secret secret = client.secrets().inNamespace(namespace).withName(secretName).get();
        if (secret != null) {
            // Skip if it is already created
            return;
        }
        // TODO: Add labels
        secret = client.secrets().inNamespace(namespace).createNew()
                .editOrNewMetadata()
                .withName(secretName)
                .endMetadata()
                .done();
        client.serviceAccounts().inNamespace(namespace).withName("default").edit()
                .addToSecrets(new ObjectReferenceBuilder()
                        .withKind(secret.getKind())
                        .withName(secret.getMetadata().getName())
                        .withApiVersion(secret.getApiVersion())
                        .build())
                .done();
    }

    @Override
    public void createEndpoint(Endpoint endpoint, Map<String, String> servicePortMap, String addressSpaceName, String namespace) {
        // TODO: Add labels
        if (client.isAdaptable(OpenShiftClient.class)) {
            DoneableRoute route = client.routes().inNamespace(namespace).createNew()
                    .editOrNewMetadata()
                    .withName(endpoint.getName())
                    .addToAnnotations(AnnotationKeys.ADDRESS_SPACE, addressSpaceName)
                    .endMetadata()
                    .editOrNewSpec()
                    .withHost(endpoint.getHost().orElse(""))
                    .withNewTo()
                    .withName(endpoint.getService())
                    .withKind("Service")
                    .endTo()
                    .withNewPort()
                    .editOrNewTargetPort()
                    .withStrVal(servicePortMap.get(endpoint.getService()))
                    .endTargetPort()
                    .endPort()
                    .endSpec();

            if (endpoint.getCertProvider().isPresent()) {
                route.editOrNewMetadata()
                        .addToAnnotations(AnnotationKeys.CERT_SECRET_NAME, endpoint.getCertProvider().get().getSecretName())
                        .endMetadata()
                        .editOrNewSpec()
                        .withNewTls()
                        .withTermination("passthrough")
                        .endTls()
                        .endSpec();
            }
            route.done();
        } else {
            DoneableIngress ingress = client.extensions().ingresses().inNamespace(namespace).createNew()
                    .editOrNewMetadata()
                    .withName(endpoint.getName())
                    .addToAnnotations(AnnotationKeys.ADDRESS_SPACE, addressSpaceName)
                    .endMetadata()
                    .editOrNewSpec()
                    .editOrNewBackend()
                    .withServiceName(endpoint.getService())
                    .withServicePort(new IntOrString(servicePortMap.get(endpoint.getService())))
                    .endBackend()
                    .endSpec();

            if (endpoint.getCertProvider().isPresent()) {
                ingress.editOrNewMetadata()
                        .addToAnnotations(AnnotationKeys.CERT_SECRET_NAME, endpoint.getCertProvider().get().getSecretName())
                        .endMetadata()
                        .editOrNewSpec();

                if (endpoint.getHost().isPresent()) {
                    ingress.editOrNewSpec()
                            .addNewTl()
                            .addToHosts(endpoint.getHost().get())
                            .withSecretName(endpoint.getCertProvider().get().getSecretName())
                            .endTl();
                }
            }
            ingress.done();
        }
    }

    public Set<Deployment> getReadyDeployments() {
        return client.extensions().deployments().inNamespace(namespace).list().getItems().stream()
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

    @Override
    public List<Pod> listRouters() {
        return client.pods().withLabel(LabelKeys.CAPABILITY, "router").list().getItems();
    }

    public static boolean isDeployment(HasMetadata res) {
        return res.getKind().equals("Deployment");  // TODO: is there an existing constant for this somewhere?
    }

    private static boolean areAllDeploymentsReady(AddressCluster dc) {
        return dc.getResources().getItems().stream().filter(KubernetesHelper::isDeployment).allMatch(d -> isReady((Deployment) d));
    }

    private static boolean isReady(Deployment deployment) {
        Integer unavailableReplicas = deployment.getStatus().getUnavailableReplicas();
        return unavailableReplicas == null || unavailableReplicas == 0;
    }
}
