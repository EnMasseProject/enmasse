package enmasse.queue.scheduler;

import enmasse.config.AddressEncoder;
import io.fabric8.kubernetes.api.model.*;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.Watcher;
import io.fabric8.kubernetes.client.dsl.ClientMixedOperation;
import io.vertx.core.Vertx;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.*;
import java.util.concurrent.*;

import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.*;

public class QueueSchedulerTest {

    private Vertx vertx;
    private ExecutorService executorService;
    private TestBrokerFactory brokerFactory;
    private QueueScheduler scheduler;
    private KubernetesClient mockClient;

    @Before
    public void setup() throws InterruptedException {
        vertx = Vertx.vertx();
        executorService = Executors.newSingleThreadScheduledExecutor();
        mockClient = mock(KubernetesClient.class);
        brokerFactory = new TestBrokerFactory(vertx, "localhost", 12223);
        scheduler = new QueueScheduler(mockClient, executorService, brokerFactory, 12223);
    }

    private void deployScheduler() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);
        vertx.deployVerticle(scheduler, r -> {
            latch.countDown();
        });
        latch.await(1, TimeUnit.MINUTES);
    }

    @After
    public void teardown() throws InterruptedException {
        vertx.close();
        executorService.shutdown();
        executorService.awaitTermination(1, TimeUnit.MINUTES);
    }

    @Test
    public void testAddressAddedBeforeBroker() throws InterruptedException, ExecutionException, TimeoutException {
        deployScheduler();
        scheduler.eventReceived(Watcher.Action.ADDED, createMap("br1", "queue1", "queue2"));

        TestBroker br1 = brokerFactory.deployBroker("br1");

        waitForAddresses(br1, 2);
        assertThat(br1.getAddressSet(), hasItem("queue1"));
        assertThat(br1.getAddressSet(), hasItem("queue2"));
    }

    @Test
    public void testAddressAdded() throws InterruptedException {
        deployScheduler();
        TestBroker br1 = brokerFactory.deployBroker("br1");
        scheduler.eventReceived(Watcher.Action.ADDED, createMap("br1", "queue1", "queue2"));

        waitForAddresses(br1, 2);
        assertThat(br1.getAddressSet(), hasItem("queue1"));
        assertThat(br1.getAddressSet(), hasItem("queue2"));

        scheduler.eventReceived(Watcher.Action.MODIFIED, createMap("br1", "queue1", "queue2", "queue3"));

        waitForAddresses(br1, 3);
        assertThat(br1.getAddressSet(), hasItem("queue1"));
        assertThat(br1.getAddressSet(), hasItem("queue2"));
        assertThat(br1.getAddressSet(), hasItem("queue3"));
    }

    @Test
    public void testAddressRemoved() throws InterruptedException {
        deployScheduler();
        TestBroker br1 = brokerFactory.deployBroker("br1");
        scheduler.eventReceived(Watcher.Action.ADDED, createMap("br1", "queue1", "queue2"));
        waitForAddresses(br1, 2);
        assertThat(br1.getAddressSet(), hasItem("queue1"));
        assertThat(br1.getAddressSet(), hasItem("queue2"));

        scheduler.eventReceived(Watcher.Action.MODIFIED, createMap("br1", "queue1"));
        waitForAddresses(br1, 1);
        assertThat(br1.getAddressSet(), hasItem("queue1"));
    }

    @Test
    public void testGroupDeleted() throws InterruptedException {
        deployScheduler();
        TestBroker br1 = brokerFactory.deployBroker("br1");
        TestBroker br2 = brokerFactory.deployBroker("br2");

        scheduler.eventReceived(Watcher.Action.ADDED, createMap("br1", "queue1"));
        scheduler.eventReceived(Watcher.Action.ADDED, createMap("br2", "queue2"));

        waitForAddresses(br1, 1);
        waitForAddresses(br2, 1);

        assertThat(br1.getAddressSet(), hasItem("queue1"));
        assertThat(br2.getAddressSet(), hasItem("queue2"));

        scheduler.eventReceived(Watcher.Action.DELETED, createMap("br1"));
        waitForAddresses(br1, 1);
        waitForAddresses(br2, 1);
        assertThat(br1.getAddressSet(), hasItem("queue1"));
        assertThat(br2.getAddressSet(), hasItem("queue2"));
    }

    @Test
    public void testBrokerAdded() throws InterruptedException {
        deployScheduler();
        scheduler.eventReceived(Watcher.Action.ADDED, createMap("br1", "queue1"));
        scheduler.eventReceived(Watcher.Action.ADDED, createMap("br2", "queue2"));

        TestBroker br1 = brokerFactory.deployBroker("br1");
        waitForAddresses(br1, 1);
        assertThat(br1.getAddressSet(), hasItem("queue1"));

        TestBroker br2 = brokerFactory.deployBroker("br2");
        waitForAddresses(br2, 1);
        assertThat(br2.getAddressSet(), hasItem("queue2"));
    }

    @Test
    public void testBrokerRemoved() throws InterruptedException, TimeoutException, ExecutionException {
        deployScheduler();
        scheduler.eventReceived(Watcher.Action.ADDED, createMap("br1", "queue1"));
        scheduler.eventReceived(Watcher.Action.ADDED, createMap("br2", "queue2"));

        TestBroker br1 = brokerFactory.deployBroker("br1");
        TestBroker br2 = brokerFactory.deployBroker("br2");

        waitForAddresses(br1, 1);
        waitForAddresses(br2, 1);

        br2.close();
        waitTask(1, TimeUnit.MINUTES);

        br2 = brokerFactory.deployBroker("br2");
        waitForAddresses(br2, 1);
        assertThat(br2.getAddressSet(), hasItem("queue2"));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testKubernetesWatch() throws InterruptedException, TimeoutException, ExecutionException {
        ClientMixedOperation mapOp = mock(ClientMixedOperation.class);
        when(mockClient.configMaps()).thenReturn(mapOp);
        when(mapOp.withLabel(anyString(), anyString())).thenReturn(mapOp);
        when(mapOp.withResourceVersion(anyString())).thenReturn(mapOp);
        ConfigMapList list = new ConfigMapListBuilder()
                .addToItems(createMap("br1", "queue1", "queue3"),
                        createMap("br2", "queue2"))
                .withMetadata(new ListMetaBuilder()
                        .withResourceVersion("r3")
                        .build())
                .build();
        when(mapOp.list()).thenReturn(list);

        deployScheduler();
        TestBroker br1 = brokerFactory.deployBroker("br1");
        waitForAddresses(br1, 2);
    }

    private static void waitForAddresses(TestBroker broker, long numAddresses) throws InterruptedException {
        waitForAddresses(broker, numAddresses, 1, TimeUnit.MINUTES);
    }

    private static void waitForAddresses(TestBroker broker, long numAddresses, long timeout, TimeUnit timeUnit) throws InterruptedException {
        long endTime = System.currentTimeMillis() + timeUnit.toMillis(timeout);
        long actualSize = broker.numQueues();
        while (System.currentTimeMillis() < endTime && actualSize != numAddresses) {
            actualSize = broker.numQueues();
            Thread.sleep(1000);
        }
        assertThat(actualSize, is(numAddresses));
    }

    private void waitTask(long timeout, TimeUnit timeUnit) throws InterruptedException, ExecutionException, TimeoutException {
        Future f = executorService.submit(() -> { });
        f.get(timeout, timeUnit);
    }

    private ConfigMap createMap(String groupId, String ... addresses) {
        Map<String, String> data = new LinkedHashMap<>();
        for (String address : addresses) {
            AddressEncoder encoder = new AddressEncoder();
            encoder.encode(true, false, Optional.of("vanilla"));
            data.put(address, encoder.toJson());
        }
        return new ConfigMapBuilder()
                .withMetadata(new ObjectMetaBuilder()
                        .withName("address-config-" + groupId)
                        .addToLabels("group_id", groupId)
                        .build())
                .withData(data)
                .build();

    }
}
