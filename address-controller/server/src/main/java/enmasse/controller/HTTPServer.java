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

import enmasse.controller.api.osb.v2.bind.OSBBindingService;
import enmasse.controller.api.osb.v2.catalog.OSBCatalogService;
import enmasse.controller.api.osb.v2.lastoperation.OSBLastOperationService;
import enmasse.controller.api.osb.v2.provision.OSBProvisioningService;
import enmasse.controller.api.v3.AddressApiHelper;
import enmasse.controller.api.v3.http.AddressingService;
import enmasse.controller.api.v3.http.FlavorsService;
import enmasse.controller.api.v3.http.InstanceService;
import enmasse.controller.api.v3.http.MultiInstanceAddressingService;
import enmasse.controller.common.exceptionmapping.DefaultExceptionMapper;
import enmasse.controller.flavor.FlavorRepository;
import enmasse.controller.instance.api.InstanceApi;
import enmasse.controller.model.InstanceId;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.http.HttpServerOptions;
import io.vertx.core.net.PemKeyCertOptions;
import org.jboss.resteasy.plugins.server.vertx.VertxRequestHandler;
import org.jboss.resteasy.plugins.server.vertx.VertxResteasyDeployment;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.logging.FileHandler;

/**
 * HTTP server for deploying address config
 */
public class HTTPServer extends AbstractVerticle {
    public static final int PORT = 8080;
    private static final Logger log = LoggerFactory.getLogger(HTTPServer.class.getName());
    private final InstanceApi instanceApi;
    private final FlavorRepository flavorRepository;
    private final InstanceId globalInstance;
    private final String certDir;

    public HTTPServer(InstanceId globalInstance, InstanceApi instanceApi, FlavorRepository flavorRepository, String certDir) {
        this.globalInstance = globalInstance;
        this.instanceApi = instanceApi;
        this.flavorRepository = flavorRepository;
        this.certDir = certDir;
    }

    @Override
    public void start() {
        VertxResteasyDeployment deployment = new VertxResteasyDeployment();
        deployment.start();

        deployment.getProviderFactory().registerProvider(DefaultExceptionMapper.class);

        AddressApiHelper addressApi = new AddressApiHelper(instanceApi);
        deployment.getRegistry().addSingletonResource(new AddressingService(globalInstance, addressApi));
        deployment.getRegistry().addSingletonResource(new InstanceService(instanceApi));
        deployment.getRegistry().addSingletonResource(new MultiInstanceAddressingService(addressApi));
        deployment.getRegistry().addSingletonResource(new FlavorsService(flavorRepository));

        deployment.getRegistry().addSingletonResource(new OSBCatalogService(instanceApi, flavorRepository));
        deployment.getRegistry().addSingletonResource(new OSBProvisioningService(instanceApi, flavorRepository));
        deployment.getRegistry().addSingletonResource(new OSBBindingService(instanceApi, flavorRepository));
        deployment.getRegistry().addSingletonResource(new OSBLastOperationService(instanceApi, flavorRepository));

        HttpServerOptions options = new HttpServerOptions();
        if (new File(certDir).exists()) {
            File keyFile = new File(certDir, "tls.key");
            File certFile = new File(certDir, "tls.crt");
            log.info("Loading key from " + keyFile.getAbsolutePath() + ", cert from " + certFile.getAbsolutePath());
            options.setKeyCertOptions(new PemKeyCertOptions()
                    .setKeyPath(keyFile.getAbsolutePath())
                    .setCertPath(certFile.getAbsolutePath()));
            options.setSsl(true);
        }

        vertx.createHttpServer(options)
                .requestHandler(new VertxRequestHandler(vertx, deployment))
                .listen(PORT, ar -> log.info("Started HTTP server. Listening on port " + PORT));
    }
}
