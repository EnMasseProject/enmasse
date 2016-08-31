package enmasse.broker.forwarder;

import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.proton.ProtonSender;
import io.vertx.proton.ProtonServer;
import io.vertx.proton.ProtonSession;
import org.apache.qpid.proton.amqp.messaging.AmqpValue;
import org.apache.qpid.proton.message.Message;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;

import static jdk.nashorn.internal.runtime.regexp.joni.Config.log;

/**
 * @author Ulf Lilleengen
 */
public class TestBroker {
    private static final Logger log = LoggerFactory.getLogger(TestBroker.class.getName());
    private final Map<String, BlockingQueue<String>> queueMap = new HashMap<>();
    private final AtomicInteger numConnected = new AtomicInteger(0);
    private final String host;
    private final String address;
    private final int port;
    private final Vertx vertx;

    TestBroker(Vertx vertx, String host, int port, String address) {
        this.vertx = vertx;
        this.host = host;
        this.port = port;
        this.address = address;
    }

    public void start() {
        ProtonServer server = ProtonServer.create(vertx);
        server.connectHandler(connection -> {
            connection.setContainer("server-" + port);
            connection.openHandler(conn -> {
                log.info(port + " connection opened from + " + connection.getRemoteContainer());
            }).closeHandler(conn -> {
                connection.close();
                connection.disconnect();
                log.info(port + " connection closed");
            }).disconnectHandler(protonConnection -> {
                connection.disconnect();
                log.info(port + " disconnected");
            }).open();

            numConnected.incrementAndGet();

            connection.sessionOpenHandler(ProtonSession::open);

            connection.receiverOpenHandler(receiver -> {
                receiver.handler((delivery, message) -> {
                    vertx.executeBlocking(future -> {
                        try {
                            synchronized (queueMap) {
                                for (Map.Entry<String, BlockingQueue<String>> queue : queueMap.entrySet()) {
                                    queue.getValue().put((String) ((AmqpValue) message.getBody()).getValue());
                                }
                            }
                            future.complete();
                        } catch (Exception e) {
                            future.fail(e);
                        }
                    }, false, result -> {
                    });
                }).open();
            });
            connection.senderOpenHandler(sender -> {
                sender.open();
                BlockingQueue<String> queue = null;
                synchronized (queueMap) {
                    if (!queueMap.containsKey(connection.getRemoteContainer())) {
                        queueMap.put(connection.getRemoteContainer(), new LinkedBlockingQueue<>());
                    }
                    queue = queueMap.get(connection.getRemoteContainer());
                }
                PollHandler pollHandler = new PollHandler(queue);
                vertx.executeBlocking(pollHandler, false, new ForwardHandler(pollHandler, sender));
            });
        }).listen(port, host);
    }

    public int numConnected() {
        return numConnected.get();
    }
        public static class PollHandler implements Handler<Future<String>> {
        private final BlockingQueue<String> queue;
        public PollHandler(BlockingQueue<String> queue) {
            this.queue = queue;
        }

        @Override
        public void handle(Future<String> event) {
            try {
                event.complete(queue.take());
            } catch (Exception e) {
                event.fail(e);
            }
        }
    }

    private class ForwardHandler implements Handler<AsyncResult<String>> {
        private final ProtonSender sender;
        private final PollHandler pollHandler;

        private ForwardHandler(PollHandler pollHandler, ProtonSender sender) {
            this.pollHandler = pollHandler;
            this.sender = sender;
        }

        @Override
        public void handle(AsyncResult<String> event) {
            Message message = Message.Factory.create();
            message.setBody(new AmqpValue(event.result()));
            message.setAddress(address);
            sender.send(message);
            vertx.executeBlocking(pollHandler, false, this);
        }
    }
}
