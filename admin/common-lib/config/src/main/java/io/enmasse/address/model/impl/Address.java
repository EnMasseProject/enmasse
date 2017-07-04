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

import io.enmasse.address.model.AddressSpace;
import io.enmasse.address.model.AddressType;
import io.enmasse.address.model.Plan;

import java.util.Objects;

/**
 * An EnMasse Address instance.
 */
public class Address implements io.enmasse.address.model.Address {
    private final String name;
    private final String address;
    private final AddressSpace addressSpace;
    private final AddressType type;
    private final Plan plan;

    private Address(String name, String address, AddressSpace addressSpace, AddressType type, Plan plan) {
        this.name = name;
        this.address = address;
        this.addressSpace = addressSpace;
        this.type = type;
        this.plan = plan;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String getAddress() {
        return address;
    }

    @Override
    public AddressSpace getAddressSpace() {
        return addressSpace;
    }

    @Override
    public AddressType getType() {
        return type;
    }

    @Override
    public Plan getPlan() {
        return plan;
    }

    public static class Builder {
        private String name;
        private String address;
        private AddressSpace addressSpace;
        private AddressType type;
        private Plan plan;

        public Builder setName(String name) {
            this.name = name;
            return this;
        }

        public Builder setAddress(String address) {
            this.address = address;
            return this;
        }

        public Builder setAddressSpace(AddressSpace addressSpace) {
            this.addressSpace = addressSpace;
            return this;
        }

        public Builder setType(AddressType addressType) {
            this.type = addressType;
            return this;
        }

        public Builder setPlan(Plan plan) {
            this.plan = plan;
            return this;
        }

        public Address build() {
            Objects.requireNonNull(name, "name not set");
            Objects.requireNonNull(address, "address not set");
            Objects.requireNonNull(addressSpace, "addressSpace not set");
            Objects.requireNonNull(type, "type not set");
            Objects.requireNonNull(plan, "plan not set");

            return new Address(name, address, addressSpace, type, plan);
        }
    }

}
