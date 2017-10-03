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
package io.enmasse.controller.standard;

import io.enmasse.address.model.AddressSpace;
import io.enmasse.address.model.types.AddressSpaceType;
import io.enmasse.address.model.types.standard.StandardAddressSpaceType;
import io.enmasse.controller.common.*;
import io.enmasse.k8s.api.AddressSpaceApi;
import io.vertx.core.Vertx;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;


/**
 * The standard controller is responsible for watching address spaces of type standard, creating
 * infrastructure required and propagating relevant status information.
 */

public class StandardController implements AddressSpaceController {
    private static final Logger log = LoggerFactory.getLogger(StandardController.class.getName());
    private final AddressSpaceApi addressSpaceApi;

    private final Map<AddressSpace, String> addressControllerMap = new HashMap<>();
    private final Kubernetes kubernetes;
    private final AuthenticationServiceResolverFactory authResolverFactory;
    private final String certDir;
    private final StandardAddressSpaceType type = new StandardAddressSpaceType();
    private final Vertx vertx;

    public StandardController(Vertx vertx, AddressSpaceApi addressSpaceApi, Kubernetes kubernetes, AuthenticationServiceResolverFactory authResolverFactory, String certDir) {
        this.vertx = vertx;
        this.addressSpaceApi = addressSpaceApi;
        this.kubernetes = kubernetes;
        this.authResolverFactory = authResolverFactory;
        this.certDir = certDir;
    }

    @Override
    public AddressSpaceType getAddressSpaceType() {
        return type;
    }

    @Override
    public synchronized void resourcesUpdated(Set<AddressSpace> instances) throws Exception {
        createAddressControllers(instances);
        deleteAddressControllers(instances);
    }

    private void createAddressControllers(Set<AddressSpace> addressSpaces) {
        for (AddressSpace addressSpace : addressSpaces) {
            if (!addressControllerMap.containsKey(addressSpace)) {
                AddressClusterGenerator clusterGenerator = new TemplateAddressClusterGenerator(addressSpaceApi, kubernetes, authResolverFactory);
                AddressController addressController = new AddressController(
                        addressSpaceApi.withAddressSpace(addressSpace),
                        kubernetes.withNamespace(addressSpace.getNamespace()),
                        clusterGenerator,
                        certDir);
                log.info("Deploying address space controller for " + addressSpace.getName());
                vertx.deployVerticle(addressController, result -> {
                    if (result.succeeded()) {
                        addressControllerMap.put(addressSpace, result.result());
                    } else {
                        log.warn("Unable to deploy address controller for " + addressSpace.getName());
                    }
                });
            }
        }
    }

    private void deleteAddressControllers(Set<AddressSpace> addressSpaces) {
        Iterator<Map.Entry<AddressSpace, String>> it = addressControllerMap.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<AddressSpace, String> entry = it.next();
            if (!addressSpaces.contains(entry.getKey())) {
                vertx.undeploy(entry.getValue());
                it.remove();
            }
        }
    }

    public synchronized Set<AddressSpace> getAddressSpaces() {
        return new HashSet<>(addressControllerMap.keySet());
    }

}
