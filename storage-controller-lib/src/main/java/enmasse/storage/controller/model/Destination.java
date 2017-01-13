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

package enmasse.storage.controller.model;

/**
 * Represents a single destination in the addressing config. It is identified by the address, but contains
 * additional properties that determine the semantics.
 */
public final class Destination {
    private final String address;
    private final boolean storeAndForward;
    private final boolean multicast;
    private final String flavor;

    public Destination(String address, boolean storeAndForward, boolean multicast, String flavor) {
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

    public String flavor() {
        return flavor;
    }
}
