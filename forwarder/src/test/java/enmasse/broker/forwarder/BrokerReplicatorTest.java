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

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.concurrent.*;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

/**
 * @author Ulf Lilleengen
 */
public class BrokerReplicatorTest {
    private Vertx vertx;
    private String localHost = "127.0.0.1";
    private final String address = "mytopic";
    private ServerFactory.TestServer serverA;
    private ServerFactory.TestServer serverB;
    private ServerFactory.TestServer serverC;

    @Before
    public void setup() {
        VertxOptions options = new VertxOptions();
        options.setWorkerPoolSize(10);
        vertx = Vertx.vertx(options);
        ServerFactory serverFactory = new ServerFactory(vertx, localHost, address);
        serverA = serverFactory.startServer(5672);
        serverB = serverFactory.startServer(5673);
        serverC = serverFactory.startServer(5674);
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

        BrokerReplicator replicator = new BrokerReplicator(hostA, address);
        replicator.start();

        Set<Host> hosts = new LinkedHashSet<>();
        hosts.add(hostB);
        hosts.add(hostC);
        replicator.hostsChanged(hosts);

        long timeout = 60_000;
        waitForConnections(serverA, 1, timeout);
        waitForConnections(serverB, 1, timeout);
        waitForConnections(serverC, 1, timeout);

        CountDownLatch latch = new CountDownLatch(1);
        ProtonClient client = ProtonClient.create(vertx);
        client.connect(localHost, 5672, event -> {
            ProtonConnection connection = event.result().open();
            ProtonSender sender = connection.createSender(address).open();
            Message message = Message.Factory.create();
            message.setBody(new AmqpValue("Hello"));
            message.setAddress(address);
            sender.send(message);
            latch.countDown();
        });

        latch.await(60, TimeUnit.SECONDS);

        CompletableFuture<String> resultB = new CompletableFuture<>();
        CompletableFuture<String> resultC = new CompletableFuture<>();

        ProtonClient.create(vertx).connect(localHost, 5673, new TestHandler(resultB));
        ProtonClient.create(vertx).connect(localHost, 5674, new TestHandler(resultC));

        assertThat(resultB.get(30, TimeUnit.SECONDS), is("Hello"));
        assertThat(resultC.get(30, TimeUnit.SECONDS), is("Hello"));
        vertx.close();
    }

    private static void waitForConnections(ServerFactory.TestServer server, int num, long timeout) throws InterruptedException {
        long endTime = System.currentTimeMillis() + timeout;
        while (server.numConnected() != num && System.currentTimeMillis() < endTime) {
            Thread.sleep(1000);
        }
        assertThat(server.numConnected(), is(num));
    }

    private class TestHandler implements Handler<AsyncResult<ProtonConnection>> {
        private final CompletableFuture<String> result;

        public TestHandler(CompletableFuture<String> result) {
            this.result = result;
        }

        @Override
        public void handle(AsyncResult<ProtonConnection> event) {
            ProtonConnection connection = event.result().open();
            connection.createReceiver(address).handler((delivery, message) -> {
                String data = (String)((AmqpValue)message.getBody()).getValue();
                System.out.println("Recevide " + data);
                result.complete(data);
            }).open();
        }
    }
}
