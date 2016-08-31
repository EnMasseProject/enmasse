package enmasse.broker.forwarder;

import io.vertx.proton.ProtonConnection;
import io.vertx.proton.ProtonSender;
import io.vertx.proton.ProtonSession;
import org.apache.qpid.proton.amqp.Symbol;
import org.apache.qpid.proton.amqp.messaging.ApplicationProperties;
import org.apache.qpid.proton.amqp.messaging.MessageAnnotations;
import org.apache.qpid.proton.message.Message;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;

/**
 * @author Ulf Lilleengen
 */
public class BrokerConnection implements ReplicatedBroker {
    private final ProtonConnection connection;
    private final ProtonSession session;
    private final ProtonSender sender;
    private static final Logger log = LoggerFactory.getLogger(BrokerConnection.class.getName());
    private static Symbol replicated = Symbol.getSymbol("replicated");

    public BrokerConnection(ProtonConnection connection, ProtonSession session, ProtonSender sender) {
        this.connection = connection;
        this.session = session;
        this.sender = sender;
    }

    public void forwardMessage(Message message, String forwardAddress) {
        Message forwardedMessage = Message.Factory.create();

        forwardedMessage.setAddress(forwardAddress);
        forwardedMessage.setBody(message.getBody());
        forwardedMessage.setMessageAnnotations(new MessageAnnotations(Collections.singletonMap(replicated, true)));

        sender.send(forwardedMessage);
    }

    public void close() {
        sender.close();
        session.close();
        connection.close();
    }

    public static boolean isMessageReplicated(Message message) {
        MessageAnnotations annotations = message.getMessageAnnotations();
        if (log.isDebugEnabled()) {
            log.debug("Annotations: " + annotations);
        }
        return annotations != null && annotations.getValue().containsKey(replicated);
    }
}
