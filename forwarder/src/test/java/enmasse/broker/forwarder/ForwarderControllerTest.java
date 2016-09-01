package enmasse.broker.forwarder;

import enmasse.discovery.Host;
import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.VertxOptions;
import io.vertx.proton.ProtonClient;
import io.vertx.proton.ProtonConnection;
import io.vertx.proton.ProtonSender;
import org.apache.qpid.proton.amqp.Symbol;
import org.apache.qpid.proton.amqp.messaging.AmqpValue;
import org.apache.qpid.proton.amqp.messaging.Source;
import org.apache.qpid.proton.amqp.messaging.Target;
import org.apache.qpid.proton.message.Message;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

/**
 * @author Ulf Lilleengen
 */
public class ForwarderControllerTest {
    private static final Logger log = LoggerFactory.getLogger(ForwarderControllerTest.class.getName());
    private Vertx vertx = Vertx.vertx(new VertxOptions().setWorkerPoolSize(10));
    private String localHost = "127.0.0.1";
    private final String address = "mytopic";
    private TestBroker serverA = new TestBroker(localHost, 5672, address);
    private TestBroker serverB = new TestBroker(localHost, 5673, address);
    private TestBroker serverC = new TestBroker(localHost, 5674, address);

    @Before
    public void setup() throws Exception {
        serverA.start();
        serverB.start();
        serverC.start();
    }

    @After
    public void teardown() {
        vertx.close();
    }

    @Test
    public void testBrokerReplicator() throws InterruptedException, TimeoutException, ExecutionException {
        Host hostA = new Host(localHost, Collections.singletonMap("amqp", 5672));
        Host hostB = new Host(localHost, Collections.singletonMap("amqp", 5673));
        Host hostC = new Host(localHost, Collections.singletonMap("amqp", 5674));

        ForwarderController replicator = new ForwarderController(hostA, address);

        Set<Host> hosts = new LinkedHashSet<>();
        hosts.add(hostB);
        replicator.hostsChanged(hosts);
        Thread.sleep(5000);
        hosts.add(hostC);
        replicator.hostsChanged(hosts);

        long timeout = 60_000;
        waitForConnections(serverA, 2, timeout);
        waitForConnections(serverB, 1, timeout);
        waitForConnections(serverB, 1, timeout);

        CompletableFuture<List<String>> resultA = new CompletableFuture<>();
        CompletableFuture<List<String>> resultB = new CompletableFuture<>();

        CountDownLatch latch = new CountDownLatch(2);
        ProtonClient.create(vertx).connect(localHost, 5673, new TestHandler(latch, resultA, 2));
        ProtonClient.create(vertx).connect(localHost, 5674, new TestHandler(latch, resultB, 2));
        latch.await(20, TimeUnit.SECONDS);

        sendMessageTo(5672, "Hello 1");
        sendMessageTo(5672, "Hello 2");

        assertMessages(resultA.get(120, TimeUnit.SECONDS), "Hello 1", "Hello 2");
        assertMessages(resultB.get(120, TimeUnit.SECONDS), "Hello 1", "Hello 2");
        vertx.close();
    }

    private void assertMessages(List<String> result, String...messages) {
        assertThat(messages.length, is(2));
        for (String message : messages) {
            assertThat(result, hasItem(message));
        }
    }

    private void sendMessageTo(int port, String body) {
        ProtonClient client = ProtonClient.create(vertx);
        client.connect(localHost, port, event -> {
            ProtonConnection connection = event.result().open();
            Target target = new Target();
            target.setAddress(address);
            target.setCapabilities(Symbol.getSymbol("topic"));
            ProtonSender sender = connection.createSender(address);
            sender.setTarget(target);
            sender.open();
            Message message = Message.Factory.create();
            message.setBody(new AmqpValue(body));
            message.setAddress(address);
            sender.send(message);
        });
    }

    private static void waitForConnections(TestBroker server, int num, long timeout) throws InterruptedException {
        long endTime = System.currentTimeMillis() + timeout;
        while (System.currentTimeMillis() < endTime) {
            log.info("Num connected is : " + server.numConnected());
            if (server.numConnected() == num) {
                break;
            }
            Thread.sleep(1000);
        }
        assertThat(server.numConnected(), is(num));
    }

    private class TestHandler implements Handler<AsyncResult<ProtonConnection>> {
        private final CompletableFuture<List<String>> result;
        private final List<String> data = new ArrayList<>();
        private final int numExpected;
        private final CountDownLatch latch;

        public TestHandler(CountDownLatch latch, CompletableFuture<List<String>> result, int numExpected) {
            this.latch = latch;
            this.result = result;
            this.numExpected = numExpected;
        }

        @Override
        public void handle(AsyncResult<ProtonConnection> event) {
            ProtonConnection connection = event.result().open();
            Source source = new Source();
            source.setAddress(address);
            source.setCapabilities(Symbol.getSymbol("topic"));
            connection.createReceiver(address)
                    .openHandler(handler -> latch.countDown())
                    .setSource(source)
                    .handler((delivery, message) -> {
                        data.add((String) ((AmqpValue) message.getBody()).getValue());
                        if (data.size() == numExpected) {
                            result.complete(data);
                        }
                    })
                    .open();
        }
    }
}
