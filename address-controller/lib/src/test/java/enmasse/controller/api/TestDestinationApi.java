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

package enmasse.controller.api;

import enmasse.controller.address.api.DestinationApi;
import enmasse.controller.model.Destination;

import java.util.LinkedHashSet;
import java.util.Optional;
import java.util.Set;

public class TestDestinationApi implements DestinationApi {
    public boolean throwException = false;

    private final Set<Destination> destinations = new LinkedHashSet<>();

    @Override
    public void createDestination(Destination destination) {
        if (throwException) {
            throw new RuntimeException("exception");
        }
        destinations.add(destination);
    }

    @Override
    public void deleteDestination(Destination destination) {
        if (throwException) {
            throw new RuntimeException("exception");
        }
        destinations.remove(destination);
    }

    @Override
    public Optional<Destination> getDestinationWithAddress(String address) {
        if (throwException) {
            throw new RuntimeException("exception");
        }
        return destinations.stream().filter(d -> d.address().equals(address)).findAny();
    }

    @Override
    public Optional<Destination> getDestinationWithUuid(String uuid) {
        if (throwException) {
            throw new RuntimeException("exception");
        }
        return destinations.stream().filter(d -> d.uuid().isPresent() && d.uuid().get().equals(uuid)).findAny();
    }

    @Override
    public Set<Destination> listDestinations() {
        if (throwException) {
            throw new RuntimeException("exception");
        }
        return new LinkedHashSet<>(destinations);
    }
}
