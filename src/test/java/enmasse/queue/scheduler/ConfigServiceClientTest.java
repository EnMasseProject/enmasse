package enmasse.queue.scheduler;

import io.vertx.core.Vertx;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import static enmasse.queue.scheduler.TestUtils.waitForPort;
import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.*;

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
        int configPort = waitForPort(() -> testConfigServ.getPort(), 1, TimeUnit.MINUTES);

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
        testConfigServ.deployConfig("{\"queue1\":{\"store_and_forward\":true,\"multicast\":false,\"group_id\":\"group1\"},\"queue2\":{\"group_id\":\"group1\"},\"queue3\":{\"group_id\":\"group2\"}}");

        waitForPort(() -> listener.addressMap == null ? 0 : 1, 1, TimeUnit.MINUTES);

        assertNotNull(listener.addressMap);
        assertThat(listener.addressMap.size(), is(2));
        assertNotNull(listener.addressMap.get("group1"));
        assertNotNull(listener.addressMap.get("group2"));
        assertThat(listener.addressMap.get("group1"), hasItem("queue1"));
        assertThat(listener.addressMap.get("group1"), hasItem("queue2"));
        assertThat(listener.addressMap.get("group2"), hasItem("queue3"));
    }

    private static class TestListener implements ConfigListener {
        public volatile Map<String, Set<String>> addressMap;

        @Override
        public void addressesChanged(Map<String, Set<String>> addressMap) {
            this.addressMap = addressMap;
        }
    }
}
