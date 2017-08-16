package enmasse.discovery;

import io.enmasse.config.service.amqp.AMQPServer;
import io.enmasse.config.service.model.Subscriber;
import io.vertx.core.Vertx;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import org.apache.qpid.proton.amqp.messaging.AmqpValue;
import org.apache.qpid.proton.message.Message;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.*;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

@RunWith(VertxUnitRunner.class)
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
    public void testDiscovery(TestContext context) throws InterruptedException, IOException, ExecutionException {
        Map<String, String> expectedLabelFilter = Collections.singletonMap("my", "key");
        Map<String, String> expectedAnnotationFilter = Collections.singletonMap("my", "annotation");
        CompletableFuture<Set<Host>> changedHosts = new CompletableFuture<>();

        BlockingQueue<Subscriber> subscriptionQueue = new LinkedBlockingDeque<>();
        AMQPServer testServer = new AMQPServer("0.0.0.0", 0, Collections.singletonMap("podsense", (observerKey, subscriber) -> {
            assertThat(observerKey.getLabelFilter(), is(expectedLabelFilter));
            assertThat(observerKey.getAnnotationFilter(), is(expectedAnnotationFilter));
            subscriptionQueue.put(subscriber);
        }));

        vertx.deployVerticle(testServer, context.asyncAssertSuccess());
        DiscoveryClient client = new DiscoveryClient(new Endpoint("127.0.0.1", waitForPort(testServer)), "podsense", expectedLabelFilter, expectedAnnotationFilter, Optional.empty());
        client.addListener(changedHosts::complete);
        vertx.deployVerticle(client, context.asyncAssertSuccess());

        Subscriber subscriber = subscriptionQueue.poll(1, TimeUnit.MINUTES);
        subscriber.resourcesUpdated(createResponse("False", "Pending"));
        try {
            changedHosts.get(10, TimeUnit.SECONDS);
            fail("Pending hosts should not update host set");
        } catch (TimeoutException ignored) {
        }

        subscriber.resourcesUpdated(createResponse("False", "Running"));
        try {
            changedHosts.get(10, TimeUnit.SECONDS);
            fail("Ready must be true before returning host");
        } catch (TimeoutException ignored) {
        }

        subscriber.resourcesUpdated(createResponse("True", "Running"));
        try {
            Set<Host> actual = changedHosts.get(2, TimeUnit.MINUTES);
            assertThat(actual.size(), is(1));
            Host actualHost = actual.iterator().next();
            assertThat(actualHost.getHostname(), is("10.0.0.1"));
        } catch (Exception e) {
            fail("Unexpected exception" + e.getMessage());
            e.printStackTrace();
        }
    }

    public Message createResponse(String ready, String phase) {
        Message message = Message.Factory.create();
        Map<String, Object> responseMap = new LinkedHashMap<>();
        Map<String, Map<String, Integer>> portMap = Collections.singletonMap("c", Collections.singletonMap("http", 1234));
        responseMap.put("host", "10.0.0.1");
        responseMap.put("ports", portMap);
        responseMap.put("ready", ready);
        responseMap.put("phase", phase);
        AmqpValue val = new AmqpValue(Collections.singletonList(responseMap));
        message.setBody(val);
        return message;
    }


    private int waitForPort(AMQPServer testServer) throws InterruptedException {
        while (testServer.port() == 0) {
            Thread.sleep(1000);
        }
        return testServer.port();
    }
}
