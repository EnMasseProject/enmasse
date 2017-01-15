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

package enmasse.storage.controller;

import enmasse.storage.controller.admin.AddressManager;
import enmasse.storage.controller.admin.AddressManagerImpl;
import enmasse.storage.controller.admin.FlavorManager;
import enmasse.storage.controller.admin.OpenShiftHelper;
import enmasse.storage.controller.generator.TemplateStorageGenerator;
import io.fabric8.openshift.client.DefaultOpenShiftClient;
import io.fabric8.openshift.client.OpenShiftClient;
import io.fabric8.openshift.client.OpenShiftConfigBuilder;
import io.vertx.core.Vertx;

import java.io.IOException;

public class StorageController implements Runnable, AutoCloseable {
    private final AddressManager addressManager;
    private final FlavorManager flavorManager;
    private final AMQPServer server;
    private final HTTPServer restServer;
    private final Vertx vertx;

    private final ConfigAdapter flavorWatcher;


    public StorageController(StorageControllerOptions options) throws IOException {
        this.vertx = Vertx.vertx();

        OpenShiftClient osClient = new DefaultOpenShiftClient(new OpenShiftConfigBuilder()
                .withMasterUrl(options.openshiftUrl())
                .withOauthToken(options.openshiftToken())
                .withNamespace(options.openshiftNamespace())
                .build());

        this.flavorManager = new FlavorManager();
        this.addressManager = new AddressManagerImpl(new OpenShiftHelper(osClient), new TemplateStorageGenerator(osClient, flavorManager));
        this.server = new AMQPServer(addressManager, options.port());
        this.restServer = new HTTPServer(addressManager);
        this.flavorWatcher = new ConfigAdapter(osClient, "flavor", flavorManager::configUpdated);
    }

    public void run() {
        flavorWatcher.start();
        vertx.deployVerticle(server);
        vertx.deployVerticle(restServer);
    }

    @Override
    public void close() throws Exception {
        flavorWatcher.stop();
        vertx.close();
    }
}
