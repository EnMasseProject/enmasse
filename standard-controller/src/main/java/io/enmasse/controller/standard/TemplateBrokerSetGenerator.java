/*
 * Copyright 2016-2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package io.enmasse.controller.standard;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.enmasse.address.model.*;
import io.enmasse.admin.model.v1.AddressPlan;
import io.enmasse.admin.model.v1.ResourceRequest;
import io.enmasse.admin.model.v1.StandardInfraConfig;
import io.enmasse.config.AnnotationKeys;
import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.KubernetesList;
import io.fabric8.kubernetes.api.model.KubernetesListBuilder;
import io.fabric8.kubernetes.api.model.PersistentVolumeClaim;
import io.fabric8.kubernetes.api.model.apps.Deployment;
import io.fabric8.kubernetes.api.model.apps.StatefulSet;
import java.util.*;

/**
 * Generates sets of brokers using Openshift templates.
 */
public class TemplateBrokerSetGenerator implements BrokerSetGenerator {
    private static final ObjectMapper mapper = new ObjectMapper();
    private final Kubernetes kubernetes;
    private final StandardControllerOptions options;

    public TemplateBrokerSetGenerator(Kubernetes kubernetes, StandardControllerOptions options) {
        this.kubernetes = kubernetes;
        this.options = options;
    }

    private boolean isShardedTopic(AddressPlan addressPlan) {
        if (addressPlan.getAddressType().equals("topic")) {
            boolean isSharded = true;
            for (ResourceRequest resourceRequest : addressPlan.getRequiredResources()) {
                if (resourceRequest.getName().equals("broker") && resourceRequest.getCredit() < 1) {
                    isSharded = false;
                    break;
                }
            }
            return isSharded;
        }
        return false;
    }

    private String getTemplateName(Address address, AddressPlan addressPlan, StandardInfraConfig standardInfraConfig) {
        if (address == null || addressPlan == null) {
            return getAnnotation(standardInfraConfig.getMetadata().getAnnotations(), AnnotationKeys.QUEUE_TEMPLATE_NAME, "queue-persisted");
        } else {
            if (isShardedTopic(addressPlan)) {
                return getAnnotation(standardInfraConfig.getMetadata().getAnnotations(), AnnotationKeys.TOPIC_TEMPLATE_NAME, "topic-persisted");
            } else {
                return getAnnotation(standardInfraConfig.getMetadata().getAnnotations(), AnnotationKeys.QUEUE_TEMPLATE_NAME, "queue-persisted");
            }
        }
    }

    private String getAnnotation(Map<String, String> annotations, String key, String defaultValue) {
        return Optional.ofNullable(annotations)
                .flatMap(m -> Optional.ofNullable(m.get(key)))
                .orElse(defaultValue);
    }

    /**
     * Generate cluster for a given destination group.
     *
     * NOTE: This method assumes that all destinations within a group share the same properties.
     *
     */
    @Override
    public BrokerCluster generateCluster(String clusterId, int numReplicas, Address address, AddressPlan addressPlan, StandardInfraConfig standardInfraConfig) throws Exception {

        KubernetesListBuilder resourcesBuilder = new KubernetesListBuilder();

        if (standardInfraConfig != null) {
            String templateName = getTemplateName(address, addressPlan, standardInfraConfig);
            KubernetesList newResources = processTemplate(clusterId, numReplicas, address, templateName, standardInfraConfig);
            resourcesBuilder.addAllToItems(newResources.getItems());
        }
        return new BrokerCluster(clusterId, resourcesBuilder.build());
    }

    private KubernetesList processTemplate(String clusterId, int numReplicas, Address address, String templateName, StandardInfraConfig standardInfraConfig) throws Exception {
        Map<String, String> paramMap = new LinkedHashMap<>();

        paramMap.put(TemplateParameter.NAME, clusterId);
        paramMap.put(TemplateParameter.INFRA_UUID, options.getInfraUuid());
        paramMap.put(TemplateParameter.CLUSTER_ID, clusterId);
        paramMap.put(TemplateParameter.ADDRESS_SPACE, options.getAddressSpace());
        paramMap.put(TemplateParameter.AUTHENTICATION_SERVICE_HOST, options.getAuthenticationServiceHost());
        paramMap.put(TemplateParameter.AUTHENTICATION_SERVICE_PORT, options.getAuthenticationServicePort());
        paramMap.put(TemplateParameter.AUTHENTICATION_SERVICE_CA_SECRET, options.getAuthenticationServiceCaSecret());
        paramMap.put(TemplateParameter.AUTHENTICATION_SERVICE_CLIENT_SECRET, options.getAuthenticationServiceClientSecret());
        paramMap.put(TemplateParameter.AUTHENTICATION_SERVICE_SASL_INIT_HOST, options.getAuthenticationServiceSaslInitHost());

        if (address != null) {
            paramMap.put(TemplateParameter.ADDRESS, address.getSpec().getAddress());
        }

        paramMap.put(TemplateParameter.BROKER_MEMORY_LIMIT, standardInfraConfig.getSpec().getBroker().getResources().getMemory());
        paramMap.put(TemplateParameter.BROKER_ADDRESS_FULL_POLICY, standardInfraConfig.getSpec().getBroker().getAddressFullPolicy());
        paramMap.put(TemplateParameter.BROKER_STORAGE_CAPACITY, standardInfraConfig.getSpec().getBroker().getResources().getStorage());

        KubernetesList items = kubernetes.processTemplate(templateName, paramMap);

        for (HasMetadata item : items.getItems()) {
            if (item instanceof StatefulSet) {
                StatefulSet set = (StatefulSet) item;
                set.getSpec().setReplicas(numReplicas);
                if (standardInfraConfig.getSpec().getBroker().getStorageClassName() != null) {
                    for (PersistentVolumeClaim persistentVolumeClaim : set.getSpec().getVolumeClaimTemplates()) {
                        persistentVolumeClaim.getSpec().setStorageClassName(standardInfraConfig.getSpec().getBroker().getStorageClassName());
                    }
                }
                Kubernetes.addObjectAnnotation(item, AnnotationKeys.APPLIED_INFRA_CONFIG, mapper.writeValueAsString(standardInfraConfig));
            } else if (item instanceof Deployment) {
                Deployment deployment = (Deployment) item;
                deployment.getSpec().setReplicas(numReplicas);
            }
        }

        // These are attributes that we need to identify components belonging to this address
        Kubernetes.addObjectAnnotation(items, AnnotationKeys.CLUSTER_ID, clusterId);
        Kubernetes.addObjectAnnotation(items, AnnotationKeys.ADDRESS_SPACE, options.getAddressSpace());
        if (address != null) {
            Kubernetes.addObjectAnnotation(items, AnnotationKeys.ADDRESS, address.getSpec().getAddress());
        }
        return items;
    }
}
