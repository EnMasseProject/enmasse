package enmasse.systemtest.amqp;

import org.apache.qpid.proton.Proton;
import org.apache.qpid.proton.amqp.Symbol;
import org.apache.qpid.proton.amqp.messaging.Accepted;
import org.apache.qpid.proton.amqp.messaging.AmqpValue;
import org.apache.qpid.proton.amqp.messaging.Source;
import org.apache.qpid.proton.amqp.transport.DeliveryState;
import org.apache.qpid.proton.engine.*;
import org.apache.qpid.proton.message.Message;
import org.apache.qpid.proton.reactor.FlowController;
import org.apache.qpid.proton.reactor.Handshaker;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.function.Predicate;

public class ReceiveHandler extends ClientHandlerBase {
    private static final Symbol AMQP_LINK_REDIRECT = Symbol.valueOf("amqp:link:redirect");

    private final List<String> messages = new ArrayList<>();
    private final CompletableFuture<List<String>> promise;
    private final Predicate<Message> done;

    protected void openLink(Session session) {
        Source source = clientOptions.getSource();
        Receiver receiver = session.receiver(source.getAddress());
        receiver.setSource(source);

        receiver.open();
    }

    public ReceiveHandler(enmasse.systemtest.Endpoint endpoint, Predicate<Message> done, CompletableFuture<List<String>> promise, ClientOptions clientOptions, CountDownLatch connectLatch) {
        super(endpoint, clientOptions, connectLatch);
        add(new Handshaker());
        add(new FlowController());
        this.done = done;
        this.promise = promise;
    }


    protected void reportException(Exception e) {
        promise.completeExceptionally(e);
    }

    @Override
    public void onLinkRemoteClose(Event event) {
        Link link = event.getLink();
        if (link.getCondition() != null && AMQP_LINK_REDIRECT.equals(link.getCondition().getCondition())) {
            String relocated = (String) link.getCondition().getInfo().get("address");
            System.out.println("Receiver link redirected to " + relocated);
            Source newSource = clientOptions.getSource();
            newSource.setAddress(relocated);

            Receiver receiver = event.getSession().receiver(relocated);
            receiver.setSource(newSource);
            receiver.open();
        } else {
            link.close();
            handleError(link.getCondition());
        }
    }


    @Override
    public void onDelivery(Event event) {
        Receiver receiver = (Receiver) event.getLink();
        Delivery delivery = receiver.current();
        if (delivery != null && delivery.isReadable() && !delivery.isPartial()) {
            int size = delivery.pending();
            byte[] buffer = new byte[size];
            int read = receiver.recv(buffer, 0, buffer.length);
            receiver.advance();

            Message message = Proton.message();
            message.decode(buffer, 0, read);
            messages.add((String) ((AmqpValue) message.getBody()).getValue());

            delivery.disposition(Accepted.getInstance());
            delivery.settle();
            if (done.test(message)) {
                event.getConnection().close();
                promise.complete(messages);
            }
        }
    }
}
