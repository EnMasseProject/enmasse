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

import com.fasterxml.jackson.databind.ObjectMapper;
import enmasse.address.controller.admin.AddressManager;
import enmasse.address.controller.parser.DestinationParser;
import io.vertx.core.AbstractVerticle;
import io.vertx.proton.ProtonDelivery;
import io.vertx.proton.ProtonServer;
import io.vertx.proton.ProtonSession;
import org.apache.qpid.proton.amqp.messaging.*;
import org.apache.qpid.proton.message.Message;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * AMQPServer for triggering deployments through AMQP
 */
public class AMQPServer extends AbstractVerticle {
    private static final Logger log = LoggerFactory.getLogger(AMQPServer.class.getName());
    private final int port;
    private final AddressManager addressManager;
    private ProtonServer server;
    private static final ObjectMapper mapper = new ObjectMapper();

    public AMQPServer(AddressManager addressManager, int port) {
        this.port = port;
        this.addressManager = addressManager;
    }

    public void start() {
        server = ProtonServer.create(vertx);
        server.connectHandler(connection -> {
            connection.setContainer("address-controller");
            connection.openHandler(conn -> {
                log.info("Connection opened");
            }).closeHandler(conn -> {
                connection.close();
                connection.disconnect();
                log.info("Connection closed");
            }).disconnectHandler(protonConnection -> {
                connection.disconnect();
                log.info("Disconnected");
            }).open();
            connection.sessionOpenHandler(ProtonSession::open);
            connection.receiverOpenHandler(protonReceiver -> {
                if ("address-config".equals(protonReceiver.getRemoteTarget().getAddress())) {
                    protonReceiver.handler(this::onAddressConfig);
                    protonReceiver.open();
                } else {
                    protonReceiver.close();
                }
            });
        }).listen(port);;
    }

    private void onAddressConfig(ProtonDelivery delivery, Message message) {
        String data = (String) ((AmqpValue) message.getBody()).getValue();
        vertx.executeBlocking(future -> {
            try {
                addressManager.destinationsUpdated(DestinationParser.parse(mapper.readTree(data)));
            } catch (Exception e) {
                future.fail(e);
            }
        }, result -> {
            if (result.succeeded()) {
                delivery.disposition(new Accepted(), true);
            } else {
                delivery.disposition(new Rejected(), true);
            }
        });
    }


    public void stop() {
        if (server != null) {
            server.close();
        }
    }
}

