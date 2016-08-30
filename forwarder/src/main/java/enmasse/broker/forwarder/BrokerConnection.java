package enmasse.broker.forwarder;

import io.vertx.proton.ProtonConnection;
import io.vertx.proton.ProtonSender;
import io.vertx.proton.ProtonSession;
import org.apache.qpid.proton.amqp.Symbol;
import org.apache.qpid.proton.amqp.messaging.MessageAnnotations;
import org.apache.qpid.proton.message.Message;

import java.util.Collections;

/**
 * @author Ulf Lilleengen
 */
public class BrokerConnection {
    private final ProtonConnection connection;
    private final ProtonSession session;
    private final ProtonSender sender;

    public BrokerConnection(ProtonConnection connection, ProtonSession session, ProtonSender sender) {
        this.connection = connection;
        this.session = session;
        this.sender = sender;
    }

    public void forwardMessage(Message message, String forwardAddress) {
        Message forwardedMessage = Message.Factory.create();

        System.out.println("Forwarding message");
        forwardedMessage.setAddress(forwardAddress);
        forwardedMessage.setBody(message.getBody());
        forwardedMessage.setMessageAnnotations(new MessageAnnotations(Collections.singletonMap(Symbol.getSymbol("replicated"), true)));

        sender.send(message);
    }

    public void close() {
        sender.close();
        session.close();
        connection.close();
    }

    public static boolean isMessageReplicated(Message message) {
        MessageAnnotations annotations = message.getMessageAnnotations();
        return annotations != null && annotations.getValue().containsKey(Symbol.getSymbol("replicated"));
    }
}
