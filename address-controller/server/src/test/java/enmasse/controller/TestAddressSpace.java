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

package enmasse.controller;

import enmasse.controller.address.AddressSpace;
import enmasse.controller.model.Destination;

import java.util.LinkedHashSet;
import java.util.Set;

public class TestAddressSpace implements AddressSpace {
    public boolean throwException = false;

    private final Set<Destination> destinations = new LinkedHashSet<>();

    @Override
    public Set<Destination> addDestination(Destination destination) {
        if (throwException) {
            throw new RuntimeException("exception");
        }
        destinations.add(destination);
        return new LinkedHashSet<>(destinations);
    }

    @Override
    public Set<Destination> deleteDestination(String address) {
        if (throwException) {
            throw new RuntimeException("exception");
        }
        destinations.removeIf(destination -> destination.address().equals(address));
        return new LinkedHashSet<>(destinations);
    }

    @Override
    public Set<Destination> deleteDestinationWithUuid(String uuid) {
        if (throwException) {
            throw new RuntimeException("exception");
        }
        destinations.removeIf(destination -> destination.uuid().filter(u -> u.equals(uuid)).isPresent());
        return new LinkedHashSet<>(destinations);
    }

    @Override
    public Set<Destination> setDestinations(Set<Destination> destinations) {
        if (throwException) {
            throw new RuntimeException("exception");
        }
        this.destinations.clear();
        this.destinations.addAll(destinations);
        return new LinkedHashSet<>(destinations);
    }

    @Override
    public Set<Destination> addDestinations(Set<Destination> destinations) {
        if (throwException) {
            throw new RuntimeException("exception");
        }
        this.destinations.addAll(destinations);
        return new LinkedHashSet<>(this.destinations);
    }

    @Override
    public Set<Destination> getDestinations() {
        if (throwException) {
            throw new RuntimeException("exception");
        }
        return new LinkedHashSet<>(destinations);
    }
}
