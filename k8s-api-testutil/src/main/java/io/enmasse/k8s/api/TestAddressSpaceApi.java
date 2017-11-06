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
package io.enmasse.k8s.api;

import io.enmasse.address.model.Address;
import io.enmasse.address.model.AddressSpace;
import io.enmasse.address.model.Status;
import io.enmasse.address.model.v1.CodecV1;
import io.fabric8.kubernetes.api.model.ConfigMap;

import java.util.*;
import java.util.stream.Collectors;

public class TestAddressSpaceApi implements AddressSpaceApi {
    Map<String, AddressSpace> addressSpaces = new HashMap<>();
    Map<String, TestAddressApi> addressApiMap = new LinkedHashMap<>();
    public boolean throwException = false;

    @Override
    public Optional<AddressSpace> getAddressSpaceWithName(String addressSpaceId) {
        if (throwException) {
            throw new RuntimeException("foo");
        }
        return Optional.ofNullable(addressSpaces.get(addressSpaceId));
    }

    @Override
    public void createAddressSpace(AddressSpace addressSpace) {
        if (throwException) {
            throw new RuntimeException("foo");
        }
        addressSpaces.put(addressSpace.getName(), addressSpace);
    }

    @Override
    public void replaceAddressSpace(AddressSpace addressSpace) {
        createAddressSpace(addressSpace);
    }

    @Override
    public void deleteAddressSpace(AddressSpace addressSpace) {
        if (throwException) {
            throw new RuntimeException("foo");
        }
        addressSpaces.remove(addressSpace.getName());
    }

    @Override
    public Set<AddressSpace> listAddressSpaces() {
        if (throwException) {
            throw new RuntimeException("foo");
        }
        return new LinkedHashSet<>(addressSpaces.values());
    }

    @Override
    public Watch watchAddressSpaces(Watcher<AddressSpace> watcher) throws Exception {
        return null;
    }

    @Override
    public AddressApi withAddressSpace(AddressSpace addressSpace) {
        if (!addressApiMap.containsKey(addressSpace.getName())) {
            addressSpaces.put(addressSpace.getName(), addressSpace);
            addressApiMap.put(addressSpace.getName(), new TestAddressApi());
        }
        return getAddressApi(addressSpace.getName());
    }

    public TestAddressApi getAddressApi(String id) {
        return addressApiMap.get(id);
    }

    public Set<Address> getAddresses() {
        return getAddressApis().stream()
                .flatMap(d -> d.listAddresses().stream())
                .collect(Collectors.toSet());
    }

    public Set<String> getAddressUuids() {
        return getAddresses().stream().map(d -> d.getUuid()).collect(Collectors.toSet());
    }

    public Collection<TestAddressApi> getAddressApis() {
        return addressApiMap.values();
    }

    public void setAllInstancesReady(boolean ready) {
        addressSpaces.entrySet().stream().forEach(entry -> addressSpaces.put(
                entry.getKey(),
                new AddressSpace.Builder(entry.getValue()).setStatus(new Status(ready)).build()));
    }
}
