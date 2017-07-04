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
package io.enmasse.address.model.impl.types.standard;

import io.enmasse.address.model.AddressSpaceType;
import io.enmasse.address.model.AddressType;
import io.enmasse.address.model.Plan;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Represents the Standard address space type.
 */
public class StandardAddressSpaceType implements AddressSpaceType {
    private final List<AddressType> types = Arrays.asList(
            new QueueType(),
            new TopicType(),
            new AnycastType(),
            new BroadcastType());

    private final List<Plan> plans = Arrays.asList(new DefaultPlan());

    @Override
    public String getName() {
        return "standard";
    }

    @Override
    public String getDescription() {
        return "A standard address space consists of an AMQP router network in combination with " +
            "attachable 'storage units'. The implementation of a storage unit is hidden from the client " +
            "and the routers with a well defined API.";
    }

    @Override
    public List<AddressType> getAddressTypes() {
        return Collections.unmodifiableList(types);
    }

    @Override
    public List<Plan> getPlans() {
        return Collections.unmodifiableList(plans);
    }
}
