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

package enmasse.address.controller;

import enmasse.address.controller.admin.FlavorRepository;
import enmasse.address.controller.admin.AddressManagerFactory;
import enmasse.address.controller.api.v1.http.RestServiceV1;
import enmasse.address.controller.api.v2.http.RestServiceV2;
import enmasse.address.controller.api.v3.ApiHandler;
import enmasse.address.controller.api.v3.http.AddressingService;
import enmasse.address.controller.api.v3.http.FlavorsService;
import enmasse.address.controller.api.v3.http.MultiInstanceAddressingService;
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
    private final FlavorRepository flavorRepository;

    public HTTPServer(AddressManagerFactory addressManagerFactory, FlavorRepository flavorRepository) {
        this.addressManagerFactory = addressManagerFactory;
        this.flavorRepository = flavorRepository;
    }

    @Override
    public void start() {
        VertxResteasyDeployment deployment = new VertxResteasyDeployment();
        deployment.start();
        deployment.getRegistry().addSingletonResource(new RestServiceV1(addressManagerFactory, vertx));
        deployment.getRegistry().addSingletonResource(new RestServiceV2(addressManagerFactory, vertx));
        deployment.getRegistry().addSingletonResource(new AddressingService(new ApiHandler(addressManagerFactory)));
        deployment.getRegistry().addSingletonResource(new MultiInstanceAddressingService(new ApiHandler(addressManagerFactory)));
        deployment.getRegistry().addSingletonResource(new FlavorsService(flavorRepository));

        vertx.createHttpServer()
                .requestHandler(new VertxRequestHandler(vertx, deployment))
                .listen(8080, ar -> log.info("Started HTTP server"));
    }
}
