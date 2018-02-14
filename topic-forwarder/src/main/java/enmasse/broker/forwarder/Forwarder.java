/*
 * Copyright 2016-2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package enmasse.broker.forwarder;

import enmasse.discovery.Endpoint;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.net.PemKeyCertOptions;
import io.vertx.core.net.PemTrustOptions;
import io.vertx.proton.*;
import org.apache.qpid.proton.amqp.Symbol;
import org.apache.qpid.proton.amqp.messaging.MessageAnnotations;
import org.apache.qpid.proton.amqp.messaging.Source;
import org.apache.qpid.proton.amqp.messaging.Target;
import org.apache.qpid.proton.amqp.messaging.TerminusDurability;
import org.apache.qpid.proton.message.Message;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.Collections;
import java.util.Optional;

/**
 * A forwarder forwards AMQP messages from one host to another, using durable subscriptions, flow control and linked acknowledgement.
 */
public class Forwarder extends AbstractVerticle {
    private static final Logger log = LoggerFactory.getLogger(Forwarder.class.getName());

    private final String address;
    private final Endpoint from;
    private final Endpoint to;
    private final long connectionRetryInterval;

    private volatile Optional<ProtonConnection> senderConnection = Optional.empty();
    private volatile Optional<ProtonConnection> receiverConnection = Optional.empty();

    private static Symbol replicated = Symbol.getSymbol("replicated");
    private static Symbol topic = Symbol.getSymbol("topic");
    private final String certDir;

    public Forwarder(Endpoint from, Endpoint to, String address, long connectionRetryInterval, String certDir) {
        this.from = from;
        this.to = to;
        this.address = address;
        this.connectionRetryInterval = connectionRetryInterval;
        this.certDir = certDir;
    }

    @Override
    public void start() {
        startSender();
    }

    private void startReceiver(ProtonSender sender, String containerId) {
        log.info("Starting receiver");
        ProtonClient client = ProtonClient.create(vertx);
        client.connect(getOptions(), from.hostname(), from.port(), event -> {
            if (event.succeeded()) {
                ProtonConnection connection = event.result();
                connection.setContainer("topic-forwarder-" + containerId);
                connection.open();
                receiverConnection = Optional.of(connection);

                Source source = new Source();
                source.setAddress(address);
                source.setCapabilities(topic);
                source.setDurable(TerminusDurability.UNSETTLED_STATE);

                ProtonReceiver receiver = connection.createReceiver(address, new ProtonLinkOptions().setLinkName(containerId));

                receiver.setAutoAccept(false);
                receiver.openHandler(handler -> {
                    log.info(this + ": receiver opened to " + connection.getRemoteContainer());
                });
                receiver.closeHandler(result -> {
                    if (result.succeeded()) {
                        log.info(this + ": receiver closed");
                        closeReceiver();
                    } else {
                        log.warn(this + ": receiver closed with error: " + result.cause().getMessage());
                        closeReceiver();
                        vertx.setTimer(connectionRetryInterval, timerId -> startReceiver(sender, containerId));
                    }
                });
                receiver.setPrefetch(0);
                receiver.flow(sender.getCredit());
                receiver.setSource(source);
                receiver.handler(((delivery, message) -> handleMessage(sender, receiver, delivery, message)));
                receiver.open();
            } else {
                log.info(this + ": connection failed, retrying: " + event.cause().getMessage());
                vertx.setTimer(connectionRetryInterval, timerId -> startReceiver(sender, containerId));
            }
        });
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append(from.hostname()).append(":").append(from.port());
        builder.append(" -> ");
        builder.append(to.hostname()).append(":").append(to.port());
        return builder.toString();
    }

    private ProtonClientOptions getOptions() {
        ProtonClientOptions options = new ProtonClientOptions();
        if (certDir != null) {
            options.setHostnameVerificationAlgorithm("")
                    .setSsl(true)
                    .addEnabledSaslMechanism("EXTERNAL")
                    .setHostnameVerificationAlgorithm("")
                    .setPemTrustOptions(new PemTrustOptions()
                            .addCertPath(new File(certDir, "ca.crt").getAbsolutePath()))
                    .setPemKeyCertOptions(new PemKeyCertOptions()
                            .setCertPath(new File(certDir, "tls.crt").getAbsolutePath())
                            .setKeyPath(new File(certDir, "tls.key").getAbsolutePath()));
        }
        return options;
    }

    private void startSender() {
        ProtonClient client = ProtonClient.create(vertx);
        log.info(this + ": starting sender");
        client.connect(getOptions(), to.hostname(), to.port(), event -> {
            if (event.succeeded()) {
                ProtonConnection connection = event.result();
                connection.open();
                senderConnection = Optional.of(connection);
                ProtonSender sender = connection.createSender(address);
                sender.openHandler(handler -> {
                    log.info(this + ": sender opened to " + connection.getRemoteContainer());
                    startReceiver(sender, connection.getRemoteContainer());

                });
                sender.closeHandler(result -> {
                    if (result.succeeded()) {
                        log.info(this + ": sender closed");
                        closeReceiver();
                    } else {
                        closeReceiver();
                        log.warn(this + ": sender closed with error: " + result.cause().getMessage());
                        vertx.setTimer(connectionRetryInterval, timerId -> startSender());
                    }
                });

                Target target = new Target();
                target.setAddress(address);
                target.setCapabilities(topic);
                sender.setTarget(target);

                sender.open();
            } else {
                closeReceiver();
                log.info(this + ": connection failed: " + event.cause().getMessage());
                vertx.setTimer(connectionRetryInterval, timerId -> startSender());
            }
        });
    }

    private void closeReceiver() {
        receiverConnection.ifPresent(ProtonConnection::close);
    }

    private void handleMessage(ProtonSender protonSender, ProtonReceiver protonReceiver, ProtonDelivery protonDelivery, Message message) {
        if (log.isDebugEnabled()) {
            log.debug(this + ": forwarding message");
        }
        if (!isMessageReplicated(message)) {
            forwardMessage(protonSender, protonReceiver, protonDelivery, message);
        }
    }

    private void forwardMessage(ProtonSender protonSender, ProtonReceiver protonReceiver, ProtonDelivery sourceDelivery, Message message) {
        MessageAnnotations annotations = message.getMessageAnnotations();
        if (annotations == null) {
            annotations = new MessageAnnotations(Collections.singletonMap(replicated, true));
        } else {
            annotations.getValue().put(replicated, true);
        }
        message.setMessageAnnotations(annotations);
        protonSender.send(message, protonDelivery -> {
            sourceDelivery.disposition(protonDelivery.getRemoteState(), protonDelivery.remotelySettled());
            protonReceiver.flow(protonSender.getCredit() - protonReceiver.getCredit());
        });
    }

    @Override
    public void stop() {
        receiverConnection.ifPresent(ProtonConnection::close);
        senderConnection.ifPresent(ProtonConnection::close);
    }

    private static boolean isMessageReplicated(Message message) {
        MessageAnnotations annotations = message.getMessageAnnotations();
        return annotations != null && annotations.getValue().containsKey(replicated);
    }
}
