/*
 * Copyright 2016-2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package enmasse.mqtt.mocks;

import enmasse.mqtt.AmqpWillData;
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
import org.apache.qpid.proton.amqp.transport.AmqpError;
import org.apache.qpid.proton.amqp.transport.ErrorCondition;
import org.apache.qpid.proton.amqp.transport.LinkError;
import org.apache.qpid.proton.message.Message;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Mock for the Last Will and Testament Service
 */
public class MockLwtService extends AbstractVerticle {

    private static final Logger LOG = LoggerFactory.getLogger(MockLwtService.class);

    private static final String LWT_SERVICE_ENDPOINT = "$lwt";
    private static final String CONTAINER_ID = "lwt-service";

    private String internalServiceHost;
    private int internalServicePort;

    private ProtonClient client;
    private ProtonConnection connection;

    // handler called when the service publish a will (or not) on detached link
    // NOTE : it's useful for disconnection tests
    private Handler<Boolean> willHandler;

    @Override
    public void start(Future<Void> startFuture) throws Exception {

        this.client = ProtonClient.create(this.vertx);

        this.client.connect(this.internalServiceHost, this.internalServicePort, done -> {

            if (done.succeeded()) {

                LOG.info("Last Will and Testament Service started successfully ...");

                this.connection = done.result();
                this.connection.setContainer(CONTAINER_ID);

                this.connection
                        .sessionOpenHandler(session -> session.open())
                        .receiverOpenHandler(this::receiverHandler)
                        .open();

                startFuture.complete();

            } else {

                LOG.error("Error starting the Last Will and Testament Service ...", done.cause());

                startFuture.fail(done.cause());
            }
        });
    }

    @Override
    public void stop(Future<Void> stopFuture) throws Exception {

        this.connection.close();
        LOG.info("Last Will and Testament Service has been shut down successfully");
        stopFuture.complete();
    }

    private void receiverHandler(ProtonReceiver receiver) {

        // Last Will and Testament Service supports only the control address
        if (!receiver.getRemoteTarget().getAddress().equals(LWT_SERVICE_ENDPOINT)) {

            ErrorCondition error = new ErrorCondition(AmqpError.NOT_FOUND, "The endpoint provided is not supported");
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
                    .closeHandler(ar -> {

                        this.closeHandler(receiver, ar);
                    })
                    .open();
        }
    }

    private void messageHandler(ProtonReceiver receiver, ProtonDelivery delivery, Message message) {

        LOG.info("Received {}", message);

        if (message.getSubject().equals(AmqpWillMessage.AMQP_SUBJECT)) {

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
    }

    /**
     * Send a request for a "will" action to the mock broker
     *
     * @param receiverName  receiver name for which send the "will" action
     * @param willAction    "will" action to execute
     */
    private void willAction(String receiverName, String willAction) {

        DeliveryOptions options = new DeliveryOptions();
        options.addHeader(MockBroker.EB_WILL_ACTION_HEADER, willAction);

        this.vertx.eventBus().send(MockBroker.EB_WILL, receiverName, options, done -> {

            if (done.succeeded()) {

                if (this.willHandler != null) {
                    this.willHandler.handle((boolean) done.result().body());
                }
            }
        });
    }

    private void closeHandler(ProtonReceiver receiver, AsyncResult<ProtonReceiver> ar) {

        // link detached without error, so the "will" should be cleared and not sent
        if (ar.succeeded()) {

            // workaround for testing "brute disconnection" ignoring the DISCONNECT
            // so the related will clear. Eclipse Paho doesn't provide a way to
            // close connection without sending DISCONNECT.
            if (!receiver.getName().contains("ignore-disconnect")) {

                // send a clear request to mock broker; client link name as body
                this.willAction(receiver.getName(), MockBroker.EB_WILL_ACTION_CLEAR);

            } else {

                // send a delivery request to mock broker; client link name as body
                this.willAction(receiver.getName(), MockBroker.EB_WILL_ACTION_DELIVERY);
            }

        // link detached with error, so the "will" should be sent
        } else {

            ErrorCondition errorCondition = receiver.getRemoteCondition();

            if ((errorCondition != null) && (errorCondition.getCondition().compareTo(LinkError.DETACH_FORCED) == 0)) {

                // send a delivery request to mock broker; client link name as body
                this.willAction(receiver.getName(), MockBroker.EB_WILL_ACTION_DELIVERY);
            }
        }
    }

    /**
     * Set the address for connecting to the AMQP services
     *
     * @param internalServiceHost    address for AMQP connections
     * @return  current Mock Last Will and Testament Service instance
     */
    public MockLwtService setInternalServiceHost(String internalServiceHost) {
        this.internalServiceHost = internalServiceHost;
        return this;
    }

    /**
     * Set the port for connecting to the AMQP services
     *
     * @param internalServicePort   port for AMQP connections
     * @return  current Mock Last Will and Testament Service instance
     */
    public MockLwtService setInternalServicePort(int internalServicePort) {
        this.internalServicePort = internalServicePort;
        return this;
    }

    /**
     * Set the handler called when the service publish (or not) a will
     * on a detached link
     *
     * @param handler
     * @return
     */
    public MockLwtService willHandler(Handler<Boolean> handler) {
        this.willHandler = handler;
        return this;
    }
}
