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
package enmasse.controller.address;

import enmasse.controller.instance.InstanceController;
import enmasse.controller.common.OpenShift;
import enmasse.controller.common.DestinationClusterGenerator;
import enmasse.controller.common.TemplateDestinationClusterGenerator;
import enmasse.controller.model.Instance;
import enmasse.controller.model.InstanceId;
import enmasse.controller.flavor.FlavorRepository;

import java.util.Optional;

/**
 * Manages namespaces and infrastructure for a single instance.
 */
public class AddressManagerFactoryImpl implements AddressManagerFactory {
    private final OpenShift openShift;
    private final FlavorRepository flavorRepository;
    private final InstanceController instanceController;

    public AddressManagerFactoryImpl(OpenShift openShift, InstanceController instanceController, FlavorRepository flavorRepository) {
        this.openShift = openShift;
        this.flavorRepository = flavorRepository;
        this.instanceController = instanceController;
    }

    @Override
    public Optional<AddressManager> getAddressManager(InstanceId instanceId) {
        return instanceController.get(instanceId).map(i -> createManager(i.id()));
    }

    private AddressManager createManager(InstanceId instanceId) {
        OpenShift instanceClient = createInstanceClient(instanceId);

        DestinationClusterGenerator generator = new TemplateDestinationClusterGenerator(instanceId, instanceClient, flavorRepository);
        return new AddressManagerImpl(instanceClient, generator);
    }

    private OpenShift createInstanceClient(InstanceId instanceId) {
        return openShift.mutateClient(instanceId);
    }

    @Override
    public AddressManager getOrCreateAddressManager(InstanceId instanceId) {
        return getAddressManager(instanceId).orElseGet(() -> deployInstance(instanceId));
    }

    private AddressManager deployInstance(InstanceId instance) {
        instanceController.create(new Instance.Builder(instance).build());
        OpenShift instanceClient = createInstanceClient(instance);
        DestinationClusterGenerator generator = new TemplateDestinationClusterGenerator(instance, instanceClient, flavorRepository);
        return new AddressManagerImpl(instanceClient, generator);
    }
}
