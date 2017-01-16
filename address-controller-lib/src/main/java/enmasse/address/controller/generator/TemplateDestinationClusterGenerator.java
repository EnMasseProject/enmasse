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

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import enmasse.address.controller.admin.FlavorRepository;
import enmasse.address.controller.model.*;
import enmasse.address.controller.openshift.DestinationCluster;
import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.KubernetesList;
import io.fabric8.openshift.api.model.DoneableTemplate;
import io.fabric8.openshift.api.model.Template;
import io.fabric8.openshift.client.OpenShiftClient;
import io.fabric8.openshift.client.ParameterValue;
import io.fabric8.openshift.client.dsl.ClientTemplateResource;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * Generates destination clusters using Openshift templates.
 */
public class TemplateDestinationClusterGenerator implements DestinationClusterGenerator {

    private final OpenShiftClient osClient;
    private static final ObjectMapper mapper = new ObjectMapper();
    private final FlavorRepository flavorRepository;

    public TemplateDestinationClusterGenerator(OpenShiftClient osClient, FlavorRepository flavorRepository) {
        this.osClient = osClient;
        this.flavorRepository = flavorRepository;
    }

    /**
     * Generate cluseter for a given destination.
     *
     * @param destination The destinations to generate cluster for
     */
    public DestinationCluster generateCluster(Destination destination) {
        Flavor flavor = destination.flavor()
                .map(f -> flavorRepository.getFlavor(f, TimeUnit.SECONDS.toMillis(60)))
                .orElse(new Flavor.Builder("direct", "direct").build());

        ClientTemplateResource<Template, KubernetesList, DoneableTemplate> templateProcessor = osClient.templates().withName(flavor.templateName());

        Map<String, String> paramMap = new LinkedHashMap<>(flavor.templateParameters());
        String addressList = createAddressList(destination.addresses());

        // If the flavor is shared, there is only one instance of it, so give it the name of the flavor
        String firstAddress = destination.addresses().iterator().next();
        if (flavor.isShared()) {
            paramMap.put(TemplateParameter.NAME, nameSanitizer(flavor.name()));
        } else {
            paramMap.put(TemplateParameter.NAME, nameSanitizer(firstAddress));
        }
        paramMap.put(TemplateParameter.ADDRESS, firstAddress);
        paramMap.put(TemplateParameter.ADDRESS_LIST, addressList);
        paramMap.put(TemplateParameter.MULTICAST, String.valueOf(destination.multicast()));

        ParameterValue parameters[] = paramMap.entrySet().stream()
                .map(entry -> new ParameterValue(entry.getKey(), entry.getValue()))
                .collect(Collectors.toList())
                .toArray(new ParameterValue[0]);


        KubernetesList items = templateProcessor.process(parameters);

        // These are attributes that we need to identify components belonging to this address
        addObjectAnnotation(items, LabelKeys.ADDRESS_LIST, addressList);
        destination.flavor().ifPresent(f -> addObjectLabel(items, LabelKeys.FLAVOR, f));
        addObjectLabel(items, LabelKeys.STORE_AND_FORWARD, String.valueOf(destination.storeAndForward()));
        addObjectLabel(items, LabelKeys.MULTICAST, String.valueOf(destination.multicast()));

        return new DestinationCluster(osClient, destination, items, flavor.isShared());
    }


    private void addObjectLabel(KubernetesList items, String labelKey, String labelValue) {
        for (HasMetadata item : items.getItems()) {
            Map<String, String> labels = item.getMetadata().getLabels();
            labels.put(labelKey, labelValue);
            item.getMetadata().setLabels(labels);
        }
    }

    private void addObjectAnnotation(KubernetesList items, String annotationKey, String annotationValue) {
        for (HasMetadata item : items.getItems()) {
            Map<String, String> annotations = item.getMetadata().getAnnotations();
            if (annotations == null) {
                annotations = new LinkedHashMap<>();
            }
            annotations.put(annotationKey, annotationValue);
            item.getMetadata().setAnnotations(annotations);
        }
    }

    private static String createAddressList(Set<String> addresses) {
        try {
            ArrayNode array = mapper.createArrayNode();
            for (String address : addresses) {
                array.add(address);
            }
            return mapper.writeValueAsString(addresses);
        } catch (Exception e) {
            throw new RuntimeException("Unable to create address list", e);
        }
    }

    private static String nameSanitizer(String name) {
        return name.toLowerCase().replaceAll("[^a-z0-9]", "-");
    }
}
