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
 * Represents a single destination in the addressing config. It is identified by one or more addresses, but contains
 * additional properties that determine the semantics.
 */
public final class Destination {
    private final Set<String> addresses = new HashSet<>();
    private final boolean storeAndForward;
    private final boolean multicast;
    private final Optional<String> flavor;

    public Destination(String address, boolean storeAndForward, boolean multicast, Optional<String> flavor) {
        this(Collections.singleton(address), storeAndForward, multicast, flavor);
    }

    public Destination(Set<String> addresses, boolean storeAndForward, boolean multicast, Optional<String> flavor) {
        Objects.requireNonNull(flavor);
        this.addresses.addAll(addresses);
        this.storeAndForward = storeAndForward;
        this.multicast = multicast;
        this.flavor = flavor;
    }

    public Set<String> addresses() {
        return Collections.unmodifiableSet(addresses);
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
        return addresses.equals(destination.addresses);

    }

    @Override
    public int hashCode() {
        return addresses.hashCode();
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("{addresses=[").append(addresses).append("],")
                .append("storeAndForward=").append(storeAndForward).append(",")
                .append("multicast=").append(multicast).append(",")
                .append("flavor=").append(flavor).append("}");
        return builder.toString();
    }
}
