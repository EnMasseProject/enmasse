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

package enmasse.address.controller.generator;

import enmasse.address.controller.admin.FlavorRepository;
import enmasse.address.controller.admin.OpenShiftHelper;
import enmasse.address.controller.admin.TemplateRepository;
import enmasse.address.controller.model.Destination;
import enmasse.address.controller.model.DestinationGroup;
import enmasse.address.controller.model.Flavor;
import enmasse.address.controller.model.TenantId;
import enmasse.address.controller.openshift.DestinationCluster;
import enmasse.config.LabelKeys;
import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.KubernetesList;
import io.fabric8.kubernetes.api.model.KubernetesListBuilder;
import io.fabric8.openshift.api.model.DoneableTemplate;
import io.fabric8.openshift.api.model.Template;
import io.fabric8.openshift.client.OpenShiftClient;
import io.fabric8.openshift.client.ParameterValue;
import io.fabric8.openshift.client.dsl.ClientTemplateResource;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * Generates destination clusters using Openshift templates.
 */
public class TemplateDestinationClusterGenerator implements DestinationClusterGenerator {

    private final OpenShiftClient osClient;
    private final TemplateRepository templateRepository;
    private final FlavorRepository flavorRepository;
    private final TenantId tenant;

    public TemplateDestinationClusterGenerator(TenantId tenant, OpenShiftClient osClient, TemplateRepository templateRepository, FlavorRepository flavorRepository) {
        this.tenant = tenant;
        this.osClient = osClient;
        this.templateRepository = templateRepository;
        this.flavorRepository = flavorRepository;
    }

    /**
     * Generate cluster for a given destination group.
     *
     * NOTE: This method assumes that all destinations within a group share the same properties.
     *
     * @param destinationGroup The group of destinations to generate cluster for
     */
    public DestinationCluster generateCluster(DestinationGroup destinationGroup) {
        Destination first = destinationGroup.getDestinations().iterator().next();
        Optional<Flavor> flavor = first.flavor()
                .map(f -> flavorRepository.getFlavor(f, TimeUnit.SECONDS.toMillis(60)));

        KubernetesList resources = flavor.map(f -> processTemplate(first, destinationGroup, f)).orElse(new KubernetesList());

        OpenShiftHelper helper = new OpenShiftHelper(tenant, osClient);
        KubernetesListBuilder combined = new KubernetesListBuilder(resources);
        combined.addToItems(helper.createAddressConfig(destinationGroup));

        return new DestinationCluster(helper, destinationGroup, combined.build());
    }

    private KubernetesList processTemplate(Destination first, DestinationGroup destinationGroup, Flavor flavor) {
        ClientTemplateResource<Template, KubernetesList, DoneableTemplate> templateProcessor = templateRepository.getTemplate(flavor.templateName());

        Map<String, String> paramMap = new LinkedHashMap<>(flavor.templateParameters());

        // If the flavor is shared, there is only one instance of it, so give it the name of the flavor
        paramMap.put(TemplateParameter.NAME, OpenShiftHelper.nameSanitizer(destinationGroup.getGroupId()));
        paramMap.put(TemplateParameter.TENANT, OpenShiftHelper.nameSanitizer(tenant.toString()));

        // If the name of the group matches that of the address, assume a scalable queue
        if (destinationGroup.getGroupId().equals(first.address()) && destinationGroup.getDestinations().size() == 1) {
            paramMap.put(TemplateParameter.ADDRESS, first.address());
        } else {
            paramMap.put(TemplateParameter.ADDRESS, "ENMASSE_INTERNAL_RESERVED");
        }

        ParameterValue parameters[] = paramMap.entrySet().stream()
                .map(entry -> new ParameterValue(entry.getKey(), entry.getValue()))
                .collect(Collectors.toList())
                .toArray(new ParameterValue[0]);


        KubernetesList items = templateProcessor.process(parameters);

        // These are attributes that we need to identify components belonging to this address
        addObjectLabel(items, LabelKeys.GROUP_ID, OpenShiftHelper.nameSanitizer(destinationGroup.getGroupId()));
        addObjectLabel(items, LabelKeys.ADDRESS_CONFIG, OpenShiftHelper.nameSanitizer("address-config-" + tenant.toString() + "-" + destinationGroup.getGroupId()));
        return items;
    }


    private void addObjectLabel(KubernetesList items, String labelKey, String labelValue) {
        for (HasMetadata item : items.getItems()) {
            Map<String, String> labels = item.getMetadata().getLabels();
            labels.put(labelKey, labelValue);
            item.getMetadata().setLabels(labels);
        }
    }
}
