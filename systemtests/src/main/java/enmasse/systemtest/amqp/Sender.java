package enmasse.systemtest.amqp;

import enmasse.systemtest.Endpoint;
import enmasse.systemtest.Logging;
import io.vertx.proton.ProtonConnection;
import io.vertx.proton.ProtonQoS;
import io.vertx.proton.ProtonSender;
import org.apache.qpid.proton.amqp.messaging.Accepted;
import org.apache.qpid.proton.message.Message;

import java.util.Iterator;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Predicate;

class Sender extends ClientHandlerBase<Integer> {
    private final AtomicInteger numSent = new AtomicInteger(0);
    private final Iterator<Message> messageQueue;
    private final ProtonQoS qos;
    private final CountDownLatch connectLatch;
    private final Predicate<Message> predicate;

    public Sender(Endpoint endpoint,
                  ClientOptions clientOptions,
                  CountDownLatch connectLatch,
                  CompletableFuture<Integer> promise,
                  Iterable<Message> messages,
                  ProtonQoS qos,
                  final Predicate<Message> predicate) {
        super(endpoint, clientOptions, promise);
        this.messageQueue = messages.iterator();
        this.qos = qos;
        this.connectLatch = connectLatch;
        this.predicate = predicate;
    }

    @Override
    public void connectionOpened(ProtonConnection connection) {
        ProtonSender sender = connection.createSender(clientOptions.getTarget().getAddress());
        sender.setTarget(clientOptions.getTarget());
        sender.setQoS(qos);
        sender.openHandler(result -> {
            Logging.log.info("Sender link " + sender.getTarget().getAddress() + " opened, sending messages");
            connectLatch.countDown();
            sendNext(connection, sender);
        });
        sender.closeHandler(result -> handleError(connection, sender.getCondition()));
        sender.open();
    }

    @Override
    protected void connectionClosed(ProtonConnection conn) {
        conn.close();
        if(!promise.isDone()) {
            promise.completeExceptionally(new RuntimeException("Connection closed after " + numSent.get() + " messages sent"));
        }
    }

    @Override
    protected void connectionDisconnected(ProtonConnection conn) {
        conn.close();
        if(!promise.isDone()) {
            promise.completeExceptionally(new RuntimeException("Connection disconnected after " + numSent.get() + " messages sent"));
        }
    }

    private void sendNext(ProtonConnection connection, ProtonSender sender) {

        Message message;
        if (messageQueue.hasNext() && (message = messageQueue.next()) != null) {
            if (sender.getQoS().equals(ProtonQoS.AT_MOST_ONCE)) {
                sender.send(message);
                numSent.incrementAndGet();
                if(predicate.test(message)) {
                    promise.complete(numSent.get());
                } else {
                    vertx.runOnContext(id -> sendNext(connection, sender));
                }
            } else {
                sender.send(message, protonDelivery -> {
                    if (protonDelivery.getRemoteState().equals(Accepted.getInstance())) {
                        numSent.incrementAndGet();
                        if(predicate.test(message)) {
                            promise.complete(numSent.get());
                            connection.close();
                        } else {
                            sendNext(connection, sender);
                        }
                    } else {
                        promise.completeExceptionally(new IllegalStateException("Message not accepted (remote state: "+protonDelivery.getRemoteState()+") after " + numSent.get() + " messages sent"));
                        connection.close();
                    }
                });
            }
        } else {
            if(predicate.test(null)) {
                promise.complete(numSent.get());
            } else {
                promise.completeExceptionally(new RuntimeException("No more messages to send after + " + numSent.get() + " messages sent"));
            }
            connection.close();
        }
    }
}
