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
import io.vertx.proton.ProtonClient;
import io.vertx.proton.ProtonConnection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicInteger;

public class StorageController implements Runnable, AutoCloseable {
    private static final Logger log = LoggerFactory.getLogger(StorageController.class.getName());

    private final ProtonClient client;
    private final StorageControllerOptions options;
    private volatile ProtonConnection connection;

    private final ClusterManager clusterManager;
    private final FlavorManager flavorManager;
    private final Vertx vertx;

    public StorageController(StorageControllerOptions options) throws IOException {
        this.vertx = Vertx.vertx();
        client = ProtonClient.create(vertx);

        IClient osClient = new ClientBuilder(options.openshiftUrl())
                .usingToken(options.openshiftToken())
                .build();

        OpenshiftClient openshiftClient = new OpenshiftClient(osClient, options.openshiftNamespace());
        this.flavorManager = new FlavorManager();
        this.clusterManager = new ClusterManager(openshiftClient, new TemplateStorageGenerator(openshiftClient, flavorManager));
        this.options = options;
    }

    public void run() {
        AtomicInteger openReceivers = new AtomicInteger(0);
        client.connect(options.configHost(), options.configPort(), connectionHandle -> {
            if (connectionHandle.succeeded()) {
                connection = connectionHandle.result();
                connection.open();

                openReceiver(connection, "flavor", new ConfigAdapter(flavorManager::configUpdated), openReceivers);
                openReceiver(connection, "maas", new ConfigAdapter(clusterManager::configUpdated), openReceivers);
            } else {
                log.error("Connect failed: " + connectionHandle.cause().getMessage());
                vertx.close();
            }
        });

        startHealthServer(openReceivers);
    }

    private void startHealthServer(AtomicInteger openReceivers) {
        vertx.createHttpServer()
                .requestHandler(request -> {
                    int numOpen = openReceivers.get();
                    if (numOpen == 2) {
                        log.debug("Got 2 receivers, sending OK");
                        request.response().setStatusCode(HttpResponseStatus.OK.code()).end();
                    } else {
                        log.debug("Got " + numOpen + " receivers, sending error");
                        request.response().setStatusCode(HttpResponseStatus.INTERNAL_SERVER_ERROR.code()).end();
                    }
                })
                .listen(8080);
    }

    private void openReceiver(ProtonConnection connection, String address, ConfigAdapter configAdapter, AtomicInteger openReceivers) {
        connection.createReceiver(address)
                .handler(configAdapter)
                .openHandler(result -> {
                    if (result.succeeded()) {
                        log.info("Receiver for " + address + " opened");
                        openReceivers.incrementAndGet();
                    } else {
                        log.warn("Unable to open receiver for " + address + ": " + result.cause().getMessage());
                        vertx.close();
                    }})
                .closeHandler(result -> {
                    if (result.succeeded()) {
                        openReceivers.decrementAndGet();
                        log.info("Receiver for " + address + " closed");

                    } else {
                        log.error("Receiver for " + address + " closed: " + result.cause().getMessage());
                        vertx.close();
                    }
                })
                .open();
    }

    @Override
    public void close() throws Exception {
        if (connection != null) {
            connection.close();
        }
    }
}
