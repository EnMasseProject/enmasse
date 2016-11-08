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

package enmasse.smoketest;

import java.util.Optional;

public class Destination {
    private final String address;
    private final boolean storeAndForward;
    private final boolean multicast;
    private final Optional<String> flavor;

    public Destination(String address, boolean storeAndForward, boolean multicast, Optional<String> flavor) {
        this.address = address;
        this.storeAndForward = storeAndForward;
        this.multicast = multicast;
        this.flavor = flavor;
    }

    public static Destination queue(String address) {
        return new Destination(address, true, false, Optional.of("vanilla-queue"));
    }

    public static Destination topic(String address) {
        return new Destination(address, true, true, Optional.of("vanilla-topic"));
    }

    public static Destination anycast(String address) {
        return new Destination(address, false, false, Optional.empty());
    }

    public static Destination broadcast(String address) {
        return new Destination(address, false, true, Optional.empty());
    }

    public String getAddress() {
        return address;
    }

    public boolean isStoreAndForward() {
        return storeAndForward;
    }

    public boolean isMulticast() {
        return multicast;
    }

    public Optional<String> getFlavor() {
        return flavor;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Destination that = (Destination) o;

        if (storeAndForward != that.storeAndForward) return false;
        if (multicast != that.multicast) return false;
        if (!address.equals(that.address)) return false;
        return flavor.equals(that.flavor);

    }

    @Override
    public int hashCode() {
        int result = address.hashCode();
        result = 31 * result + (storeAndForward ? 1 : 0);
        result = 31 * result + (multicast ? 1 : 0);
        result = 31 * result + flavor.hashCode();
        return result;
    }
}
