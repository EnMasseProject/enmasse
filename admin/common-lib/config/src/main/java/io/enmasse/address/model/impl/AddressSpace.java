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
package io.enmasse.address.model.impl;

import io.enmasse.address.model.Address;
import io.enmasse.address.model.AddressSpaceType;
import io.enmasse.address.model.Endpoint;
import io.enmasse.address.model.Plan;

import java.util.*;

/**
 * An EnMasse AddressSpace instance.
 */
public class AddressSpace implements io.enmasse.address.model.AddressSpace {
    private final String name;
    private final AddressSpaceType type;
    private final List<Address> addressList;
    private final List<Endpoint> endpointList;
    private final Plan plan;

    private AddressSpace(String name, AddressSpaceType type, List<Address> addressList, List<Endpoint> endpointList, Plan plan) {
        this.name = name;
        this.type = type;
        this.addressList = addressList;
        this.endpointList = endpointList;
        this.plan = plan;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public AddressSpaceType getType() {
        return type;
    }

    @Override
    public List<Address> getAddresses() {
        return Collections.unmodifiableList(addressList);
    }

    @Override
    public List<Endpoint> getEndpoints() {
        return Collections.unmodifiableList(endpointList);
    }

    @Override
    public Plan getPlan() {
        return plan;
    }

    public static class Builder {
        private String name;
        private AddressSpaceType type;
        private List<Address> addressList = new ArrayList<>();
        private List<Endpoint> endpointList = new ArrayList<>();
        private Plan plan;

        public Builder setName(String name) {
            this.name = name;
            return this;
        }

        public Builder setType(AddressSpaceType type) {
            this.type = type;
            return this;
        }

        public Builder setAddressList(List<Address> addressList) {
            this.addressList = new ArrayList<>(addressList);
            return this;
        }

        public Builder appendAddress(Address address) {
            this.addressList.add(address);
            return this;
        }

        public Builder setEndpointList(List<Endpoint> endpointList) {
            this.endpointList = new ArrayList<>(endpointList);
            return this;
        }

        public Builder appendEndpoint(Endpoint endpoint) {
            this.endpointList.add(endpoint);
            return this;
        }

        public Builder setPlan(Plan plan) {
            this.plan = plan;
            return this;
        }

        public AddressSpace build() {
            Objects.requireNonNull(name, "name not set");
            Objects.requireNonNull(type, "type not set");
            Objects.requireNonNull(addressList);
            Objects.requireNonNull(endpointList);
            Objects.requireNonNull(plan, "plan not set");
            return new AddressSpace(name, type, addressList, endpointList, plan);
        }
    }
}
