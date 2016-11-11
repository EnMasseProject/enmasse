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

package enmasse.mqtt;

import io.vertx.core.Vertx;
import io.vertx.proton.*;
import org.apache.qpid.proton.amqp.transport.ErrorCondition;
import org.apache.qpid.proton.amqp.transport.LinkError;
import org.apache.qpid.proton.message.Message;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Mock for the Will Service
 */
public class MockWillService {

    private static final Logger LOG = LoggerFactory.getLogger(MockWillService.class);

    private static final String WILL_SERVICE_ENDPOINT = "$mqtt.willservice";

    private static final String BIND_ADDRESS = "0.0.0.0";
    private static final int LISTEN_PORT = 5672;

    private ProtonServer server;

    public MockWillService(Vertx vertx) {

        this.server = ProtonServer.create(vertx);
    }

    public void listen() {

        this.server.connectHandler(this::connectHandler);
        this.server.listen(LISTEN_PORT, BIND_ADDRESS, done -> {

            if (done.succeeded()) {

                LOG.info("Will Service started successfully ...");
            } else {

                LOG.info("Error starting the Will Service ...", done.cause());
            }
        });
    }

    private void connectHandler(ProtonConnection connection) {

        connection
                .sessionOpenHandler(session -> session.open())
                .receiverOpenHandler(this::receiverHandler)
                .open();

    }

    private void receiverHandler(ProtonReceiver receiver) {

        // Will Service supports only the control address
        if (!receiver.getRemoteTarget().getAddress().equals(WILL_SERVICE_ENDPOINT)) {

            ErrorCondition error = new ErrorCondition(LinkError.DETACH_FORCED, "The endpoint provided is not supported");
            receiver
                    .setCondition(error)
                    .close();
        } else {

            // TODO: tracking the AMQP sender

            receiver
                    .setTarget(receiver.getRemoteTarget())
                    .handler((delivery, message) -> {

                        this.messageHandler(receiver, delivery, message);
                    })
                    .open();
        }
    }

    private void messageHandler(ProtonReceiver receiver, ProtonDelivery delivery, Message message) {

        // TODO:
    }

    public void close() {

        this.server.close();
    }
}
