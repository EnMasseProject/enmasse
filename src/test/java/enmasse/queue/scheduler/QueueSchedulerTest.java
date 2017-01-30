package enmasse.queue.scheduler;

import enmasse.config.AddressEncoder;
import io.fabric8.kubernetes.api.model.ConfigMap;
import io.fabric8.kubernetes.api.model.ConfigMapBuilder;
import io.fabric8.kubernetes.api.model.ConfigMapList;
import io.fabric8.kubernetes.api.model.ObjectMetaBuilder;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.Watcher;
import io.fabric8.kubernetes.client.dsl.ClientMixedOperation;
import io.fabric8.kubernetes.client.dsl.ClientOperation;
import io.vertx.core.Vertx;
import org.apache.qpid.proton.message.Message;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.omg.PortableServer.THREAD_POLICY_ID;

import javax.lang.model.type.ExecutableType;
import java.util.*;
import java.util.concurrent.*;

import static org.mockito.Mockito.*;

public class QueueSchedulerTest {

    private Vertx vertx;
    private String schedulerHost;
    private int schedulerPort;
    private ExecutorService executorService;

    @Before
    public void setup() {
        executorService = Executors.newSingleThreadScheduledExecutor();
        vertx = Vertx.vertx();
        schedulerHost = "localhost";
        schedulerPort = 12223;
    }

    @After
    public void teardown() throws InterruptedException {
        vertx.close();
        executorService.shutdown();
        executorService.awaitTermination(1, TimeUnit.MINUTES);
    }


    public TestBroker deployBroker(String id) throws InterruptedException, ExecutionException, TimeoutException {
        CompletableFuture<String> promise = new CompletableFuture<>();
        TestBroker broker = new TestBroker(id, schedulerHost, schedulerPort);
        vertx.deployVerticle(broker, result -> {
            if (result.succeeded()) {
                promise.complete(result.result());
            } else {
                promise.completeExceptionally(result.cause());
            }
        });
        promise.get(1, TimeUnit.MINUTES);
        return broker;
    }

    @Test
    public void testScheduler() throws InterruptedException, ExecutionException, TimeoutException {
        KubernetesClient mockClient = mock(KubernetesClient.class);
        QueueScheduler scheduler = new QueueScheduler(mockClient, executorService, schedulerPort);
        vertx.deployVerticle(scheduler);

        scheduler.eventReceived(Watcher.Action.ADDED, createMap("br1", "queue1", "queue2"));

        waitTask(1, TimeUnit.MINUTES);
        Thread.sleep(6000);
        TestBroker br1 = deployBroker("br1");
        waitTask(1, TimeUnit.MINUTES);
        Thread.sleep(6000);
        Message received = br1.receive();
        System.out.println(received);
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
