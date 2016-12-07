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

import enmasse.mqtt.messages.AmqpWillClearMessage;
import enmasse.mqtt.messages.AmqpWillMessage;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.eventbus.DeliveryOptions;
import io.vertx.proton.ProtonClient;
import io.vertx.proton.ProtonConnection;
import io.vertx.proton.ProtonDelivery;
import io.vertx.proton.ProtonQoS;
import io.vertx.proton.ProtonReceiver;
import org.apache.qpid.proton.amqp.messaging.Accepted;
import org.apache.qpid.proton.amqp.transport.ErrorCondition;
import org.apache.qpid.proton.amqp.transport.LinkError;
import org.apache.qpid.proton.message.Message;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Mock for the Will Service
 */
public class MockWillService extends AbstractVerticle {

    private static final Logger LOG = LoggerFactory.getLogger(MockWillService.class);

    private static final String WILL_SERVICE_ENDPOINT = "$mqtt.willservice";
    private static final String CONTAINER_ID = "will-service";

    private String connectAddress;
    private int connectPort;

    private ProtonClient client;
    private ProtonConnection connection;

    // handler called when the service publish a will (or not) on detached link
    // NOTE : it's useful for disconnection tests
    private Handler<Boolean> willHandler;

    @Override
    public void start(Future<Void> startFuture) throws Exception {

        this.client = ProtonClient.create(this.vertx);

        this.client.connect(this.connectAddress, this.connectPort, done -> {

            if (done.succeeded()) {

                LOG.info("Will Service started successfully ...");

                this.connection = done.result();
                this.connection.setContainer(CONTAINER_ID);

                this.connection
                        .sessionOpenHandler(session -> session.open())
                        .receiverOpenHandler(this::receiverHandler)
                        .open();

                startFuture.complete();

            } else {

                LOG.error("Error starting the Will Service ...", done.cause());

                startFuture.fail(done.cause());
            }
        });
    }

    @Override
    public void stop(Future<Void> stopFuture) throws Exception {

        this.connection.close();
        LOG.info("Will Service has been shut down successfully");
        stopFuture.complete();
    }

    private void receiverHandler(ProtonReceiver receiver) {

        // Will Service supports only the control address
        if (!receiver.getRemoteTarget().getAddress().equals(WILL_SERVICE_ENDPOINT)) {

            ErrorCondition error = new ErrorCondition(LinkError.DETACH_FORCED, "The endpoint provided is not supported");
            receiver
                    .setCondition(error)
                    .close();
        } else {

            // TODO: tracking the AMQP sender ??

            receiver
                    .setTarget(receiver.getRemoteTarget())
                    .setQoS(ProtonQoS.AT_LEAST_ONCE)
                    .handler((delivery, message) -> {

                        this.messageHandler(receiver, delivery, message);
                    })
                    .closeHandler(this::closeHandler)
                    .open();
        }
    }

    private void messageHandler(ProtonReceiver receiver, ProtonDelivery delivery, Message message) {

        LOG.info("Received {}", message);

        switch (message.getSubject()) {

            case AmqpWillMessage.AMQP_SUBJECT:

                {
                    // get AMQP_WILL message, save it and send disposition for settlement
                    AmqpWillMessage amqpWillMessage = AmqpWillMessage.from(message);

                    this.vertx.sharedData().getLocalMap(MockBroker.EB_WILL)
                            .put(receiver.getName(), new AmqpWillData(receiver.getName(), amqpWillMessage));

                    DeliveryOptions options = new DeliveryOptions();
                    options.addHeader(MockBroker.EB_WILL_ACTION_HEADER, MockBroker.EB_WILL_ACTION_ADD);

                    this.vertx.eventBus().send(MockBroker.EB_WILL, receiver.getName(), options, done -> {

                        if (done.succeeded()) {
                            delivery.disposition(Accepted.getInstance(), true);
                        }
                    });

                }
                break;

            case AmqpWillClearMessage.AMQP_SUBJECT:

                {
                    // workaround for testing "brute disconnection" ignoring the DISCONNECT
                    // so the related AMQP_WILL_CLEAR. Eclipse Paho doesn't provide a way to
                    // close connection without sending DISCONNECT.
                    if (!receiver.getName().contains("ignore-disconnect")) {

                        DeliveryOptions options = new DeliveryOptions();
                        options.addHeader(MockBroker.EB_WILL_ACTION_HEADER, MockBroker.EB_WILL_ACTION_CLEAR);

                        this.vertx.eventBus().send(MockBroker.EB_WILL, receiver.getName(), options, done -> {

                            if (done.succeeded()) {
                                delivery.disposition(Accepted.getInstance(), true);
                            }
                        });
                    }

                }
                break;
        }

    }

    private void closeHandler(AsyncResult<ProtonReceiver> ar) {

        if (ar.succeeded()) {

            ProtonReceiver receiver = ar.result();

            // send a delivery request to mock broker; client link name as body
            DeliveryOptions options = new DeliveryOptions();
            options.addHeader(MockBroker.EB_WILL_ACTION_HEADER, MockBroker.EB_WILL_ACTION_DELIVERY);

            this.vertx.eventBus().send(MockBroker.EB_WILL, receiver.getName(), options, done -> {

                if (done.succeeded()) {

                    if (this.willHandler != null) {
                        this.willHandler.handle((boolean) done.result().body());
                    }
                }

            });

        }
    }

    /**
     * Set the address for connecting to the AMQP services
     *
     * @param connectAddress    address for AMQP connections
     * @return  current Mock Will Service instance
     */
    public MockWillService setConnectAddress(String connectAddress) {
        this.connectAddress = connectAddress;
        return this;
    }

    /**
     * Set the port for connecting to the AMQP services
     *
     * @param connectPort   port for AMQP connections
     * @return  current Mock Will Service instance
     */
    public MockWillService setConnectPort(int connectPort) {
        this.connectPort = connectPort;
        return this;
    }

    /**
     * Set the handler called when the service publish (or not) a will
     * on a detached link
     *
     * @param handler
     * @return
     */
    public MockWillService willHandler(Handler<Boolean> handler) {
        this.willHandler = handler;
        return this;
    }
}
