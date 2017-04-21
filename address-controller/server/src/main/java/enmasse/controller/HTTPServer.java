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

package enmasse.controller;

import enmasse.controller.address.AddressManager;
import enmasse.controller.api.osb.v2.bind.OSBBindingService;
import enmasse.controller.api.osb.v2.catalog.OSBCatalogService;
import enmasse.controller.api.osb.v2.provision.OSBProvisioningService;
import enmasse.controller.api.v3.ApiHandler;
import enmasse.controller.api.v3.http.AddressingService;
import enmasse.controller.api.v3.http.FlavorsService;
import enmasse.controller.api.v3.http.InstanceService;
import enmasse.controller.api.v3.http.MultiInstanceAddressingService;
import enmasse.controller.common.exceptionmapping.DefaultExceptionMapper;
import enmasse.controller.flavor.FlavorRepository;
import enmasse.controller.instance.InstanceManager;
import enmasse.controller.model.InstanceId;
import io.vertx.core.AbstractVerticle;
import org.jboss.resteasy.plugins.server.vertx.VertxRequestHandler;
import org.jboss.resteasy.plugins.server.vertx.VertxResteasyDeployment;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * HTTP server for deploying address config
 */
public class HTTPServer extends AbstractVerticle {
    public static final int PORT = 8080;
    private static final Logger log = LoggerFactory.getLogger(HTTPServer.class.getName());
    private final AddressManager addressManager;
    private final InstanceManager instanceManager;
    private final FlavorRepository flavorRepository;
    private final InstanceId globalInstance;

    public HTTPServer(InstanceId globalInstance, AddressManager addressManager, InstanceManager instanceManager, FlavorRepository flavorRepository) {
        this.globalInstance = globalInstance;
        this.addressManager = addressManager;
        this.instanceManager = instanceManager;
        this.flavorRepository = flavorRepository;
    }

    @Override
    public void start() {
        VertxResteasyDeployment deployment = new VertxResteasyDeployment();
        deployment.start();

        deployment.getProviderFactory().registerProvider(DefaultExceptionMapper.class);

        deployment.getRegistry().addSingletonResource(new AddressingService(globalInstance, new ApiHandler(instanceManager, addressManager)));
        deployment.getRegistry().addSingletonResource(new InstanceService(instanceManager));
        deployment.getRegistry().addSingletonResource(new MultiInstanceAddressingService(new ApiHandler(instanceManager, addressManager)));
        deployment.getRegistry().addSingletonResource(new FlavorsService(flavorRepository));

        deployment.getRegistry().addSingletonResource(new OSBCatalogService(instanceManager, addressManager, flavorRepository));
        deployment.getRegistry().addSingletonResource(new OSBProvisioningService(instanceManager, addressManager, flavorRepository));
        deployment.getRegistry().addSingletonResource(new OSBBindingService(instanceManager, addressManager, flavorRepository));

        vertx.createHttpServer()
                .requestHandler(new VertxRequestHandler(vertx, deployment))
                .listen(PORT, ar -> log.info("Started HTTP server. Listening on port " + PORT));
    }
}
