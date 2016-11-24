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

import com.openshift.restclient.ClientBuilder;
import com.openshift.restclient.IClient;
import enmasse.storage.controller.admin.ClusterManager;
import enmasse.storage.controller.admin.FlavorManager;
import enmasse.storage.controller.generator.TemplateStorageGenerator;
import enmasse.storage.controller.openshift.OpenshiftClient;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.vertx.core.Vertx;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

public class StorageController implements Runnable, AutoCloseable {
    private final ClusterManager clusterManager;
    private final FlavorManager flavorManager;
    private final AMQPServer server;
    private final Vertx vertx;

    private final ConfigAdapter flavorWatcher;


    public StorageController(StorageControllerOptions options) throws IOException {
        this.vertx = Vertx.vertx();

        IClient osClient = new ClientBuilder(options.openshiftUrl())
                .usingToken(options.openshiftToken())
                .build();

        OpenshiftClient openshiftClient = new OpenshiftClient(osClient, options.openshiftNamespace());
        this.flavorManager = new FlavorManager();
        this.clusterManager = new ClusterManager(openshiftClient, new TemplateStorageGenerator(openshiftClient, flavorManager));
        this.server = new AMQPServer(clusterManager, options.port());
        this.flavorWatcher = new ConfigAdapter(openshiftClient, "flavor", flavorManager::configUpdated);
    }

    public void run() {
        flavorWatcher.start();
        vertx.deployVerticle(server);
    }

    @Override
    public void close() throws Exception {
        flavorWatcher.stop();
        vertx.close();
    }
}
