package enmasse.systemtest.amqp;

import io.vertx.proton.ProtonConnection;
import io.vertx.proton.ProtonDelivery;
import io.vertx.proton.ProtonMessageHandler;
import io.vertx.proton.ProtonReceiver;
import org.apache.qpid.proton.Proton;
import org.apache.qpid.proton.amqp.Symbol;
import org.apache.qpid.proton.amqp.messaging.Accepted;
import org.apache.qpid.proton.amqp.messaging.AmqpValue;
import org.apache.qpid.proton.amqp.messaging.Source;
import org.apache.qpid.proton.engine.*;
import org.apache.qpid.proton.message.Message;
import org.apache.qpid.proton.reactor.FlowController;
import org.apache.qpid.proton.reactor.Handshaker;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.function.Predicate;

public class Receiver extends ClientHandlerBase<List<String>> {
    private static final Symbol AMQP_LINK_REDIRECT = Symbol.valueOf("amqp:link:redirect");

    private final List<String> messages = new ArrayList<>();
    private final Predicate<Message> done;

    public Receiver(enmasse.systemtest.Endpoint endpoint, Predicate<Message> done, CompletableFuture<List<String>> promise, ClientOptions clientOptions, CountDownLatch connectLatch) {
        super(endpoint, clientOptions, promise);
        this.done = done;
    }

    @Override
    protected void connectionOpened(ProtonConnection conn) {
        connectionOpened(conn, clientOptions.getLinkName().orElse(clientOptions.getSource().getAddress()), clientOptions.getSource());
    }

    private void connectionOpened(ProtonConnection conn, String linkName, Source source) {
        ProtonReceiver receiver = conn.createReceiver(linkName);
        receiver.setSource(source);
        receiver.setPrefetch(0);
        receiver.handler((protonDelivery, message) -> {
            messages.add((String) ((AmqpValue) message.getBody()).getValue());
            if (done.test(message)) {
                promise.complete(messages);
            } else {
                receiver.flow(1);
            }
        });
        receiver.openHandler(result -> {
            receiver.flow(1);
        });

        receiver.closeHandler(closed -> {
            if (closed.succeeded()) {
                if (receiver.getRemoteCondition() != null && AMQP_LINK_REDIRECT.equals(receiver.getRemoteCondition().getCondition())) {
                    String relocated = (String) receiver.getRemoteCondition().getInfo().get("address");
                    System.out.println("Receiver link redirected to " + relocated);
                    Source newSource = clientOptions.getSource();
                    newSource.setAddress(relocated);
                    String newLinkName = clientOptions.getLinkName().orElse(newSource.getAddress());

                    vertx.setTimer(0, id -> {
                        connectionOpened(conn, newLinkName, newSource);
                    });
                }
            } else {
                handleError(receiver.getRemoteCondition());
            }
            receiver.close();
        });
        receiver.open();
    }

    @Override
    protected void connectionClosed(ProtonConnection conn) {
        promise.complete(messages);
    }

    @Override
    protected void connectionDisconnected(ProtonConnection conn) {
        promise.complete(messages);
    }
}
