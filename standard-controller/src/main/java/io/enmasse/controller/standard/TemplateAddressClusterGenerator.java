/*
 * Copyright 2016-2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package io.enmasse.controller.standard;

import io.enmasse.address.model.*;
import io.enmasse.config.AnnotationKeys;
import io.enmasse.config.LabelKeys;
import io.enmasse.k8s.api.KubeUtil;
import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.KubernetesList;
import io.fabric8.kubernetes.api.model.KubernetesListBuilder;
import io.fabric8.openshift.client.ParameterValue;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Generates destination clusters using Openshift templates.
 */
public class TemplateAddressClusterGenerator implements AddressClusterGenerator {
    private final Kubernetes kubernetes;
    private final AddressResolver addressResolver;
    private final TemplateOptions templateOptions;

    public TemplateAddressClusterGenerator(Kubernetes kubernetes, AddressResolver addressResolver, TemplateOptions templateOptions) {
        this.kubernetes = kubernetes;
        this.addressResolver = addressResolver;
        this.templateOptions = templateOptions;
    }

    /**
     * Generate cluster for a given destination group.
     *
     * NOTE: This method assumes that all destinations within a group share the same properties.
     *
     * @param addressSet The set of addresses to generate cluster for
     */
    public AddressCluster generateCluster(String clusterId, Set<Address> addressSet) {
        Address first = addressSet.iterator().next();

        AddressPlan plan = addressResolver.getPlan(addressResolver.getType(first), first);
        List<ResourceDefinition> resourceDefinitions = addressResolver.getResourceDefinitions(plan);

        KubernetesListBuilder resourcesBuilder = new KubernetesListBuilder();
        for (ResourceDefinition resourceDefinition : resourceDefinitions) {
            if (resourceDefinition.getTemplateName().isPresent()) {
                KubernetesList newResources = processTemplate(clusterId, first, addressSet, resourceDefinition.getTemplateName().get(), resourceDefinition.getTemplateParameters());
                resourcesBuilder.addAllToItems(newResources.getItems());
            }
        }
        return new AddressCluster(clusterId, resourcesBuilder.build());
    }

    private KubernetesList processTemplate(String clusterId, Address first, Set<Address> addressSet, String templateName, Map<String, String> parameterMap) {
        Map<String, String> paramMap = new LinkedHashMap<>(parameterMap);

        paramMap.put(TemplateParameter.NAME, KubeUtil.sanitizeName(clusterId));
        paramMap.put(TemplateParameter.CLUSTER_ID, clusterId);
        paramMap.put(TemplateParameter.ADDRESS_SPACE, first.getAddressSpace());
        paramMap.put(TemplateParameter.COLOCATED_ROUTER_SECRET, templateOptions.getMessagingSecret());
        paramMap.put(TemplateParameter.AUTHENTICATION_SERVICE_HOST, templateOptions.getAuthenticationServiceHost());
        paramMap.put(TemplateParameter.AUTHENTICATION_SERVICE_PORT, templateOptions.getAuthenticationServicePort());
        paramMap.put(TemplateParameter.AUTHENTICATION_SERVICE_CA_SECRET, templateOptions.getAuthenticationServiceCaSecret());
        paramMap.put(TemplateParameter.AUTHENTICATION_SERVICE_CLIENT_SECRET, templateOptions.getAuthenticationServiceClientSecret());
        paramMap.put(TemplateParameter.AUTHENTICATION_SERVICE_SASL_INIT_HOST, templateOptions.getAuthenticationServiceSaslInitHost());

        // If the name of the group matches that of the address, assume a scalable queue
        if (clusterId.equals(first.getName()) && addressSet.size() == 1) {
            paramMap.put(TemplateParameter.ADDRESS, first.getAddress());
        }

        ParameterValue parameters[] = paramMap.entrySet().stream()
                .map(entry -> new ParameterValue(entry.getKey(), entry.getValue()))
                .collect(Collectors.toList())
                .toArray(new ParameterValue[0]);


        KubernetesList items = kubernetes.processTemplate(templateName, parameters);


        // These are attributes that we need to identify components belonging to this address
        Kubernetes.addObjectAnnotation(items, AnnotationKeys.CLUSTER_ID, clusterId);
        Kubernetes.addObjectAnnotation(items, AnnotationKeys.ADDRESS_SPACE, first.getAddressSpace());
        Kubernetes.addObjectLabel(items, LabelKeys.UUID, first.getUuid());
        return items;
    }
}
