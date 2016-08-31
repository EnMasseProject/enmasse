package enmasse.broker.forwarder;

import enmasse.discovery.Host;
import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.VertxOptions;
import io.vertx.proton.ProtonClient;
import io.vertx.proton.ProtonConnection;
import io.vertx.proton.ProtonSender;
import org.apache.qpid.proton.amqp.messaging.AmqpValue;
import org.apache.qpid.proton.message.Message;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.*;

import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.junit.Assert.assertThat;

/**
 * @author Ulf Lilleengen
 */
public class ForwarderControllerTest {
    private Vertx vertx = Vertx.vertx(new VertxOptions().setWorkerPoolSize(10));
    private String localHost = "127.0.0.1";
    private final String address = "mytopic";
    private TestBroker serverA = new TestBroker(localHost, 5672, address);
    private TestBroker serverB = new TestBroker(localHost, 5673, address);
    private TestBroker serverC = new TestBroker(localHost, 5674, address);

    @Before
    public void setup() {
        Logger.getGlobal().setLevel(Level.INFO);
        Logger.getLogger("enmasse").setLevel(Level.FINEST);
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

        ForwarderController replicator = new ForwarderController(hostA, address, "fwdId");

        Set<Host> hosts = new LinkedHashSet<>();
        hosts.add(hostB);
        replicator.hostsChanged(hosts);
        Thread.sleep(1000);
        hosts.add(hostC);
        replicator.hostsChanged(hosts);

        long timeout = 60_000;
        waitForConnections(serverA, 2, timeout);
        waitForConnections(serverB, 1, timeout);
        waitForConnections(serverB, 1, timeout);

        CompletableFuture<List<String>> result = new CompletableFuture<>();

        ProtonClient.create(vertx).connect(localHost, 5672, new TestHandler(result, 2));

        sendMessageTo(5673, "Hello from B");
        sendMessageTo(5674, "Hello from C");

        List<String> messages = result.get(30, TimeUnit.SECONDS);
        System.out.println("Got these messages: " + messages);
        assertThat(messages.size(), is(2));
        assertThat(messages, hasItem("Hello from B"));
        assertThat(messages, hasItem("Hello from C"));
        vertx.close();
    }

    private void sendMessageTo(int port, String body) {
        ProtonClient client = ProtonClient.create(vertx);
        client.connect(localHost, port, event -> {
            ProtonConnection connection = event.result().open();
            ProtonSender sender = connection.createSender(address).open();
            Message message = Message.Factory.create();
            message.setBody(new AmqpValue(body));
            message.setAddress(address);
            sender.send(message);
        });
    }

    private static void waitForConnections(TestBroker server, int num, long timeout) throws InterruptedException {
        long endTime = System.currentTimeMillis() + timeout;
        while (System.currentTimeMillis() < endTime) {
            System.out.println("Num connected is : " + server.numConnected());
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

        public TestHandler(CompletableFuture<List<String>> result, int numExpected) {
            this.result = result;
            this.numExpected = numExpected;
        }

        @Override
        public void handle(AsyncResult<ProtonConnection> event) {
            ProtonConnection connection = event.result().open();
            connection.createReceiver(address).handler((delivery, message) -> {
                data.add((String)((AmqpValue)message.getBody()).getValue());
                if (data.size() == numExpected) {
                    result.complete(data);
                }
            }).open();
        }
    }
}
