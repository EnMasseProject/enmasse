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

package io.enmasse.controller.standard;

import io.enmasse.address.model.*;
import io.enmasse.address.model.types.standard.StandardAddressSpaceType;
import io.enmasse.config.AnnotationKeys;
import io.enmasse.config.LabelKeys;
import io.enmasse.address.model.types.Plan;
import io.enmasse.address.model.types.TemplateConfig;
import io.enmasse.k8s.api.KubeUtil;
import io.fabric8.kubernetes.api.model.KubernetesList;
import io.fabric8.openshift.client.ParameterValue;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Generates destination clusters using Openshift templates.
 */
public class TemplateAddressClusterGenerator implements AddressClusterGenerator {
    private final Kubernetes kubernetes;
    private static final AddressResolver addressResolver = new AddressResolver(new StandardAddressSpaceType());
    private final TemplateOptions templateOptions;

    public TemplateAddressClusterGenerator(Kubernetes kubernetes, TemplateOptions templateOptions) {
        this.kubernetes = kubernetes;
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

        Plan plan = addressResolver.getPlan(first);
        KubernetesList resources = plan.getTemplateConfig().map(t -> processTemplate(clusterId, first, addressSet, t)).orElse(new KubernetesList());
        return new AddressCluster(clusterId, resources);
    }

    private KubernetesList processTemplate(String clusterId, Address first, Set<Address> addressSet, TemplateConfig templateConfig) {
        Map<String, String> paramMap = new LinkedHashMap<>(templateConfig.getParameters());

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


        KubernetesList items = kubernetes.processTemplate(templateConfig.getName(), parameters);


        // These are attributes that we need to identify components belonging to this address
        Kubernetes.addObjectAnnotation(items, AnnotationKeys.CLUSTER_ID, clusterId);
        Kubernetes.addObjectAnnotation(items, AnnotationKeys.ADDRESS_SPACE, first.getAddressSpace());
        Kubernetes.addObjectLabel(items, LabelKeys.UUID, first.getUuid());
        return items;
    }
}
