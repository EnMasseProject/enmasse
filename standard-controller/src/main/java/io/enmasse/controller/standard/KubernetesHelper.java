/*
 * Copyright 2017-2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.controller.standard;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import io.fabric8.kubernetes.client.KubernetesClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.ObjectMapper;

import io.enmasse.admin.model.v1.StandardInfraConfig;
import io.enmasse.config.AnnotationKeys;
import io.enmasse.config.LabelKeys;
import io.enmasse.k8s.util.Templates;
import io.fabric8.kubernetes.api.model.ConfigMap;
import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.KubernetesList;
import io.fabric8.kubernetes.api.model.PersistentVolumeClaim;
import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.api.model.Secret;
import io.fabric8.kubernetes.api.model.Service;
import io.fabric8.kubernetes.api.model.ServiceAccount;
import io.fabric8.kubernetes.api.model.apps.Deployment;
import io.fabric8.kubernetes.api.model.apps.StatefulSet;
import io.fabric8.kubernetes.client.KubernetesClientException;
import io.fabric8.kubernetes.client.internal.readiness.Readiness;

public class KubernetesHelper implements Kubernetes {
    private static final Logger log = LoggerFactory.getLogger(KubernetesHelper.class);
    private static final ObjectMapper mapper = new ObjectMapper();
    private final File templateDir;
    private static final String TEMPLATE_SUFFIX = ".yaml";
    private final KubernetesClient client;
    private final String infraUuid;

    public KubernetesHelper(KubernetesClient client, File templateDir, String infraUuid) {
        this.client = client;
        this.templateDir = templateDir;
        this.infraUuid = infraUuid;
    }

    @Override
    public List<BrokerCluster> listClusters() {
        Map<String, List<HasMetadata>> resourceMap = new HashMap<>();

        // Add other resources part of a destination cluster
        List<HasMetadata> objects = new ArrayList<>();
        objects.addAll(client.apps().deployments().withLabel(LabelKeys.INFRA_UUID, infraUuid).list().getItems());
        objects.addAll(client.apps().statefulSets().withLabel(LabelKeys.INFRA_UUID, infraUuid).list().getItems());
        objects.addAll(client.persistentVolumeClaims().withLabel(LabelKeys.INFRA_UUID, infraUuid).list().getItems());
        objects.addAll(client.configMaps().withLabel(LabelKeys.INFRA_UUID, infraUuid).withLabelNotIn("type", "address-config", "address-space", "address-space-plan", "address-plan").list().getItems());
        objects.addAll(client.services().withLabel(LabelKeys.INFRA_UUID, infraUuid).list().getItems());

        for (HasMetadata config : objects) {
            Map<String, String> annotations = config.getMetadata().getAnnotations();

            if (annotations != null && annotations.containsKey(AnnotationKeys.CLUSTER_ID)) {
                String groupId = annotations.get(AnnotationKeys.CLUSTER_ID);

                Map<String, String> labels = config.getMetadata().getLabels();

                if (labels != null) {
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
                    return new BrokerCluster(entry.getKey(), list);
                }).collect(Collectors.toList());
    }

    @Override
    public RouterCluster getRouterCluster() throws IOException {
        StatefulSet s = client.apps().statefulSets().withName("qdrouterd-" + infraUuid).get();
        StandardInfraConfig infraConfig = null;
        if (s.getMetadata().getAnnotations() != null && s.getMetadata().getAnnotations().get(AnnotationKeys.APPLIED_INFRA_CONFIG) != null) {
            infraConfig = mapper.readValue(s.getMetadata().getAnnotations().get(AnnotationKeys.APPLIED_INFRA_CONFIG), StandardInfraConfig.class);
        }
        return new RouterCluster(s.getMetadata().getName(), s.getSpec().getReplicas(), infraConfig);
    }

    @Override
    public boolean isDestinationClusterReady(String clusterId) {
        return listClusters().stream()
                .filter(dc -> clusterId.equals(dc.getClusterId()))
                .anyMatch(KubernetesHelper::areAllDeploymentsReady);
    }

    private static boolean areAllDeploymentsReady(BrokerCluster dc) {
        return dc.getResources().getItems().stream().filter(KubernetesHelper::isDeployment).allMatch(Readiness::isReady);
    }

    public static boolean isDeployment(HasMetadata res) {
        return res.getKind().equals("Deployment") || res.getKind().equals("StatefulSet");  // TODO: is there an existing constant for this somewhere?
    }

    @Override
    public List<Pod> listRouters() {
        return client.pods().withLabel(LabelKeys.CAPABILITY, "router").withLabel(LabelKeys.INFRA_UUID, infraUuid).list().getItems();
    }

    @Override
    public void create(KubernetesList resources) {
        client.lists().create(resources);
    }

    @Override
    public void apply(KubernetesList resources, boolean patchPersistentVolumeClaims, boolean replaceStatefulSets, Consumer<HasMetadata> appliedResourceConsumer) {
        for (HasMetadata resource : resources.getItems()) {
            apply(resource, patchPersistentVolumeClaims, replaceStatefulSets);
            if (appliedResourceConsumer != null) {
                appliedResourceConsumer.accept(resource);
            }
        }
    }

    @Override
    public void apply(HasMetadata resource, boolean patchPersistentVolumeClaims, boolean replaceStatefulSets) {
        try {
            if (resource instanceof ConfigMap) {
                client.configMaps().withName(resource.getMetadata().getName()).patch((ConfigMap) resource);
            } else if (resource instanceof Secret) {
                client.secrets().withName(resource.getMetadata().getName()).patch((Secret) resource);
            } else if (resource instanceof Deployment) {
                client.apps().deployments().withName(resource.getMetadata().getName()).patch((Deployment) resource);
            } else if (resource instanceof StatefulSet) {
                if (replaceStatefulSets) {
                    client.apps().statefulSets().withName(resource.getMetadata().getName()).cascading(false).replace((StatefulSet) resource);
                } else {
                    client.apps().statefulSets().withName(resource.getMetadata().getName()).cascading(false).patch((StatefulSet) resource);
                }
            } else if (resource instanceof Service) {
                client.services().withName(resource.getMetadata().getName()).patch((Service) resource);
            } else if (resource instanceof ServiceAccount) {
                client.serviceAccounts().withName(resource.getMetadata().getName()).patch((ServiceAccount) resource);
            } else if (resource instanceof PersistentVolumeClaim && patchPersistentVolumeClaims) {
                client.persistentVolumeClaims().withName(resource.getMetadata().getName()).patch((PersistentVolumeClaim) resource);
            }
        } catch (KubernetesClientException e) {
            if (e.getCode() == 404) {
                // Create it if it does not exist
                client.resource(resource).createOrReplace();
            } else {
                throw e;
            }
        }
    }

    @Override
    public void delete(KubernetesList resources) {
        for (HasMetadata resource : resources.getItems()) {
            int maxRetries = 10;
            int retry = 0;
            while (true) {
                try {
                    client.resource(resource).cascading(true).delete();
                    break;
                } catch (Exception e) {
                    if (retry < maxRetries) {
                        // Re-fetch resources to make it up to date
                        resource = client.resource(resource).get();
                        retry++;
                    } else {
                        log.warn("Error deleting {} after {} attempts", resource, maxRetries, e);
                        throw e;
                    }
                }
            }
        }
    }

    @Override
    public void scaleStatefulSet(String name, int numReplicas) {
        log.info("Scaling stateful set with id {} and {} replicas", name, numReplicas);
        client.apps().statefulSets().withName(name).scale(numReplicas);
    }

    @Override
    public List<Pod> listBrokers(String clusterId) {
        return client.pods().withLabel(LabelKeys.ROLE, "broker").withLabel(LabelKeys.NAME, clusterId).withLabel(LabelKeys.INFRA_UUID, infraUuid).list().getItems();
    }

    @Override
    public KubernetesList processTemplate(String templateName, Map<String,String> parameters) {
        File templateFile = new File(templateDir, templateName + TEMPLATE_SUFFIX);
        return Templates.process(templateFile, parameters);
    }
}
