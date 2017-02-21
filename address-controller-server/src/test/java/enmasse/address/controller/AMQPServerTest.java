package enmasse.address.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import enmasse.address.controller.api.v3.AddressList;
import enmasse.address.controller.model.Destination;
import enmasse.address.controller.model.DestinationGroup;
import enmasse.amqp.SyncRequestClient;
import io.vertx.core.Vertx;
import org.apache.qpid.proton.amqp.messaging.AmqpValue;
import org.apache.qpid.proton.amqp.messaging.ApplicationProperties;
import org.apache.qpid.proton.message.Message;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.internal.util.collections.Sets;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static org.hamcrest.CoreMatchers.hasItem;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

public class AMQPServerTest {
    private Vertx vertx;
    private HTTPServerTest.TestManager testManager;
    private int port;

    @Before
    public void setup() throws InterruptedException {
        vertx = Vertx.vertx();
        testManager = new HTTPServerTest.TestManager();
        CountDownLatch latch = new CountDownLatch(1);
        AMQPServer server = new AMQPServer(testManager, 0);
        vertx.deployVerticle(server, c -> {
            latch.countDown();
        });
        latch.await(1, TimeUnit.MINUTES);
        port = waitForPort(server, 1, TimeUnit.MINUTES);
    }

    private int waitForPort(AMQPServer server, long timeout, TimeUnit timeUnit) throws InterruptedException {
        long endTime = System.currentTimeMillis() + timeUnit.toMillis(timeout);
        int actualPort = server.getPort();
        while (actualPort == 0 && System.currentTimeMillis() < endTime) {
            Thread.sleep(1000);
            actualPort = server.getPort();
        }
        assertTrue(actualPort != 0);
        return actualPort;
    }

    @After
    public void teardown() {
        vertx.close();
    }

    @Test
    public void testAddressingService() throws InterruptedException, ExecutionException, TimeoutException, IOException {
        DestinationGroup gr = new DestinationGroup("group0", Sets.newSet(new Destination("addr1", "group0", false, false, Optional.empty())));
        testManager.destinationList.add(gr);

        SyncRequestClient client = new SyncRequestClient("localhost", port, vertx);
        Message request = Message.Factory.create();
        Map<String, String> properties = new LinkedHashMap<>();
        properties.put("method", "GET");
        request.setAddress("$address");
        request.setApplicationProperties(new ApplicationProperties(properties));
        Message response = client.request(request, 1, TimeUnit.MINUTES);

        ObjectMapper mapper = new ObjectMapper();
        AddressList list = mapper.readValue((String)((AmqpValue)response.getBody()).getValue(), AddressList.class);
        assertThat(list.getDestinationGroups(), hasItem(gr));
    }
}
