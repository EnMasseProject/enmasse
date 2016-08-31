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
import java.util.logging.Level;
import java.util.logging.Logger;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

/**
 * @author Ulf Lilleengen
 */
public class ForwarderControllerTest {
    private Vertx vertx = Vertx.vertx(new VertxOptions().setWorkerPoolSize(10));
    private String localHost = "127.0.0.1";
    private final String address = "mytopic";
    private TestBroker serverA = new TestBroker(vertx, localHost, 5672, address);
    private TestBroker serverB = new TestBroker(vertx, localHost, 5673, address);
    private TestBroker serverC = new TestBroker(vertx, localHost, 5674, address);

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

        ForwarderController replicator = new ForwarderController(hostA, address);

        Set<Host> hosts = new LinkedHashSet<>();
        hosts.add(hostB);
        replicator.hostsChanged(hosts);
        Thread.sleep(1000);
        hosts.add(hostC);
        replicator.hostsChanged(hosts);

        long timeout = 60_000;
        waitForConnections(serverA, 2, timeout);
        waitForConnections(serverB, 1, timeout);
        waitForConnections(serverC, 1, timeout);

        CompletableFuture<String> resultB = new CompletableFuture<>();
        CompletableFuture<String> resultC = new CompletableFuture<>();

        ProtonClient.create(vertx).connect(localHost, 5673, new TestHandler(resultB));
        ProtonClient.create(vertx).connect(localHost, 5674, new TestHandler(resultC));

        ProtonClient client = ProtonClient.create(vertx);
        client.connect(localHost, 5672, event -> {
            ProtonConnection connection = event.result().open();
            ProtonSender sender = connection.createSender(address).open();
            vertx.setPeriodic(2000, timerId -> {
                Message message = Message.Factory.create();
                message.setBody(new AmqpValue("Hello"));
                message.setAddress(address);
                sender.send(message);
            });
        });

        assertThat(resultB.get(30, TimeUnit.SECONDS), is("Hello"));
        assertThat(resultC.get(30, TimeUnit.SECONDS), is("Hello"));
        vertx.close();
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
        private final CompletableFuture<String> result;

        public TestHandler(CompletableFuture<String> result) {
            this.result = result;
        }

        @Override
        public void handle(AsyncResult<ProtonConnection> event) {
            ProtonConnection connection = event.result().open();
            connection.createReceiver(address).handler((delivery, message) -> {
                String data = (String)((AmqpValue)message.getBody()).getValue();
                result.complete(data);
            }).open();
        }
    }
}
