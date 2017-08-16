package io.enmasse.queue.scheduler;

import io.enmasse.address.model.Address;
import io.vertx.core.Vertx;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;

public class ConfigServiceClientTest {
    private Vertx vertx;
    private TestConfigServ testConfigServ;
    private ConfigServiceClient client;
    private TestListener listener;

    @Before
    public void setup() throws Exception {
        vertx = Vertx.vertx();
        testConfigServ = new TestConfigServ(0);
        listener = new TestListener();
        TestUtils.deployVerticle(vertx, testConfigServ);
        int configPort = TestUtils.waitForPort(() -> testConfigServ.getPort(), 1, TimeUnit.MINUTES);

        client = new ConfigServiceClient("localhost", configPort, listener);
        TestUtils.deployVerticle(vertx, client);

    }

    @After
    public void teardown() throws InterruptedException {
        vertx.close();
    }

    @Test
    public void testClientUpdatesListener() throws Exception {
        assertNull(listener.addressMap);
        testConfigServ.deployConfig("{\"apiVersion\": \"enmasse.io/v1\", \"kind\": \"AddressList\", \"items\": [" +
            "{ \"apiVersion\": \"enmasse.io/v1\", \"kind\": \"Address\", \"metadata\": { \"name\": \"queue1\" }, \"spec\": { \"address\": \"queue1\", \"type\": \"queue\", \"plan\": \"pooled-inmemory\" } }," +
            "{ \"apiVersion\": \"enmasse.io/v1\", \"kind\": \"Address\", \"metadata\": { \"name\": \"queue2\" }, \"spec\": { \"address\": \"queue2\", \"type\": \"queue\", \"plan\": \"pooled-inmemory\" } }," +
            "{ \"apiVersion\": \"enmasse.io/v1\", \"kind\": \"Address\", \"metadata\": { \"name\": \"queue3\" }, \"spec\": { \"address\": \"queue3\", \"type\": \"queue\", \"plan\": \"inmemory\" } }," +
            "{ \"apiVersion\": \"enmasse.io/v1\", \"kind\": \"Address\", \"metadata\": { \"name\": \"direct1\" }, \"spec\": { \"address\": \"direct1\", \"type\": \"anycast\", \"plan\": \"standard\" } }" +
            "]}");

        TestUtils.waitForPort(() -> listener.addressMap == null ? 0 : 1, 1, TimeUnit.MINUTES);

        assertNotNull(listener.addressMap);
        assertThat(listener.addressMap.size(), is(2));
        assertNotNull(listener.addressMap.get("pooled-inmemory"));
        assertNotNull(listener.addressMap.get("queue3"));
        assertThat(listener.addressMap.get("pooled-inmemory"), hasItem("queue1"));
        assertThat(listener.addressMap.get("pooled-inmemory"), hasItem("queue2"));
        assertThat(listener.addressMap.get("queue3"), hasItem("queue3"));
    }

    private static class TestListener implements ConfigListener {
        volatile Map<String, Set<String>> addressMap;

        @Override
        public void addressesChanged(Map<String, Set<Address>> addressMap) {

            this.addressMap = addressMap.entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey,
                                          e -> e.getValue().stream().map(Address::getAddress).collect(Collectors.toSet())));
        }

    }

}
