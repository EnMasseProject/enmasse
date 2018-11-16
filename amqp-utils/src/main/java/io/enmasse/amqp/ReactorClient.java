/*
 * Copyright 2016-2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.amqp;

import org.apache.qpid.proton.Proton;
import org.apache.qpid.proton.amqp.messaging.Received;
import org.apache.qpid.proton.amqp.messaging.Source;
import org.apache.qpid.proton.amqp.messaging.Target;
import org.apache.qpid.proton.engine.*;
import org.apache.qpid.proton.message.Message;
import org.apache.qpid.proton.message.impl.MessageImpl;
import org.apache.qpid.proton.reactor.FlowController;
import org.apache.qpid.proton.reactor.Handshaker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.UUID;

class ReactorClient extends BaseHandler {
    private static final Logger log = LoggerFactory.getLogger(ReactorClient.class);
    private final String host;
    private final int port;
    private final ProtonRequestClientOptions options;
    private final String address;
    private final ClientHandler clientHandler;
    private Connection connection;
    private Session session;
    private Receiver receiver;
    private Sender sender;
    private int nextTag = 0;

    ReactorClient(String host, int port, ProtonRequestClientOptions options, String address, ClientHandler handler) {
        add(new Handshaker());
        add(new FlowController());
        this.host = host;
        this.port = port;
        this.options = options;
        this.address = address;
        this.clientHandler = handler;
    }

    @Override
    public void onReactorInit(Event event) {
        event.getReactor().connectionToHost(host, port, this);
    }

    @Override
    public void onTransportError(Event event) {
        clientHandler.onTransportError(event.getTransport().getCondition());
    }

    @Override
    public void onConnectionBound(Event event) {
        if (options.isSslEnabled()) {
            event.getTransport().ssl(options.getSslDomain());
        }
        if (options.isSaslEnabled()) {
            event.getTransport().sasl().setMechanisms(options.getSaslMechanisms());
        }
    }

    @Override
    public void onConnectionInit(Event event) {
        connection = event.getConnection();
        connection.setContainer(options.getContainerId());
        connection.open();
    }

    @Override
    public void onConnectionRemoteOpen(Event event) {
        session = event.getConnection().session();

        sender = session.sender(options.getContainerId() + "-sender");
        if (address != null) {
            Target target = new Target();
            target.setAddress(address);
            sender.setTarget(target);
        }

        receiver = session.receiver(options.getContainerId() + "-receiver");
        Source source = new Source();
        source.setDynamic(true);
        receiver.setSource(source);

        session.open();
        sender.open();
        receiver.open();
    }

    @Override
    public void onLinkRemoteOpen(Event event) {
        Link link = event.getLink();
        if (link instanceof Receiver) {
            clientHandler.onReceiverAttached(event.getConnection().getRemoteContainer(), link.getRemoteSource().getAddress());
        }
    }

    @Override
    public void onDelivery(Event event) {
        if (event.getLink() instanceof Receiver) {
            Receiver receiver = (Receiver) event.getLink();
            Delivery delivery = receiver.current();
            if (delivery != null && delivery.isReadable() && !delivery.isPartial()) {
                int size = delivery.pending();
                byte[] data = new byte[size];
                int len = receiver.recv(data, 0, data.length);

                Message message = Proton.message();
                message.decode(data, 0, len);

                receiver.advance();
                clientHandler.onMessage(message, delivery);
            }
        }
    }


    public void sendMessage(Message message) {
        byte[] tag = String.valueOf(nextTag++).getBytes();
        sender.delivery(tag);

        int bufferSize = 1024;
        byte[] encodedMessage = new byte[bufferSize];
        MessageImpl msg = (MessageImpl) message;
        int len = msg.encode2(encodedMessage, 0, bufferSize);

        if (len > encodedMessage.length) {
            encodedMessage = new byte[len];
            msg.encode(encodedMessage, 0, len);
        }

        sender.send(encodedMessage, 0, len);
        sender.advance();
    }

    public void close() {
        if (connection != null) {
            connection.close();
        }
    }
}
