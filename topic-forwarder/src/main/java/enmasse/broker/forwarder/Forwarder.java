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
import org.apache.qpid.proton.amqp.messaging.Accepted;
import org.apache.qpid.proton.amqp.messaging.MessageAnnotations;
import org.apache.qpid.proton.amqp.messaging.Source;
import org.apache.qpid.proton.amqp.messaging.Target;
import org.apache.qpid.proton.amqp.messaging.TerminusDurability;
import org.apache.qpid.proton.message.Message;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * A forwarder forwards AMQP messages from one host to another, using durable subscriptions, flow control and linked acknowledgement.
 */
public class Forwarder extends AbstractVerticle {
    private static final Logger log = LoggerFactory.getLogger(Forwarder.class.getName());

    private final Endpoint from;
    private final Endpoint to;
    private final long connectionRetryInterval;
    private Set<String> currentAddresses = new HashSet<>();

    private volatile Optional<ProtonConnection> sourceConnection = Optional.empty();
    private volatile Optional<ProtonConnection> destConnection = Optional.empty();

    private final Map<String, ProtonSender> senderMap = new ConcurrentHashMap<>();
    private final Map<String, ProtonReceiver> receiverMap = new ConcurrentHashMap<>();

    private static Symbol replicated = Symbol.getSymbol("replicated");
    private static Symbol topic = Symbol.getSymbol("topic");
    private final String certDir;
    private final String containerId;

    public Forwarder(Endpoint from, Endpoint to, long connectionRetryInterval, String certDir) {
        this.from = from;
        this.to = to;
        this.connectionRetryInterval = connectionRetryInterval;
        this.certDir = certDir;
        this.containerId = "topic-forwarder-" + from.hostname() + "-to-" + to.hostname();
    }

    @Override
    public void start() {
        ProtonClient client = ProtonClient.create(vertx);
        startSourceConnection(client);
        startDestConnection(client);
    }

    private void startSourceConnection(ProtonClient client) {
        client.connect(getOptions(), from.hostname(), from.port(), event -> {
            if (event.succeeded()) {
                ProtonConnection connection = event.result();
                connection.setContainer(containerId);
                sourceConnection = Optional.ofNullable(connection);
                connection.open();
            } else {
                log.info(this + ": source connection failed: " + event.cause().getMessage());
                vertx.setTimer(connectionRetryInterval, timerId -> startSourceConnection(client));
            }
        });
    }

    private void startDestConnection(ProtonClient client) {
        client.connect(getOptions(), to.hostname(), to.port(), event -> {
            if (event.succeeded()) {
                ProtonConnection connection = event.result();
                connection.setContainer(containerId);
                destConnection = Optional.ofNullable(connection);
                connection.open();
            } else {
                log.info(this + ": destination connection failed: " + event.cause().getMessage());
                vertx.setTimer(connectionRetryInterval, timerId -> startDestConnection(client));
            }
        });
    }

    public synchronized void onAddressesUpdated(Set<String> desiredAddresses) {
        Set<String> toAdd = new HashSet<>(desiredAddresses);
        Set<String> toRemove = new HashSet<>(currentAddresses);
        toRemove.removeAll(desiredAddresses);
        toAdd.removeAll(currentAddresses);
        for (String address : toAdd) {
            if (sourceConnection.isPresent() && destConnection.isPresent()) {
                vertx.runOnContext(t -> startSender(address));
            }
        }

        for (String address : toRemove) {
            stopLinks(address);
        }

        currentAddresses = desiredAddresses;
    }

    private void stopLinks(String address) {
        ProtonReceiver receiver = receiverMap.get(address);
        if (receiver != null) {
            vertx.runOnContext(v -> receiver.close());
        }

        ProtonSender sender = senderMap.get(address);
        if (sender != null) {
            vertx.runOnContext(v -> sender.close());
        }
    }

    private void startReceiver(ProtonSender sender, String address, String linkName) {
        log.info("Starting receiver");
        sourceConnection.ifPresent(connection -> {
            Source source = new Source();
            source.setAddress(address);
            source.setCapabilities(topic);
            source.setDurable(TerminusDurability.UNSETTLED_STATE);

            ProtonReceiver receiver = connection.createReceiver(address, new ProtonLinkOptions().setLinkName(linkName));

            receiver.setAutoAccept(false);
            receiver.openHandler(handler -> {
                receiverMap.put(address, receiver);
                log.info(this + ": receiver opened to " + connection.getRemoteContainer());
            });
            receiver.closeHandler(result -> {
                if (result.succeeded()) {
                    log.info(this + ": receiver closed");
                } else {
                    log.warn(this + ": receiver closed with error: " + result.cause().getMessage());
                    vertx.setTimer(connectionRetryInterval, timerId -> startReceiver(sender, address, containerId));
                }
            });
            receiver.setPrefetch(0);
            receiver.flow(sender.getCredit());
            receiver.setSource(source);
            receiver.handler(((delivery, message) -> handleMessage(sender, receiver, delivery, message)));
            receiver.open();
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
                    .addEnabledSaslMechanism("ANONYMOUS")
                    .setHostnameVerificationAlgorithm("")
                    .setPemTrustOptions(new PemTrustOptions()
                            .addCertPath(new File(certDir, "ca.crt").getAbsolutePath()))
                    .setPemKeyCertOptions(new PemKeyCertOptions()
                            .setCertPath(new File(certDir, "tls.crt").getAbsolutePath())
                            .setKeyPath(new File(certDir, "tls.key").getAbsolutePath()));
        }
        return options;
    }

    private void startSender(String address) {
        destConnection.ifPresent(connection -> {
            ProtonSender sender = connection.createSender(address);
            sender.openHandler(handler -> {
                log.info(this + ": sender opened to " + connection.getRemoteContainer());
                senderMap.put(address, sender);
                startReceiver(sender, address, connection.getRemoteContainer());

            });
            sender.closeHandler(result -> {
                if (result.succeeded()) {
                    log.info(this + ": sender closed");
                } else {
                    log.warn(this + ": sender closed with error: " + result.cause().getMessage());
                    vertx.setTimer(connectionRetryInterval, timerId -> startSender(address));
                }
            });

            Target target = new Target();
            target.setAddress(address);
            target.setCapabilities(topic);
            sender.setTarget(target);

            sender.open();
        });
    }

    private void handleMessage(ProtonSender protonSender, ProtonReceiver protonReceiver, ProtonDelivery protonDelivery, Message message) {
        if (log.isDebugEnabled()) {
            log.debug(this + ": forwarding message");
        }
        if (!isMessageReplicated(message)) {
            forwardMessage(protonSender, protonReceiver, protonDelivery, message);
        } else {
            protonDelivery.disposition(Accepted.getInstance(), true);
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
        sourceConnection.ifPresent(ProtonConnection::close);
        destConnection.ifPresent(ProtonConnection::close);
    }

    private static boolean isMessageReplicated(Message message) {
        MessageAnnotations annotations = message.getMessageAnnotations();
        return annotations != null && annotations.getValue().containsKey(replicated);
    }
}
