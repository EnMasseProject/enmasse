/*
 * Copyright 2017-2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.time.Clock;
import java.util.*;
import java.util.stream.Collectors;

public class KubernetesHelper implements Kubernetes {
    private static final Logger log = LoggerFactory.getLogger(KubernetesHelper.class);
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
    public List<String> listBrokers(String clusterId) {
        List<String> addresses = new ArrayList<>();
        for (Pod pod : client.pods().list().getItems()) {
            if (pod.getMetadata().getAnnotations() != null &&
                    clusterId.equals(pod.getMetadata().getAnnotations().get(AnnotationKeys.CLUSTER_ID))) {


                String host = pod.getMetadata().getName();
                log.info("Found endpoints for {}: {}", clusterId, host);
                addresses.add(host);
            }
        }
        return addresses;
    }

    @Override
    public void scaleDeployment(String deploymentName, int numReplicas) {
        client.extensions().deployments().withName(deploymentName).scale(numReplicas);
    }

    @Override
    public void scaleStatefulSet(String setName, int numReplicas) {
        client.apps().statefulSets().withName(setName).scale(numReplicas);
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
