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
package io.enmasse.address.model.types.brokered;

import io.enmasse.address.model.types.AddressSpaceType;
import io.enmasse.address.model.types.AddressType;
import io.enmasse.address.model.types.common.Plan;
import io.enmasse.address.model.types.common.TemplateConfig;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static io.enmasse.address.model.types.brokered.BrokeredType.QUEUE;
import static io.enmasse.address.model.types.brokered.BrokeredType.TOPIC;

/**
 * Represents the Standard address space type.
 */
public class BrokeredAddressSpaceType implements AddressSpaceType {
    static final List<AddressType> types = Arrays.asList(QUEUE, TOPIC);
    static final List<io.enmasse.address.model.types.Plan> plans = Arrays.asList(
            new Plan("unlimited", "Unlimited", "No restrictions on resource usage", "09671470-61a7-11e7-8a6e-507b9def37d9",
                    new TemplateConfig("brokered-space-infra", Collections.emptyMap())));
    static final List<String> serviceNames = Arrays.asList("messaging", "console");

    @Override
    public String getName() {
        return "brokered";
    }

    @Override
    public String getDescription() {
        return "A brokered address space consists of a broker combined with a console for managing addresses.";
    }

    @Override
    public List<AddressType> getAddressTypes() {
        return Collections.unmodifiableList(types);
    }

    @Override
    public List<io.enmasse.address.model.types.Plan> getPlans() {
        return Collections.unmodifiableList(plans);
    }

    @Override
    public io.enmasse.address.model.types.Plan getDefaultPlan() {
        return plans.get(0);
    }

    @Override
    public List<String> getServiceNames() {
        return Collections.unmodifiableList(serviceNames);
    }
}
