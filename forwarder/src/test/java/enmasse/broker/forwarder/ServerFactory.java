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
import org.slf4j.event.Level;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author Ulf Lilleengen
 */
public class ServerFactory {
    private final Vertx vertx;
    private final String localHost;
    private final String address;
    private final Logger log = LoggerFactory.getLogger(BrokerReplicatorTest.class.getName());

    public ServerFactory(Vertx vertx, String localHost, String address) {
        this.vertx = vertx;
        this.localHost = localHost;
        this.address = address;
    }

    public TestServer startServer(int port) {
        AtomicInteger numConnected = new AtomicInteger(0);
        BlockingQueue<String> queue = new LinkedBlockingQueue<>();
        ProtonServer server = ProtonServer.create(vertx);
        server.connectHandler(connection -> {
            System.out.println("new incoming connection for " + port);
            connection.setContainer("server-" + port);
            connection.openHandler(conn -> {
                log.info(port + " connection opened");
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
                log.info("Opened receiver on " + port);
                receiver.handler((delivery, message) -> {
                    vertx.executeBlocking(future -> {
                        try {
                            queue.put((String)((AmqpValue)message.getBody()).getValue());
                            future.complete();
                        } catch (Exception e) {
                            future.fail(e);
                        }}, false, result -> {});
                }).open();
            });
            connection.senderOpenHandler(sender -> {
                sender.open();
                log.info("Opened sender on " + port);
                PollHandler pollHandler = new PollHandler(queue);
                vertx.executeBlocking(pollHandler, false, new ForwardHandler(pollHandler, sender));
            });
        }).listen(port, localHost);
        log.info("Created server " + port);
        return new TestServer(server, numConnected);
    }

    public static class TestServer {
        private final ProtonServer server;
        private final AtomicInteger numConnected;

        private TestServer(ProtonServer server, AtomicInteger numConnected) {
            this.server = server;
            this.numConnected = numConnected;
        }

        public int numConnected() {
            return numConnected.get();
        }
    }

    public class PollHandler implements Handler<Future<String>> {
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
