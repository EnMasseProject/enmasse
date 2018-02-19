/*
 * Copyright 2016-2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package io.enmasse.controller.standard;

import io.enmasse.address.model.*;
import io.enmasse.config.AnnotationKeys;
import io.enmasse.address.model.KubeUtil;
import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.KubernetesList;
import io.fabric8.kubernetes.api.model.KubernetesListBuilder;
import io.fabric8.kubernetes.api.model.extensions.Deployment;
import io.fabric8.kubernetes.api.model.extensions.StatefulSet;
import io.fabric8.openshift.client.ParameterValue;

import java.util.*;

/**
 * Generates sets of brokers using Openshift templates.
 */
public class TemplateBrokerSetGenerator implements BrokerSetGenerator {
    private final Kubernetes kubernetes;
    private final TemplateOptions templateOptions;
    private final String addressSpace;

    public TemplateBrokerSetGenerator(Kubernetes kubernetes, TemplateOptions templateOptions, String addressSpace) {
        this.kubernetes = kubernetes;
        this.templateOptions = templateOptions;
        this.addressSpace = addressSpace;
    }

    /**
     * Generate cluster for a given destination group.
     *
     * NOTE: This method assumes that all destinations within a group share the same properties.
     *
     */
    public AddressCluster generateCluster(String clusterId, ResourceDefinition resourceDefinition, int numReplicas, Address address) {

        KubernetesListBuilder resourcesBuilder = new KubernetesListBuilder();
        if (resourceDefinition.getTemplateName().isPresent()) {
            KubernetesList newResources = processTemplate(clusterId, numReplicas, address, resourceDefinition.getTemplateName().get(), resourceDefinition.getTemplateParameters());
            resourcesBuilder.addAllToItems(newResources.getItems());
        }
        return new AddressCluster(clusterId, resourcesBuilder.build());
    }

    private KubernetesList processTemplate(String clusterId, int numReplicas, Address address, String templateName, Map<String, String> parameterMap) {
        Map<String, String> paramMap = new LinkedHashMap<>(parameterMap);

        paramMap.put(TemplateParameter.NAME, clusterId);
        paramMap.put(TemplateParameter.CLUSTER_ID, clusterId);
        paramMap.put(TemplateParameter.ADDRESS_SPACE, addressSpace);
        paramMap.put(TemplateParameter.COLOCATED_ROUTER_SECRET, templateOptions.getMessagingSecret());
        paramMap.put(TemplateParameter.AUTHENTICATION_SERVICE_HOST, templateOptions.getAuthenticationServiceHost());
        paramMap.put(TemplateParameter.AUTHENTICATION_SERVICE_PORT, templateOptions.getAuthenticationServicePort());
        paramMap.put(TemplateParameter.AUTHENTICATION_SERVICE_CA_SECRET, templateOptions.getAuthenticationServiceCaSecret());
        paramMap.put(TemplateParameter.AUTHENTICATION_SERVICE_CLIENT_SECRET, templateOptions.getAuthenticationServiceClientSecret());
        paramMap.put(TemplateParameter.AUTHENTICATION_SERVICE_SASL_INIT_HOST, templateOptions.getAuthenticationServiceSaslInitHost());

        if (address != null) {
            paramMap.put(TemplateParameter.ADDRESS, address.getAddress());
        }

        ParameterValue parameters[] = paramMap.entrySet().stream()
                .map(entry -> new ParameterValue(entry.getKey(), entry.getValue())).toArray(ParameterValue[]::new);


        KubernetesList items = kubernetes.processTemplate(templateName, parameters);

        for (HasMetadata item : items.getItems()) {
            if (item instanceof StatefulSet) {
                StatefulSet set = (StatefulSet) item;
                set.getSpec().setReplicas(numReplicas);
            } else if (item instanceof Deployment) {
                Deployment deployment = (Deployment) item;
                deployment.getSpec().setReplicas(numReplicas);
            }
        }

        // These are attributes that we need to identify components belonging to this address
        Kubernetes.addObjectAnnotation(items, AnnotationKeys.CLUSTER_ID, clusterId);
        Kubernetes.addObjectAnnotation(items, AnnotationKeys.ADDRESS_SPACE, addressSpace);
        return items;
    }
}
