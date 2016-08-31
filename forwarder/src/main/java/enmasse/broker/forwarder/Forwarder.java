package enmasse.broker.forwarder;

import enmasse.discovery.Host;
import io.vertx.core.Vertx;
import io.vertx.proton.ProtonClient;
import io.vertx.proton.ProtonConnection;
import io.vertx.proton.ProtonDelivery;
import io.vertx.proton.ProtonLinkOptions;
import io.vertx.proton.ProtonSender;
import org.apache.qpid.proton.amqp.Symbol;
import org.apache.qpid.proton.amqp.messaging.MessageAnnotations;
import org.apache.qpid.proton.amqp.messaging.Source;
import org.apache.qpid.proton.amqp.messaging.Target;
import org.apache.qpid.proton.amqp.messaging.TerminusDurability;
import org.apache.qpid.proton.message.Message;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;

/**
 * @author Ulf Lilleengen
 */
public class Forwarder {
    private static final Logger log = LoggerFactory.getLogger(Forwarder.class.getName());

    private final Vertx vertx;
    private final ProtonClient client;
    private final String address;
    private final String containerId;
    private final Host from;
    private final Host to;
    private final long connectionRetryInterval;

    private volatile ProtonConnection senderConnection;
    private volatile ProtonConnection receiverConnection;
    private volatile ProtonSender sender;

    private static Symbol replicated = Symbol.getSymbol("replicated");
    private static Symbol topic = Symbol.getSymbol("topic");

    public Forwarder(Vertx vertx, Host from, Host to, String address, String containerId, long connectionRetryInterval) {
        this.vertx = vertx;
        this.client = ProtonClient.create(vertx);
        this.from = from;
        this.to = to;
        this.address = address;
        this.containerId = containerId;
        this.connectionRetryInterval = connectionRetryInterval;
    }

    public void start() {
        startReceiver();
        startSender();
    }

    private void startReceiver() {
        client.connect(from.getHostname(), from.getAmqpPort(), event -> {
            if (event.succeeded()) {
                ProtonConnection connection = event.result();
                connection.setContainer(containerId);
                connection.open();
                Source source = new Source();
                source.setAddress(address);
                source.setCapabilities(topic);
                source.setDurable(TerminusDurability.UNSETTLED_STATE);

                connection.createReceiver(address, new ProtonLinkOptions().setLinkName(containerId))
                        .openHandler(handler -> {
                            log.info(this + ": receiver opened");
                        })
                        .closeHandler(result -> {
                            if (result.succeeded()) {
                                log.info(this + ": receiver closed");
                            } else {
                                log.warn(this + ": receiver closed with error: " + result.cause().getMessage());
                                connection.close();
                                vertx.setTimer(connectionRetryInterval, timerId -> startReceiver());
                            }
                        })
                        .setSource(source)
                        .handler(this::handleMessage)
                        .open();
                this.receiverConnection = connection;
            } else {
                log.info(this + ": connection failed, retrying: " + event.cause().getMessage());
                vertx.setTimer(connectionRetryInterval, timerId -> startReceiver());
            }
        });
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append(from.getHostname()).append(":").append(from.getAmqpPort());
        builder.append(" -> ");
        builder.append(to.getHostname()).append(":").append(to.getAmqpPort());
        return builder.toString();
    }

    private void startSender() {
        client.connect(to.getHostname(), to.getAmqpPort(), event -> {
            if (event.succeeded()) {
                ProtonConnection connection = event.result();
                connection.setContainer("forwarder-" + from.getHostname());
                connection.open();
                ProtonSender sender = connection.createSender(address)
                        .openHandler(handler -> {
                            log.info(this + ": sender opened");
                        })
                        .closeHandler(result -> {
                            if (result.succeeded()) {
                                log.info(this + ": sender closed");
                            } else {
                                log.warn(this + ": sender closed with error: " + result.cause().getMessage());
                                vertx.setTimer(connectionRetryInterval, timerId -> startSender());
                            }})
                        .open();

                Target target = new Target();
                target.setAddress(address);
                target.setCapabilities(topic);
                sender.setTarget(target);
                this.sender = sender;
                this.senderConnection = connection;
            } else {
                log.info(this + ": connection failed: " + event.cause().getMessage());
                vertx.setTimer(connectionRetryInterval, timerId -> startSender());
            }
        });
    }

    private void handleMessage(ProtonDelivery protonDelivery, Message message) {
        if (log.isDebugEnabled()) {
            log.debug(this + ": forwarding message");
        }
        if (message.getAddress().equals(address) && !isMessageReplicated(message)) {
            forwardMessage(message);
        }
    }

    private void forwardMessage(Message message) {
        if (sender == null) {
            log.debug(this + ": received message, but sender is not yet created");
        }
        MessageAnnotations annotations = message.getMessageAnnotations();
        if (annotations == null) {
            annotations = new MessageAnnotations(Collections.singletonMap(replicated, true));
        } else {
            annotations.getValue().put(replicated, true);
        }
        message.setMessageAnnotations(annotations);
        sender.send(message);
    }

    public void stop() {
        receiverConnection.close();
        senderConnection.close();
    }

    private static boolean isMessageReplicated(Message message) {
        MessageAnnotations annotations = message.getMessageAnnotations();
        return annotations != null && annotations.getValue().containsKey(replicated);
    }
}
