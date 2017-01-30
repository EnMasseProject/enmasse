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

package enmasse.address.controller.model;

import java.util.*;

/**
 * Represents a single destination in the addressing config. It is identified by an address and
 * additional properties that determine the semantics.
 */
public final class Destination {
    private final String address;
    private final boolean storeAndForward;
    private final boolean multicast;
    private final Optional<String> flavor;

    public Destination(String address, boolean storeAndForward, boolean multicast, Optional<String> flavor) {
        Objects.requireNonNull(flavor);
        this.address = address;
        this.storeAndForward = storeAndForward;
        this.multicast = multicast;
        this.flavor = flavor;
    }

    public String address() {
        return address;
    }

    public boolean storeAndForward() {
        return storeAndForward;
    }

    public boolean multicast() {
        return multicast;
    }

    public Optional<String> flavor() {
        return flavor;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Destination destination = (Destination) o;
        return address.equals(destination.address);

    }

    @Override
    public int hashCode() {
        return address.hashCode();
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("{address=").append(address).append(",")
                .append("storeAndForward=").append(storeAndForward).append(",")
                .append("multicast=").append(multicast).append(",")
                .append("flavor=").append(flavor).append("}");
        return builder.toString();
    }

    public static class Builder {
        private String address;
        private boolean storeAndForward = false;
        private boolean multicast = false;
        private Optional<String> flavor = Optional.empty();

        public Builder(String address) {
            this.address = address;
        }

        public Builder(Destination destination) {
            this.address = destination.address();
            this.storeAndForward = destination.storeAndForward();
            this.multicast = destination.multicast();
            this.flavor = destination.flavor();
        }

        public Builder address(String address) {
            this.address = address;
            return this;
        }

        public Builder storeAndForward(boolean storeAndForward) {
            this.storeAndForward = storeAndForward;
            return this;
        }

        public Builder multicast(boolean multicast) {
            this.multicast = multicast;
            return this;
        }

        public Builder flavor(Optional<String> flavor) {
            this.flavor = flavor;
            return this;
        }

        public Optional<String> flavor() {
            return flavor;
        }

        public Destination build() {
            return new Destination(address, storeAndForward, multicast, flavor);
        }
    }
}
