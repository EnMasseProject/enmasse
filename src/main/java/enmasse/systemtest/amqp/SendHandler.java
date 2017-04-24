package enmasse.systemtest.amqp;

import io.vertx.proton.ProtonQoS;
import org.apache.qpid.proton.amqp.messaging.Accepted;
import org.apache.qpid.proton.engine.*;
import org.apache.qpid.proton.message.Message;
import org.apache.qpid.proton.message.impl.MessageImpl;
import org.apache.qpid.proton.reactor.FlowController;

import java.util.Queue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * TODO: Description
 */
class SendHandler extends ClientHandlerBase {
    private final CompletableFuture<Integer> promise;
    private final AtomicInteger numSent = new AtomicInteger(0);
    private final Queue<Message> messageQueue;
    private final int numToSend;
    private final boolean presettle;

    public SendHandler(enmasse.systemtest.Endpoint endpoint, ClientOptions clientOptions, CountDownLatch connectLatch, CompletableFuture<Integer> promise, Queue<Message> messages, ProtonQoS qos) {
        super(endpoint, clientOptions, connectLatch);
        add(new FlowController());
        this.promise = promise;
        this.messageQueue = messages;
        this.numToSend = messages.size();
        this.presettle = qos.equals(ProtonQoS.AT_MOST_ONCE);
    }

    @Override
    protected void openLink(Session session) {
        Sender sender = session.sender(clientOptions.getTarget().getAddress());
        sender.setTarget(clientOptions.getTarget());
        sender.open();
    }

    @Override
    public void onLinkRemoteClose(Event event) {
        Link link = event.getLink();
        handleError(link.getCondition());
    }

    @Override
    protected void reportException(Exception e) {
        promise.completeExceptionally(e);
    }

    @Override
    public void onDelivery(Event event) {
        Delivery delivery = event.getDelivery();
        if (delivery.remotelySettled()) {
            delivery.settle();
            if (delivery.getRemoteState().equals(Accepted.getInstance())) {
                if (numSent.incrementAndGet() == numToSend) {
                    promise.complete(numSent.get());
                }
            } else {
                event.getConnection().close();
                promise.complete(numSent.get());
            }
        }
    }

    @Override
    public void onLinkFlow(Event event) {
        Connection connection = event.getConnection();
        Sender sender = event.getSender();
        if (sender.getCredit() > 0) {
            Message message = messageQueue.poll();

            if (message != null) {
                Delivery delivery = sender.delivery(String.valueOf(numSent.get()).getBytes());

                if (presettle) {
                    delivery.settle();
                }
                int BUFFER_SIZE = 1024;
                byte encodedMessage[] = new byte[BUFFER_SIZE];
                MessageImpl msg = (MessageImpl) message;
                int len = msg.encode2(encodedMessage, 0, BUFFER_SIZE);

                // looks like the message is bigger than our initial buffer, lets resize and try again.
                if (len > encodedMessage.length) {
                    encodedMessage = new byte[len];
                    msg.encode(encodedMessage, 0, len);
                }
                sender.send(encodedMessage, 0, len);

                sender.advance();
                if (presettle) {
                    if (numSent.incrementAndGet() == numToSend) {
                        promise.complete(numToSend);
                    }
                }
            }
        }
    }
}
