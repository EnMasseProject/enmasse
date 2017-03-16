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

import enmasse.address.controller.admin.FlavorManager;
import enmasse.address.controller.admin.AddressManagerFactory;
import enmasse.address.controller.admin.AddressManagerFactoryImpl;
import enmasse.address.controller.admin.OpenShiftHelper;
import enmasse.address.controller.model.InstanceId;
import io.fabric8.kubernetes.client.ConfigBuilder;
import io.fabric8.openshift.client.DefaultOpenShiftClient;
import io.fabric8.openshift.client.OpenShiftClient;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.Vertx;

import java.io.IOException;

public class AddressController implements Runnable, AutoCloseable {
    private final FlavorManager flavorManager;
    private final AMQPServer server;
    private final HTTPServer restServer;
    private final AddressManagerFactory addressManagerFactory;
    private final Vertx vertx;

    private final ConfigAdapter flavorWatcher;


    public AddressController(AddressControllerOptions options) throws IOException {
        this.vertx = Vertx.vertx();

        OpenShiftClient controllerClient = new DefaultOpenShiftClient(new ConfigBuilder()
                .withMasterUrl(options.openshiftUrl())
                .withOauthToken(options.openshiftToken())
                .withNamespace(options.openshiftNamespace())
                .build());

        this.flavorManager = new FlavorManager();
        this.addressManagerFactory = new AddressManagerFactoryImpl(new OpenShiftHelper(InstanceId.withIdAndNamespace(options.openshiftNamespace(), options.openshiftNamespace()), controllerClient)
                ,flavorManager, options.isMultiinstance(), options.useTLS());
        this.server = new AMQPServer(addressManagerFactory, flavorManager, options.port());
        this.restServer = new HTTPServer(addressManagerFactory, flavorManager);
        this.flavorWatcher = new ConfigAdapter(controllerClient, "flavor", flavorManager::configUpdated);
    }

    public void run() {
        flavorWatcher.start();
        vertx.deployVerticle(server);
        vertx.deployVerticle(restServer, new DeploymentOptions().setWorker(true));
    }

    @Override
    public void close() throws Exception {
        flavorWatcher.stop();
        vertx.close();
    }
}
