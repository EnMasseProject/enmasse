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
package io.enmasse.controller.standard;

import io.enmasse.config.AnnotationKeys;
import io.enmasse.config.LabelKeys;
import io.enmasse.k8s.api.AddressApi;
import io.enmasse.k8s.api.ConfigMapAddressApi;
import io.enmasse.k8s.api.EventLogger;
import io.enmasse.k8s.api.KubeEventLogger;
import io.fabric8.kubernetes.api.model.*;
import io.fabric8.kubernetes.client.internal.readiness.Readiness;
import io.fabric8.openshift.client.OpenShiftClient;
import io.fabric8.openshift.client.ParameterValue;

import java.io.File;
import java.time.Clock;
import java.util.*;
import java.util.stream.Collectors;

public class KubernetesHelper implements Kubernetes {
    private final File templateDir;
    private static final String TEMPLATE_SUFFIX = ".json";
    private final OpenShiftClient client;

    public KubernetesHelper(OpenShiftClient client, File templateDir) {
        this.client = client;
        this.templateDir = templateDir;
    }

    @Override
    public List<AddressCluster> listClusters() {
        Map<String, List<HasMetadata>> resourceMap = new HashMap<>();

        // Add other resources part of a destination cluster
        List<HasMetadata> objects = new ArrayList<>();
        objects.addAll(client.extensions().deployments().list().getItems());
        objects.addAll(client.apps().statefulSets().list().getItems());
        objects.addAll(client.persistentVolumeClaims().list().getItems());
        objects.addAll(client.configMaps().list().getItems());
        objects.addAll(client.services().list().getItems());

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
    public boolean isDestinationClusterReady(String clusterId) {
        return listClusters().stream()
                .filter(dc -> clusterId.equals(dc.getClusterId()))
                .anyMatch(KubernetesHelper::areAllDeploymentsReady);
    }

    private static boolean areAllDeploymentsReady(AddressCluster dc) {
        return dc.getResources().getItems().stream().filter(KubernetesHelper::isDeployment).allMatch(Readiness::isReady);
    }

    public static boolean isDeployment(HasMetadata res) {
        return res.getKind().equals("Deployment") || res.getKind().equals("StatefulSet");  // TODO: is there an existing constant for this somewhere?
    }

    @Override
    public List<Pod> listRouters() {
        return client.pods().withLabel(LabelKeys.CAPABILITY, "router").list().getItems();
    }

    @Override
    public void create(KubernetesList resources) {
        client.lists().create(resources);
    }

    @Override
    public void delete(KubernetesList resources) {
        client.lists().delete(resources);
    }

    @Override
    public EventLogger createEventLogger(Clock clock, String componentName) {
        return new KubeEventLogger(client, client.getNamespace(), clock, componentName);
    }

    @Override
    public AddressApi createAddressApi() {
        return new ConfigMapAddressApi(client, client.getNamespace());
    }

    @Override
    public List<EndpointAddress> listBrokers(String clusterId) {
        for (Service service : client.services().list().getItems()) {
            if (service.getMetadata().getAnnotations() != null &&
                    clusterId.equals(service.getMetadata().getAnnotations().get(AnnotationKeys.CLUSTER_ID))) {
                Endpoints endpoints = client.endpoints().withName(service.getMetadata().getName()).get();
                if (endpoints != null) {
                    List<EndpointAddress> addresses = new ArrayList<>();
                    List<EndpointSubset> subsets = endpoints.getSubsets();
                    for (EndpointSubset subset : subsets) {
                        addresses.addAll(subset.getAddresses());
                    }
                    return addresses;
                }
            }
        }
        return Collections.emptyList();
    }

    @Override
    public KubernetesList processTemplate(String templateName, ParameterValue... parameterValues) {
        if (templateDir != null) {
            File templateFile = new File(templateDir, templateName + TEMPLATE_SUFFIX);
            return client.templates().load(templateFile).processLocally(parameterValues);
        } else {
            return client.templates().withName(templateName).process(parameterValues);
        }
    }
}
