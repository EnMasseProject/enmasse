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

import enmasse.controller.common.DestinationClusterGenerator;
import enmasse.controller.common.Kubernetes;
import enmasse.controller.common.TemplateDestinationClusterGenerator;
import enmasse.controller.flavor.FlavorRepository;
import enmasse.controller.model.Instance;

/**
 * Manages address spaces
 */
public class AddressManagerImpl implements AddressManager {
    private final Kubernetes kubernetes;
    private final FlavorRepository flavorRepository;

    public AddressManagerImpl(Kubernetes kubernetes, FlavorRepository flavorRepository) {
        this.kubernetes = kubernetes;
        this.flavorRepository = flavorRepository;
    }

    @Override
    public AddressSpace getAddressSpace(Instance instance) {
        Kubernetes instanceClient = kubernetes.mutateClient(instance.id());
        DestinationClusterGenerator generator = new TemplateDestinationClusterGenerator(instance.id(), instanceClient, flavorRepository);
        return new AddressSpaceImpl(instanceClient, generator);
    }
}
