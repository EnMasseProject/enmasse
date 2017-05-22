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

import enmasse.config.LabelKeys;
import enmasse.controller.address.DestinationCluster;
import enmasse.controller.flavor.FlavorRepository;
import enmasse.controller.model.Destination;
import enmasse.controller.model.Flavor;
import enmasse.controller.model.InstanceId;
import io.fabric8.kubernetes.api.model.KubernetesList;
import io.fabric8.openshift.client.ParameterValue;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * Generates destination clusters using Openshift templates.
 */
public class TemplateDestinationClusterGenerator implements DestinationClusterGenerator {
    private final Kubernetes kubernetes;
    private final FlavorRepository flavorRepository;
    private final InstanceId instance;

    public TemplateDestinationClusterGenerator(InstanceId instance, Kubernetes kubernetes, FlavorRepository flavorRepository) {
        this.instance = instance;
        this.kubernetes = kubernetes;
        this.flavorRepository = flavorRepository;
    }

    /**
     * Generate cluster for a given destination group.
     *
     * NOTE: This method assumes that all destinations within a group share the same properties.
     *
     * @param destinations The set of destinations to generate cluster for
     */
    public DestinationCluster generateCluster(Set<Destination> destinations) {
        Destination first = destinations.iterator().next();
        Optional<Flavor> flavor = first.flavor()
                .map(f -> flavorRepository.getFlavor(f, TimeUnit.SECONDS.toMillis(60)));

        KubernetesList resources = flavor.map(f -> processTemplate(first, destinations, f)).orElse(new KubernetesList());
        return new DestinationCluster(first.group(), resources);
    }

    private KubernetesList processTemplate(Destination first, Set<Destination> destinations, Flavor flavor) {
        String groupId = destinations.iterator().next().group();
        Map<String, String> paramMap = new LinkedHashMap<>(flavor.templateParameters());

        // If the flavor is shared, there is only one instance of it, so give it the name of the flavor
        paramMap.put(TemplateParameter.NAME, Kubernetes.sanitizeName(groupId));
        paramMap.put(TemplateParameter.INSTANCE, Kubernetes.sanitizeName(instance.getId()));
        paramMap.put(TemplateParameter.COLOCATED_ROUTER_SECRET, instance.getId() + "-certs");

        // If the name of the group matches that of the address, assume a scalable queue
        if (groupId.equals(first.address()) && destinations.size() == 1) {
            paramMap.put(TemplateParameter.ADDRESS, first.address());
        } else {
            paramMap.put(TemplateParameter.ADDRESS, "ENMASSE_INTERNAL_RESERVED");
        }

        ParameterValue parameters[] = paramMap.entrySet().stream()
                .map(entry -> new ParameterValue(entry.getKey(), entry.getValue()))
                .collect(Collectors.toList())
                .toArray(new ParameterValue[0]);


        KubernetesList items = kubernetes.processTemplate(flavor.templateName(), parameters);

        // These are attributes that we need to identify components belonging to this address
        Kubernetes.addObjectLabel(items, LabelKeys.GROUP_ID, Kubernetes.sanitizeName(groupId));
        Kubernetes.addObjectLabel(items, LabelKeys.ADDRESS_CONFIG, Kubernetes.sanitizeName("address-config-" + groupId));
        first.uuid().ifPresent(uuid -> Kubernetes.addObjectLabel(items, LabelKeys.UUID, uuid));
        return items;
    }
}
