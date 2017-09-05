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

import io.enmasse.address.model.AddressSpace;
import io.enmasse.address.model.types.AddressSpaceType;
import io.enmasse.address.model.types.brokered.BrokeredAddressSpaceType;
import io.enmasse.controller.common.AuthenticationServiceResolverFactory;
import io.enmasse.controller.common.ControllerBase;
import io.enmasse.controller.common.ControllerHelper;
import io.enmasse.controller.common.Kubernetes;
import io.enmasse.k8s.api.AddressSpaceApi;
import io.fabric8.openshift.client.OpenShiftClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.stream.Collectors;

public class BrokeredController extends ControllerBase {
    private static final Logger log = LoggerFactory.getLogger(BrokeredController.class.getName());

    private final Map<AddressSpace, String> addressControllerMap = new HashMap<>();
    private final AddressSpaceType type = new BrokeredAddressSpaceType();
    private final ControllerHelper helper;

    public BrokeredController(OpenShiftClient controllerClient, AddressSpaceApi addressSpaceApi, Kubernetes kubernetes, AuthenticationServiceResolverFactory resolverFactory, boolean multiinstance) {
        super(addressSpaceApi, controllerClient);
        Map<String, String> serviceMapping = new HashMap<>();
        serviceMapping.put("messaging", "amqps");
        this.helper = new ControllerHelper(kubernetes, multiinstance, resolverFactory, serviceMapping);
    }

    @Override
    public synchronized void resourcesUpdated(Set<AddressSpace> instances) throws Exception {
        instances = instances.stream()
                .filter(addressSpace -> addressSpace.getType().getName().equals(type.getName()))
                .collect(Collectors.toSet());

        log.debug("Check brokered address spaces: " + instances);
        helper.createAddressSpaces(instances);
        helper.retainAddressSpaces(instances);

        for (AddressSpace instance : addressSpaceApi.listAddressSpaces()) {
            AddressSpace.Builder mutableAddressSpace = new AddressSpace.Builder(instance);
            updateReadiness(helper, mutableAddressSpace);
            updateEndpoints(mutableAddressSpace);
            addressSpaceApi.replaceAddressSpace(mutableAddressSpace.build());
        }

        createAddressControllers(instances);
        deleteAddressControllers(instances);
    }

    private void createAddressControllers(Set<AddressSpace> addressSpaces) {
        for (AddressSpace addressSpace : addressSpaces) {
            if (!addressControllerMap.containsKey(addressSpace)) {
                AddressController addressController = new AddressController();
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
}
