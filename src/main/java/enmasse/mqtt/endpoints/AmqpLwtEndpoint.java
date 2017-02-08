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

package enmasse.mqtt.endpoints;

import io.vertx.core.AsyncResult;
import io.vertx.proton.ProtonConnection;
import io.vertx.proton.ProtonDelivery;
import io.vertx.proton.ProtonQoS;
import io.vertx.proton.ProtonReceiver;
import org.apache.qpid.proton.amqp.transport.AmqpError;
import org.apache.qpid.proton.amqp.transport.ErrorCondition;
import org.apache.qpid.proton.message.Message;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * LWT endpoint
 */
public class AmqpLwtEndpoint {

    private static final Logger LOG = LoggerFactory.getLogger(AmqpLwtEndpoint.class);

    public static final int AMQP_WILL_CREDITS = 1;
    public static final String LWT_SERVICE_ENDPOINT = "$lwt";

    private ProtonConnection connection;

    /**
     * Constructor
     *
     * @param connection    ProtonConnection instance
     */
    public AmqpLwtEndpoint(ProtonConnection connection) {
        this.connection = connection;
    }

    /**
     * Open the endpoint, opening the connection
     */
    public void open() {

        this.connection
                .sessionOpenHandler(session -> session.open())
                .receiverOpenHandler(this::receiverHandler)
                .open();
    }

    /**
     * Close the endpoint, closing the connection
     */
    public void close() {

        // TODO : check what to close other than connection while this class evolves
        if (this.connection != null) {
            this.connection.close();
        }
    }

    private void receiverHandler(ProtonReceiver receiver) {

        LOG.debug("Attaching link request");

        // the LWT service supports only the control address
        if (!receiver.getRemoteTarget().getAddress().equals(LWT_SERVICE_ENDPOINT)) {

            ErrorCondition errorCondition =
                    new ErrorCondition(AmqpError.NOT_FOUND, "The provided address isn't supported");

            receiver.setCondition(errorCondition)
                    .close();
        } else {

            receiver.setTarget(receiver.getRemoteTarget())
                    .setQoS(ProtonQoS.AT_LEAST_ONCE)
                    .handler((delivery, message) -> {
                        this.messageHandler(receiver, delivery, message);
                    })
                    .closeHandler(ar -> {
                        this.closeHandler(receiver, ar);
                    })
                    .setPrefetch(0)
                    .open();

            receiver.flow(AMQP_WILL_CREDITS);
        }
    }

    private void messageHandler(ProtonReceiver receiver, ProtonDelivery delivery, Message message) {
        // TODO

        // NOTE : after receiving the AMQP_WILL, a new credit should be issued
        //        with AMQP we want to change the "will" message during the client life
    }

    private void closeHandler(ProtonReceiver receiver, AsyncResult<ProtonReceiver> ar) {

        ErrorCondition errorCondition = null;

        // link detached without error, so the "will" should be cleared and not sent
        if (ar.succeeded()) {

            LOG.info("Clean disconnection from {}", receiver.getName());
            // TODO


        // link detached with error, so the "will" should be sent
        } else {

            LOG.info("Brute disconnection from {}", receiver.getName());

            errorCondition = new ErrorCondition(receiver.getRemoteCondition().getCondition(),
                    String.format("client detached with: %s", receiver.getRemoteCondition().getDescription()));

            // TODO
        }

        if (errorCondition != null) {
            receiver.setCondition(errorCondition);
        }
        receiver.close();
    }
}
