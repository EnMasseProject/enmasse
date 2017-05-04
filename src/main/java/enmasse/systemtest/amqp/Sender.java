package enmasse.systemtest.amqp;

import enmasse.systemtest.Logging;
import io.vertx.proton.ProtonConnection;
import io.vertx.proton.ProtonQoS;
import io.vertx.proton.ProtonSender;
import org.apache.qpid.proton.amqp.messaging.Accepted;
import org.apache.qpid.proton.message.Message;

import java.util.Queue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;

class Sender extends ClientHandlerBase<Integer> {
    private final AtomicInteger numSent = new AtomicInteger(0);
    private final Queue<Message> messageQueue;
    private final ProtonQoS qos;
    private final CountDownLatch connectLatch;

    public Sender(enmasse.systemtest.Endpoint endpoint, ClientOptions clientOptions, CountDownLatch connectLatch, CompletableFuture<Integer> promise, Queue<Message> messages, ProtonQoS qos) {
        super(endpoint, clientOptions, promise);
        this.messageQueue = messages;
        this.qos = qos;
        this.connectLatch = connectLatch;
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
        sender.closeHandler(result -> {
            handleError(connection, sender.getCondition());
        });
        sender.open();
    }

    @Override
    protected void connectionClosed(ProtonConnection conn) {
        conn.close();
        promise.complete(numSent.get());
    }

    @Override
    protected void connectionDisconnected(ProtonConnection conn) {
        conn.close();
        promise.complete(numSent.get());
    }

    private void sendNext(ProtonConnection connection, ProtonSender sender) {
        Message message = messageQueue.poll();

        if (message == null) {
            connection.close();
            promise.complete(numSent.get());
        } else {
            if (sender.getQoS().equals(ProtonQoS.AT_MOST_ONCE)) {
                sender.send(message);
                numSent.incrementAndGet();
                vertx.runOnContext(id -> sendNext(connection, sender));
            } else {
                sender.send(message, protonDelivery -> {
                    if (protonDelivery.getRemoteState().equals(Accepted.getInstance())) {
                        numSent.incrementAndGet();
                        sendNext(connection, sender);
                    } else {
                        connection.close();
                        promise.complete(numSent.get());
                    }
                });
            }
        }
    }
}
