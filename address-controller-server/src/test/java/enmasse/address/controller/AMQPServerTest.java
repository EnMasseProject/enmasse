package enmasse.address.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import enmasse.address.controller.admin.FlavorManager;
import enmasse.address.controller.api.v3.AddressList;
import enmasse.address.controller.model.Destination;
import enmasse.address.controller.model.DestinationGroup;
import enmasse.address.controller.model.Flavor;
import enmasse.address.controller.model.InstanceId;
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
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

public class AMQPServerTest {
    private Vertx vertx;
    private TestAddressManagerFactory testInstanceManager;
    private TestAddressManager testAddressManager;
    private FlavorManager testRepository;
    private int port;

    @Before
    public void setup() throws InterruptedException {
        vertx = Vertx.vertx();
        testAddressManager = new TestAddressManager();
        testInstanceManager = new TestAddressManagerFactory().addManager(InstanceId.fromString("myinstance"), testAddressManager);
        testRepository = new FlavorManager();
        CountDownLatch latch = new CountDownLatch(1);
        AMQPServer server = new AMQPServer(testInstanceManager, testRepository, 0);
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
        DestinationGroup gr = new DestinationGroup("group0", Sets.newSet(new Destination("addr1", "group0", false, false, Optional.empty(), Optional.empty())));
        testAddressManager.destinationList.add(gr);

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

    @Test
    public void testFlavorsService() throws InterruptedException, ExecutionException, TimeoutException, IOException {
        Flavor flavor = new Flavor.Builder("vanilla", "inmemory-queue")
                .type("queue")
                .description("Simple queue")
                .build();
        testRepository.flavorsUpdated(Collections.singletonMap("vanilla", flavor));

        SyncRequestClient client = new SyncRequestClient("localhost", port, vertx);
        Message request = Message.Factory.create();
        Map<String, String> properties = new LinkedHashMap<>();
        properties.put("method", "GET");
        request.setAddress("$flavor");
        request.setApplicationProperties(new ApplicationProperties(properties));
        Message response = client.request(request, 1, TimeUnit.MINUTES);

        ObjectMapper mapper = new ObjectMapper();
        ObjectNode list = mapper.readValue((String)((AmqpValue)response.getBody()).getValue(), ObjectNode.class);
        assertTrue(list.has("flavors"));
        ObjectNode flavors = (ObjectNode) list.get("flavors");
        assertTrue(flavors.has("vanilla"));
        ObjectNode f = (ObjectNode) flavors.get("vanilla");
        assertThat(f.get("type").asText(), is("queue"));
        assertThat(f.get("description").asText(), is("Simple queue"));
    }

}
