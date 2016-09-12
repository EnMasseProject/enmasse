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

package enmasse.config.bridge.amqp;

import io.vertx.core.Vertx;
import io.vertx.proton.ProtonClient;
import io.vertx.proton.ProtonClientOptions;
import io.vertx.proton.ProtonConnection;
import io.vertx.proton.ProtonMessageHandler;

public class TestClient {

    private String serverHost;
    private int serverPort;
    private ProtonClient client;
    private ProtonConnection connection;

    public TestClient(String serverHost, int serverPort) {
        this.serverHost = serverHost;
        this.serverPort = serverPort;
        this.client = ProtonClient.create(Vertx.vertx());
    }

    public void subscribe(String address, ProtonMessageHandler handler) {
        client.connect(new ProtonClientOptions().setConnectTimeout(10000), serverHost, serverPort, connectResult -> {
            if (connectResult.succeeded()) {
                System.out.println("Connected'");
                connection = connectResult.result();
                connection.open();
                System.out.println("Creating receiver");
                connection.createReceiver(address).handler(handler).open();
            } else {
                System.out.println("Connection failed: " + connectResult.cause().getMessage());
            }
        });
    }

    public void close() {
        if (connection != null) {
            connection.close();
        }
    }
}
