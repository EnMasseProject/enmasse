package enmasse.discovery;

import enmasse.config.service.amqp.AMQPServer;
import enmasse.config.service.model.ResourceDatabase;
import enmasse.config.service.model.Subscriber;
import io.vertx.core.Vertx;
import org.apache.qpid.proton.amqp.messaging.AmqpSequence;
import org.apache.qpid.proton.message.Message;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

public class DiscoveryTest {

    private Vertx vertx;

    @Before
    public void setup() {
        vertx = Vertx.vertx();
    }

    @After
    public void teardown() {
        vertx.close();
    }

    @Test
    public void testDiscovery() throws InterruptedException, IOException, TimeoutException, ExecutionException {
        Map<String, String> expectedFilter = Collections.singletonMap("my", "key");
        CompletableFuture<Set<Host>> changedHosts = new CompletableFuture<>();

        AMQPServer testServer = new AMQPServer("0.0.0.0", 0, (address, map, subscriber) -> {
            assertThat(address, is("podsense"));
            assertThat(map, is(expectedFilter));

            Message message = Message.Factory.create();
            Map<String, Object> responseMap = new LinkedHashMap<>();
            Map<Integer, String> portMap = Collections.singletonMap(1234, "http");
            responseMap.put("ip", "10.0.0.1");
            responseMap.put("ports", portMap);
            AmqpSequence seq = new AmqpSequence(Collections.singletonList(responseMap));
            message.setBody(seq);

            subscriber.resourcesUpdated(message);
            return true;
        });

        vertx.deployVerticle(testServer);
        DiscoveryClient client = new DiscoveryClient(new Endpoint("127.0.0.1", waitForPort(testServer)), expectedFilter);
        client.addListener(changedHosts::complete);
        vertx.deployVerticle(client);

        Set<Host> actual = changedHosts.get(1, TimeUnit.MINUTES);
        assertThat(actual.size(), is(1));
        Host actualHost = actual.iterator().next();
        assertThat(actualHost.getHostname(), is("10.0.0.1"));
    }


    private int waitForPort(AMQPServer testServer) throws InterruptedException {
        while (testServer.port() == 0) {
            Thread.sleep(1000);
        }
        return testServer.port();
    }
}
