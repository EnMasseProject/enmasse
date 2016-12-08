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

package enmasse.storage.controller.generator;

import enmasse.storage.controller.admin.FlavorRepository;
import enmasse.storage.controller.model.*;
import enmasse.storage.controller.openshift.StorageCluster;
import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.KubernetesList;
import io.fabric8.openshift.api.model.DoneableTemplate;
import io.fabric8.openshift.api.model.Template;
import io.fabric8.openshift.client.OpenShiftClient;
import io.fabric8.openshift.client.ParameterValue;
import io.fabric8.openshift.client.dsl.ClientTemplateResource;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * Generates storage clusters using Openshift templates.
 */
public class TemplateStorageGenerator implements StorageGenerator {

    private final OpenShiftClient osClient;
    private final FlavorRepository flavorRepository;

    public TemplateStorageGenerator(OpenShiftClient osClient, FlavorRepository flavorRepository) {
        this.osClient = osClient;
        this.flavorRepository = flavorRepository;
    }

    /**
     * Generate required storage definition for a given destination.
     *
     * @param destination The destination to generate storage definitions for
     */
    public StorageCluster generateStorage(Destination destination) {
        KubernetesList items;
        if (destination.storeAndForward()) {
            items = prepareStoreAndForwardTemplate(destination);
        } else {
            items = prepareDirectTemplate(destination);
        }

        return new StorageCluster(osClient, destination, items);
    }

    private KubernetesList prepareStoreAndForwardTemplate(Destination destination) {
        Flavor flavor = flavorRepository.getFlavor(destination.flavor(), TimeUnit.SECONDS.toMillis(60));
        ClientTemplateResource<Template, KubernetesList, DoneableTemplate> templateProcessor = osClient.templates().withName(flavor.templateName());

        Template template = templateProcessor.get();
        if (!template.getMetadata().getLabels().containsKey(LabelKeys.ADDRESS_TYPE)) {
            throw new IllegalArgumentException("Template is missing label " + LabelKeys.ADDRESS_TYPE);
        }
        AddressType.validate(template.getMetadata().getLabels().get(LabelKeys.ADDRESS_TYPE));

        Map<String, String> paramMap = new LinkedHashMap<>(flavor.templateParameters());
        paramMap.put(TemplateParameter.NAME, nameSanitizer(destination.address()));
        paramMap.put(TemplateParameter.ADDRESS, destination.address());
        paramMap.put(TemplateParameter.MULTICAST, String.valueOf(destination.multicast()));

        ParameterValue parameters[] = paramMap.entrySet().stream()
                .map(entry -> new ParameterValue(entry.getKey(), entry.getValue()))
                .collect(Collectors.toList())
                .toArray(new ParameterValue[0]);


        KubernetesList items = templateProcessor.process(parameters);
        addObjectLabel(items, LabelKeys.ADDRESS, destination.address());
        addObjectLabel(items, LabelKeys.FLAVOR, destination.flavor());
        addObjectLabel(items, LabelKeys.ADDRESS_TYPE, destination.multicast() ? AddressType.TOPIC.value() : AddressType.QUEUE.value());
        return items;
    }

    private void addObjectLabel(KubernetesList items, String labelKey, String labelValue) {
        for (HasMetadata item : items.getItems()) {
            Map<String, String> labels = item.getMetadata().getLabels();
            labels.put(labelKey, labelValue);
            item.getMetadata().setLabels(labels);
        }
    }

    private KubernetesList prepareDirectTemplate(Destination destination) {
        Flavor flavor = new Flavor.Builder().templateName("direct").build();
        ClientTemplateResource<Template, KubernetesList, DoneableTemplate> template = osClient.templates().withName(flavor.templateName());

        ParameterValue parameters[] = new ParameterValue[3];
        parameters[0] = new ParameterValue(TemplateParameter.NAME, nameSanitizer(destination.address()));
        parameters[1] = new ParameterValue(TemplateParameter.ADDRESS, destination.address());
        parameters[2] = new ParameterValue(TemplateParameter.MULTICAST, String.valueOf(destination.multicast()));

        KubernetesList items = template.process(parameters);
        addObjectLabel(items, LabelKeys.ADDRESS, destination.address());
        addObjectLabel(items, LabelKeys.FLAVOR, destination.flavor());
        addObjectLabel(items, LabelKeys.ADDRESS_TYPE, destination.multicast() ? AddressType.TOPIC.value() : AddressType.QUEUE.value());
        return items;
    }

    private static String nameSanitizer(String name) {
        return name.toLowerCase().replaceAll("[^a-z0-9]", "-");
    }
}
