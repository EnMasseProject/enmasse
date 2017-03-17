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
import enmasse.address.controller.model.Instance;
import enmasse.address.controller.model.InstanceId;

import java.util.Optional;

/**
 * Manages namespaces and infrastructure for a single instance.
 */
public class AddressManagerFactoryImpl implements AddressManagerFactory {
    private final OpenShift openShift;
    private final FlavorRepository flavorRepository;
    private final InstanceManager instanceManager;

    public AddressManagerFactoryImpl(OpenShift openShift, InstanceManager instanceManager, FlavorRepository flavorRepository) {
        this.openShift = openShift;
        this.flavorRepository = flavorRepository;
        this.instanceManager = instanceManager;
    }

    @Override
    public Optional<AddressManager> getAddressManager(InstanceId instanceId) {
        return instanceManager.get(instanceId).map(i -> createManager(i.id()));
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
        instanceManager.create(new Instance.Builder(instance).build());
        OpenShift instanceClient = createInstanceClient(instance);
        DestinationClusterGenerator generator = new TemplateDestinationClusterGenerator(instance, instanceClient, flavorRepository);
        return new AddressManagerImpl(instanceClient, generator);
    }
}
