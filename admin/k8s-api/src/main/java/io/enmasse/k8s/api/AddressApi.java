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

import java.util.Optional;
import java.util.Set;

/**
 * API for managing addresses in kubernetes.
 */
public interface AddressApi {
    Optional<Address> getAddressWithName(String name);
    Optional<Address> getAddressWithUuid(String uuid);
    Set<Address> listAddresses();

    void createAddress(Address address);
    void replaceAddress(Address address);
    void deleteAddress(Address address);

    Watch watchAddresses(Watcher<Address> watcher) throws Exception;
}
