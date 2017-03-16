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
import io.fabric8.openshift.client.ParameterValue;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Manages namespaces and infrastructure for a single instance.
 */
public class AddressManagerFactoryImpl implements AddressManagerFactory {
    private final OpenShift openShift;
    private final FlavorRepository flavorRepository;
    private final boolean isMultiinstance;
    private final String instanceTemplateName;

    public AddressManagerFactoryImpl(OpenShift openShift, FlavorRepository flavorRepository, boolean isMultiinstance, boolean useTLS) {
        this.openShift = openShift;
        this.flavorRepository = flavorRepository;
        this.isMultiinstance = isMultiinstance;
        this.instanceTemplateName = useTLS ? "tls-enmasse-instance-infra" : "enmasse-instance-infra";
    }

    @Override
    public Optional<AddressManager> getAddressManager(InstanceId instance) {
        return hasInstance(instance) ? Optional.of(createManager(instance)) : Optional.empty();
    }

    private AddressManager createManager(InstanceId instance) {
        OpenShift instanceClient = createInstanceClient(instance);

        DestinationClusterGenerator generator = new TemplateDestinationClusterGenerator(instance, instanceClient, flavorRepository);
        return new AddressManagerImpl(instanceClient, generator);
    }

    private OpenShift createInstanceClient(InstanceId instance) {
        if (isMultiinstance) {
            return openShift.mutateClient(instance);
        } else {
            return openShift;
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
            openShift.createNamespace(instance);
            openShift.addDefaultViewPolicy(instance);
        }
    }

    private boolean hasInstance(InstanceId instance) {
        Map<String, String> labelMap = new HashMap<>();
        labelMap.put("app", "enmasse");
        labelMap.put("instance", instance.getId());
        if (isMultiinstance) {
            return openShift.hasNamespace(labelMap);
        } else {
            return openShift.hasService(labelMap);
        }
    }

    private AddressManager deployInstance(InstanceId instance) {
        createNamespace(instance);

        ParameterValue parameters[] = new ParameterValue[1];
        parameters[0] = new ParameterValue(TemplateParameter.INSTANCE, OpenShift.sanitizeName(instance.getId()));
        KubernetesList items = openShift.processTemplate(instanceTemplateName, parameters);

        OpenShift instanceClient = createInstanceClient(instance);
        instanceClient.create(items);

        DestinationClusterGenerator generator = new TemplateDestinationClusterGenerator(instance, instanceClient, flavorRepository);
        return new AddressManagerImpl(instanceClient, generator);
    }
}
