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
package io.enmasse.controller.brokered;

import io.enmasse.address.model.Address;
import io.enmasse.controller.common.Kubernetes;
import io.enmasse.k8s.api.AddressApi;
import io.enmasse.k8s.api.Watcher;
import io.vertx.core.AbstractVerticle;

import java.util.Set;

/**
 * Controls the addresses of a brokered address space.
 */
public class AddressController extends AbstractVerticle implements Watcher<Address> {
    private final AddressApi addressApi;
    private final Kubernetes kubernetes;

    public AddressController(AddressApi addressApi, Kubernetes kubernetes) {
        this.addressApi = addressApi;
        this.kubernetes = kubernetes;
    }

    @Override
    public void resourcesUpdated(Set<Address> resources) throws Exception {

    }
}
