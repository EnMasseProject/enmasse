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
import io.fabric8.kubernetes.api.model.apps.Deployment;
import io.fabric8.kubernetes.api.model.apps.StatefulSet;
import io.fabric8.openshift.client.ParameterValue;

import java.util.*;

/**
 * Generates sets of brokers using Openshift templates.
 */
public class TemplateBrokerSetGenerator implements BrokerSetGenerator {
    private static final ObjectMapper mapper = new ObjectMapper();
    private final Kubernetes kubernetes;
    private final TemplateOptions templateOptions;
    private final String addressSpace;
    private final String infraUuid;
    private final SchemaProvider schemaProvider;

    public TemplateBrokerSetGenerator(Kubernetes kubernetes, TemplateOptions templateOptions, String addressSpace, String infraUuid, SchemaProvider schemaProvider) {
        this.kubernetes = kubernetes;
        this.templateOptions = templateOptions;
        this.addressSpace = addressSpace;
        this.infraUuid = infraUuid;
        this.schemaProvider = schemaProvider;
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
            return standardInfraConfig.getMetadata().getAnnotations().get(AnnotationKeys.QUEUE_TEMPLATE_NAME);
        } else {
            if (isShardedTopic(addressPlan)) {
                return standardInfraConfig.getMetadata().getAnnotations().get(AnnotationKeys.TOPIC_TEMPLATE_NAME);
            } else {
                return standardInfraConfig.getMetadata().getAnnotations().get(AnnotationKeys.QUEUE_TEMPLATE_NAME);
            }
        }
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
        paramMap.put(TemplateParameter.INFRA_UUID, infraUuid);
        paramMap.put(TemplateParameter.CLUSTER_ID, clusterId);
        paramMap.put(TemplateParameter.ADDRESS_SPACE, addressSpace);
        paramMap.put(TemplateParameter.AUTHENTICATION_SERVICE_HOST, templateOptions.getAuthenticationServiceHost());
        paramMap.put(TemplateParameter.AUTHENTICATION_SERVICE_PORT, templateOptions.getAuthenticationServicePort());
        paramMap.put(TemplateParameter.AUTHENTICATION_SERVICE_CA_SECRET, templateOptions.getAuthenticationServiceCaSecret());
        paramMap.put(TemplateParameter.AUTHENTICATION_SERVICE_CLIENT_SECRET, templateOptions.getAuthenticationServiceClientSecret());
        paramMap.put(TemplateParameter.AUTHENTICATION_SERVICE_SASL_INIT_HOST, templateOptions.getAuthenticationServiceSaslInitHost());

        if (address != null) {
            paramMap.put(TemplateParameter.ADDRESS, address.getAddress());
        }

        paramMap.put(TemplateParameter.BROKER_MEMORY_LIMIT, standardInfraConfig.getSpec().getBroker().getResources().getMemory());
        paramMap.put(TemplateParameter.BROKER_ADDRESS_FULL_POLICY, standardInfraConfig.getSpec().getBroker().getAddressFullPolicy());
        paramMap.put(TemplateParameter.BROKER_STORAGE_CAPACITY, standardInfraConfig.getSpec().getBroker().getResources().getStorage());

        ParameterValue parameters[] = paramMap.entrySet().stream()
                .map(entry -> new ParameterValue(entry.getKey(), entry.getValue())).toArray(ParameterValue[]::new);


        KubernetesList items = kubernetes.processTemplate(templateName, parameters);

        for (HasMetadata item : items.getItems()) {
            if (item instanceof StatefulSet) {
                StatefulSet set = (StatefulSet) item;
                set.getSpec().setReplicas(numReplicas);
                Kubernetes.addObjectAnnotation(item, AnnotationKeys.APPLIED_INFRA_CONFIG, mapper.writeValueAsString(standardInfraConfig));
            } else if (item instanceof Deployment) {
                Deployment deployment = (Deployment) item;
                deployment.getSpec().setReplicas(numReplicas);
            }
        }

        // These are attributes that we need to identify components belonging to this address
        Kubernetes.addObjectAnnotation(items, AnnotationKeys.CLUSTER_ID, clusterId);
        Kubernetes.addObjectAnnotation(items, AnnotationKeys.ADDRESS_SPACE, addressSpace);
        if (address != null) {
            Kubernetes.addObjectAnnotation(items, AnnotationKeys.ADDRESS, address.getAddress());
        }
        return items;
    }
}
