/*
 * Copyright 2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.controller;

import io.enmasse.address.model.AddressSpace;
import io.enmasse.address.model.Endpoint;
import io.enmasse.address.model.SecretCertProvider;
import io.enmasse.k8s.api.TestAddressSpaceApi;
import io.vertx.core.Vertx;
import org.junit.After;
import org.junit.Before;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertTrue;

public class AMQPServerTest {
    private Vertx vertx;
    private TestAddressSpaceApi instanceApi;
    private int port;

    @Before
    public void setup() throws InterruptedException {
        vertx = Vertx.vertx();
        instanceApi = new TestAddressSpaceApi();
        String addressSpaceName = "myinstance";
        instanceApi.createAddressSpace(createAddressSpace(addressSpaceName));
        CountDownLatch latch = new CountDownLatch(1);
        AMQPServer server = new AMQPServer(addressSpaceName, instanceApi, 0);
        vertx.deployVerticle(server, c -> {
            latch.countDown();
        });
        latch.await(1, TimeUnit.MINUTES);
        port = waitForPort(server, 1, TimeUnit.MINUTES);
    }

    private AddressSpace createAddressSpace(String name) {
        return new AddressSpace.Builder()
                .setName(name)
                .setNamespace(name)
                .setType("mytype")
                .setPlan("myplan")
                .setStatus(new io.enmasse.address.model.Status(false))
                .appendEndpoint(new Endpoint.Builder()
                        .setName("foo")
                        .setService("messaging")
                        .setCertProvider(new SecretCertProvider("mysecret"))
                        .build())
                .build();
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

    /*
    @Test
    public void testAddressingService() throws InterruptedException, ExecutionException, TimeoutException, IOException {
        Address destination =
                new Address("addr1", "group0", false, false, Optional.empty(), Optional.empty(), new Status(false));
        addressSpaceApi.withAddressSpace(AddressSpaceId.withId("myinstance")).createAddress(destination);

        SyncRequestClient client = new SyncRequestClient("localhost", port, vertx);
        Message request = Message.Factory.create();
        Map<String, String> properties = new LinkedHashMap<>();
        properties.put("method", "GET");
        request.setAddress("$address");
        request.setApplicationProperties(new ApplicationProperties(properties));
        Message response = client.request(request, 1, TimeUnit.MINUTES);

        ObjectMapper mapper = new ObjectMapper();
        AddressList list = mapper.readValue((String)((AmqpValue)response.getBody()).getValue(), AddressList.class);
        assertThat(list.getAddresss(), hasItem(destination));
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
        assertThat(list.get("items").get(0).get("metadata").get("name").asText(), is("vanilla"));
        assertThat(list.get("items").get(0).get("spec").get("type").asText(), is("queue"));
        assertThat(list.get("items").get(0).get("spec").get("description").asText(), is("Simple queue"));
    }

    */
}
