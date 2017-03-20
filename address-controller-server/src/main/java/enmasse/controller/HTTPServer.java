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

import enmasse.controller.flavor.FlavorRepository;
import enmasse.controller.address.AddressManagerFactory;
import enmasse.controller.instance.InstanceController;
import enmasse.controller.api.http.RestServiceV1;
import enmasse.controller.api.http.RestServiceV2;
import enmasse.controller.api.v3.ApiHandler;
import enmasse.controller.api.v3.http.AddressingService;
import enmasse.controller.api.v3.http.FlavorsService;
import enmasse.controller.api.v3.http.InstanceService;
import enmasse.controller.api.v3.http.MultiInstanceAddressingService;
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
    private static final Logger log = LoggerFactory.getLogger(HTTPServer.class.getName());
    private final AddressManagerFactory addressManagerFactory;
    private final InstanceController instanceController;
    private final FlavorRepository flavorRepository;
    private final InstanceId globalInstance;

    public HTTPServer(InstanceId globalInstance, AddressManagerFactory addressManagerFactory, InstanceController instanceController, FlavorRepository flavorRepository) {
        this.globalInstance = globalInstance;
        this.addressManagerFactory = addressManagerFactory;
        this.instanceController = instanceController;
        this.flavorRepository = flavorRepository;
    }

    @Override
    public void start() {
        VertxResteasyDeployment deployment = new VertxResteasyDeployment();
        deployment.start();
        deployment.getRegistry().addSingletonResource(new RestServiceV1(globalInstance, addressManagerFactory, vertx));
        deployment.getRegistry().addSingletonResource(new RestServiceV2(globalInstance, addressManagerFactory, vertx));
        deployment.getRegistry().addSingletonResource(new AddressingService(globalInstance, new ApiHandler(addressManagerFactory)));
        deployment.getRegistry().addSingletonResource(new InstanceService(instanceController));
        deployment.getRegistry().addSingletonResource(new MultiInstanceAddressingService(new ApiHandler(addressManagerFactory)));
        deployment.getRegistry().addSingletonResource(new FlavorsService(flavorRepository));

        vertx.createHttpServer()
                .requestHandler(new VertxRequestHandler(vertx, deployment))
                .listen(8080, ar -> log.info("Started HTTP server"));
    }
}
