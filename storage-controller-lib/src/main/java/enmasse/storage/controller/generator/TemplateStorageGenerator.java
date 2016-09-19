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

import com.openshift.restclient.model.IResource;
import com.openshift.restclient.model.template.ITemplate;
import enmasse.storage.controller.admin.FlavorRepository;
import enmasse.storage.controller.model.AddressType;
import enmasse.storage.controller.model.Destination;
import enmasse.storage.controller.model.Flavor;
import enmasse.storage.controller.model.LabelKeys;
import enmasse.storage.controller.model.TemplateParameter;
import enmasse.storage.controller.openshift.OpenshiftClient;
import enmasse.storage.controller.openshift.StorageCluster;

import java.util.Collection;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Generates storage clusters using Openshift templates.
 */
public class TemplateStorageGenerator implements StorageGenerator {

    private final OpenshiftClient osClient;
    private final FlavorRepository flavorRepository;

    public TemplateStorageGenerator(OpenshiftClient osClient, FlavorRepository flavorRepository) {
        this.osClient = osClient;
        this.flavorRepository = flavorRepository;
    }

    /**
     * Generate required storage definition for a given destination.
     *
     * @param destination The destination to generate storage definitions for
     */
    public StorageCluster generateStorage(Destination destination) {
        ITemplate template;
        if (destination.storeAndForward()) {
            template = prepareStoreAndForwardTemplate(destination);
        } else {
            template = prepareDirectTemplate(destination);
        }

        Collection<IResource> resources = osClient.processTemplate(template);
        return new StorageCluster(osClient, destination, resources);
    }

    private ITemplate prepareStoreAndForwardTemplate(Destination destination) {
        Flavor flavor = flavorRepository.getFlavor(destination.flavor(), TimeUnit.SECONDS.toMillis(60));
        ITemplate template = osClient.getTemplate(flavor.templateName());
        if (!template.getLabels().containsKey(LabelKeys.ADDRESS_TYPE)) {
            throw new IllegalArgumentException("Template is missing label " + LabelKeys.ADDRESS_TYPE);
        }
        AddressType.validate(template.getLabels().get(LabelKeys.ADDRESS_TYPE));

        template.updateParameter(TemplateParameter.ADDRESS, destination.address());
        for (Map.Entry<String, String> entry : flavor.templateParameters().entrySet()) {
            template.updateParameter(entry.getKey(), entry.getValue());
        }
        template.addObjectLabel(LabelKeys.ADDRESS, destination.address());
        template.addObjectLabel(LabelKeys.FLAVOR, destination.flavor());
        template.addObjectLabel(LabelKeys.ADDRESS_TYPE, destination.multicast() ? AddressType.TOPIC.value() : AddressType.QUEUE.value());
        return template;
    }

    private ITemplate prepareDirectTemplate(Destination destination) {
        Flavor flavor = new Flavor.Builder().templateName(destination.multicast() ? "broacast" : "multicast").build();
        ITemplate template = osClient.getTemplate(flavor.templateName());
        template.updateParameter(TemplateParameter.ADDRESS, destination.address());
        return template;
    }
}
