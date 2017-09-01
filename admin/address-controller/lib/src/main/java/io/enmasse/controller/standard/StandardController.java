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
import io.enmasse.address.model.types.standard.StandardAddressSpaceType;
import io.enmasse.controller.common.*;
import io.enmasse.k8s.api.AddressSpaceApi;
import io.fabric8.openshift.client.OpenShiftClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;


/**
 * The standard controller is responsible for watching address spaces of type standard, creating
 * infrastructure required and propagating relevant status information.
 */

public class StandardController extends ControllerBase {
    private static final Logger log = LoggerFactory.getLogger(StandardController.class.getName());

    private final ControllerHelper helper;

    private final Map<AddressSpace, String> addressControllerMap = new HashMap<>();
    private final Kubernetes kubernetes;
    private final AuthenticationServiceResolverFactory authResolverFactory;
    private final StandardAddressSpaceType standardType = new StandardAddressSpaceType();

    public StandardController(OpenShiftClient client, AddressSpaceApi addressSpaceApi, Kubernetes kubernetes, AuthenticationServiceResolverFactory authResolverFactory, boolean isMultitenant) {
        super(addressSpaceApi, client);
        Map<String, String> serviceMapping = new HashMap<>();
        serviceMapping.put("messaging", "amqps");
        serviceMapping.put("mqtt", "secure-mqtt");
        serviceMapping.put("console", "http");

        this.helper = new ControllerHelper(kubernetes, isMultitenant, authResolverFactory, serviceMapping);
        this.kubernetes = kubernetes;
        this.authResolverFactory = authResolverFactory;
    }

    @Override
    public synchronized void resourcesUpdated(Set<AddressSpace> instances) throws Exception {
        instances = instances.stream()
                .filter(addressSpace -> addressSpace.getType().getName().equals(standardType.getName()))
                .collect(Collectors.toSet());

        log.debug("Check standard address spaces: " + instances);
        createAddressSpaces(instances);
        retainAddressSpaces(instances);

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
                AddressClusterGenerator clusterGenerator = new TemplateAddressClusterGenerator(addressSpaceApi, kubernetes, authResolverFactory);
                AddressController addressController = new AddressController(
                        addressSpaceApi.withAddressSpace(addressSpace),
                        kubernetes.withNamespace(addressSpace.getNamespace()),
                        clusterGenerator);
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

    private void retainAddressSpaces(Set<AddressSpace> desiredAddressSpaces) {
        helper.retainAddressSpaces(desiredAddressSpaces);
    }

    private void createAddressSpaces(Set<AddressSpace> instances) {
        for (AddressSpace instance : instances) {
            helper.create(instance);
        }
    }
}
