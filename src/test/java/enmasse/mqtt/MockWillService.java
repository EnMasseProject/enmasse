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

/**
 * Mock for the Will Service
 */
public class MockWillService {

    private static final String WILL_SERVICE_ENDPOINT = "$mqtt.willservice";

    ProtonServer server;

    public MockWillService(Vertx vertx) {

        this.server = ProtonServer.create(vertx);
        this.server.connectHandler(this::connectHandler);

        this.server.listen();
    }

    private void connectHandler(ProtonConnection connection) {

        connection
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
                    .handler(this::messageHandler)
                    .open();
        }
    }

    private void messageHandler(ProtonDelivery delivery, Message message) {

        // TODO:
    }
}
