package enmasse.broker.forwarder;

import enmasse.discovery.Host;
import io.vertx.core.Vertx;
import io.vertx.proton.ProtonClient;
import io.vertx.proton.ProtonConnection;
import io.vertx.proton.ProtonDelivery;
import io.vertx.proton.ProtonLinkOptions;
import io.vertx.proton.ProtonReceiver;
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
import java.util.Optional;

/**
 * @author Ulf Lilleengen
 */
public class Forwarder {
    private static final Logger log = LoggerFactory.getLogger(Forwarder.class.getName());

    private final Vertx vertx;
    private final ProtonClient client;
    private final String address;
    private final Host from;
    private final Host to;
    private final long connectionRetryInterval;

    private volatile Optional<ProtonConnection> senderConnection = Optional.empty();
    private volatile Optional<ProtonConnection> receiverConnection = Optional.empty();

    private static Symbol replicated = Symbol.getSymbol("replicated");
    private static Symbol topic = Symbol.getSymbol("topic");

    public Forwarder(Vertx vertx, Host from, Host to, String address, long connectionRetryInterval) {
        this.vertx = vertx;
        this.client = ProtonClient.create(vertx);
        this.from = from;
        this.to = to;
        this.address = address;
        this.connectionRetryInterval = connectionRetryInterval;
    }

    public void start() {
        startSender();
    }

    private void startReceiver(ProtonSender sender, String containerId) {
        log.info("Starting receiver");
        client.connect(from.getHostname(), from.getAmqpPort(), event -> {
            if (event.succeeded()) {
                ProtonConnection connection = event.result();
                connection.setContainer(containerId);
                connection.open();
                receiverConnection = Optional.of(connection);

                Source source = new Source();
                source.setAddress(address);
                source.setCapabilities(topic);
                source.setDurable(TerminusDurability.UNSETTLED_STATE);

                ProtonReceiver receiver = connection.createReceiver(address, new ProtonLinkOptions().setLinkName(containerId));

                receiver.openHandler(handler -> {
                    log.info(this + ": receiver opened to " + connection.getRemoteContainer());
                });
                receiver.closeHandler(result -> {
                    if (result.succeeded()) {
                        log.info(this + ": receiver closed");
                    } else {
                        log.warn(this + ": receiver closed with error: " + result.cause().getMessage());
                        closeReceiver();
                        vertx.setTimer(connectionRetryInterval, timerId -> startReceiver(sender, containerId));
                    }
                });
                receiver.setSource(source);
                receiver.handler(((delivery, message) -> handleMessage(sender, delivery, message)));
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
        builder.append(from.getHostname()).append(":").append(from.getAmqpPort());
        builder.append(" -> ");
        builder.append(to.getHostname()).append(":").append(to.getAmqpPort());
        return builder.toString();
    }

    private void startSender() {
        log.info(this + ": starting sender");
        client.connect(to.getHostname(), to.getAmqpPort(), event -> {
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

    private void handleMessage(ProtonSender protonSender, ProtonDelivery protonDelivery, Message message) {
        if (log.isDebugEnabled()) {
            log.debug(this + ": forwarding message");
        }
        if (message.getAddress().equals(address) && !isMessageReplicated(message)) {
            forwardMessage(protonSender, message);
        }
    }

    private void forwardMessage(ProtonSender protonSender, Message message) {
        MessageAnnotations annotations = message.getMessageAnnotations();
        if (annotations == null) {
            annotations = new MessageAnnotations(Collections.singletonMap(replicated, true));
        } else {
            annotations.getValue().put(replicated, true);
        }
        message.setMessageAnnotations(annotations);
        protonSender.send(message);
    }

    public void stop() {
        receiverConnection.ifPresent(ProtonConnection::close);
        senderConnection.ifPresent(ProtonConnection::close);
    }

    private static boolean isMessageReplicated(Message message) {
        MessageAnnotations annotations = message.getMessageAnnotations();
        return annotations != null && annotations.getValue().containsKey(replicated);
    }
}
