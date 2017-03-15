/*
 * Copyright 2017 Red Hat Inc.
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
package enmasse.address.controller.admin;

import enmasse.address.controller.generator.DestinationClusterGenerator;
import enmasse.address.controller.generator.TemplateDestinationClusterGenerator;
import enmasse.address.controller.generator.TemplateParameter;
import enmasse.address.controller.model.InstanceId;
import io.fabric8.kubernetes.api.model.KubernetesList;
import io.fabric8.openshift.api.model.DoneableTemplate;
import io.fabric8.openshift.api.model.Template;
import io.fabric8.openshift.client.OpenShiftClient;
import io.fabric8.openshift.client.ParameterValue;
import io.fabric8.openshift.client.dsl.ClientTemplateResource;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Manages namespaces and infrastructure for a single instance.
 */
public class AddressManagerFactoryImpl implements AddressManagerFactory {
    private final OpenShiftClient openShiftClient;
    private final InstanceClientFactory clientFactory;
    private final FlavorRepository flavorRepository;
    private final TemplateRepository templateRepository;
    private final boolean isMultiinstance;
    private final String instanceTemplateName;

    public AddressManagerFactoryImpl(OpenShiftClient openShiftClient, InstanceClientFactory clientFactory, FlavorRepository flavorRepository, boolean isMultiinstance, boolean useTLS) {
        this.openShiftClient = openShiftClient;
        this.templateRepository = new TemplateRepository(openShiftClient);
        this.clientFactory = clientFactory;
        this.flavorRepository = flavorRepository;
        this.isMultiinstance = isMultiinstance;
        this.instanceTemplateName = useTLS ? "tls-enmasse-instance-infra" : "enmasse-instance-infra";
    }

    @Override
    public Optional<AddressManager> getAddressManager(InstanceId instance) {
        return hasInstance(instance) ? Optional.of(createManager(instance)) : Optional.empty();
    }

    private AddressManager createManager(InstanceId instance) {
        OpenShiftClient instanceClient = createInstanceClient(instance);

        DestinationClusterGenerator generator = new TemplateDestinationClusterGenerator(instance, instanceClient, templateRepository, flavorRepository);
        return new AddressManagerImpl(new OpenShiftHelper(instance, instanceClient), generator);
    }

    private OpenShiftClient createInstanceClient(InstanceId instance) {
        if (isMultiinstance) {
            return clientFactory.createClient(instance);
        } else {
            return openShiftClient;
        }
    }

    @Override
    public AddressManager getOrCreateAddressManager(InstanceId instance) {
        if (hasInstance(instance)) {
            return createManager(instance);
        } else {
            return deployInstance(instance);
        }
    }

    private void createNamespace(InstanceId instance) {
        if (isMultiinstance) {
            openShiftClient.namespaces().createNew()
                    .editOrNewMetadata()
                    .withName("enmasse-" + instance.toString())
                    .addToLabels("app", "enmasse")
                    .addToLabels("instance", instance.toString())
                    .endMetadata()
                    .done();
        }
    }

    private boolean hasInstance(InstanceId instance) {
        Map<String, String> labelMap = new HashMap<>();
        labelMap.put("app", "enmasse");
        labelMap.put("instance", instance.toString());
        if (isMultiinstance) {
            return !openShiftClient.namespaces().withLabels(labelMap).list().getItems().isEmpty();
        } else {
            return !openShiftClient.services().withLabels(labelMap).list().getItems().isEmpty();
        }
    }

    private AddressManager deployInstance(InstanceId instance) {
        createNamespace(instance);

        ClientTemplateResource<Template, KubernetesList, DoneableTemplate> templateProcessor = templateRepository.getTemplate(instanceTemplateName);

        Map<String, String> paramMap = new LinkedHashMap<>();

        // If the flavor is shared, there is only one instance of it, so give it the name of the flavor
        paramMap.put(TemplateParameter.INSTANCE, OpenShiftHelper.nameSanitizer(instance.toString()));

        ParameterValue parameters[] = new ParameterValue[1];
        parameters[0] = new ParameterValue(TemplateParameter.INSTANCE, OpenShiftHelper.nameSanitizer(instance.toString()));
        KubernetesList items = templateProcessor.process(parameters);

        OpenShiftClient instanceClient = createInstanceClient(instance);
        instanceClient.lists().create(items);

        DestinationClusterGenerator generator = new TemplateDestinationClusterGenerator(instance, instanceClient, templateRepository, flavorRepository);
        return new AddressManagerImpl(new OpenShiftHelper(instance, instanceClient), generator);
    }
}
