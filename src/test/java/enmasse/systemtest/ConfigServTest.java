package enmasse.systemtest;

import io.fabric8.openshift.client.OpenShiftClient;
import io.vertx.proton.ProtonClient;
import io.vertx.proton.ProtonConnection;
import io.vertx.proton.ProtonReceiver;
import org.apache.qpid.proton.amqp.Symbol;
import org.apache.qpid.proton.amqp.messaging.AmqpValue;
import org.apache.qpid.proton.amqp.messaging.Source;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.*;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.TimeUnit;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.*;

public class ConfigServTest extends AmqpTestBase {

    private Set<String> pods;

    @Before
    public void setupPodList() {
        pods = new LinkedHashSet<>();
    }

    @After
    public void teardownPods() {
        OpenShiftClient client = openShift.getClient();
        for (String pod : pods) {
            assertTrue(client.pods().withName(pod).delete());
        }
        pods.clear();
    }

    public void createPod(String name) throws Exception {
        OpenShiftClient client = openShift.getClient();
        client.pods().createNew()
                .withNewMetadata()
                    .withName(name)
                    .addToLabels("testpodsense", "working")
                    .endMetadata()
                .withNewSpec()
                    .addNewContainer()
                        .withName("dummy")
                        .withImage("fedora:25")
                        .withCommand("read")
                        .endContainer()
                    .endSpec()
                .done();
        pods.add(name);
    }

    private void deletePod(String name) throws Exception {
        OpenShiftClient client = openShift.getClient();
        assertTrue(client.pods().withName(name).delete());
        pods.remove(name);
    }

    @SuppressWarnings("unchecked")
    public void testPodSense() throws Exception {
        Endpoint configserv = getConfigServEndpoint();
        BlockingQueue<List<String>> latestPods = new LinkedBlockingDeque<>();
        ProtonClient.create(vertx).connect(configserv.getHost(), configserv.getPort(), event -> {
            if (event.succeeded()) {
                ProtonConnection connection = event.result().open();
                Source source = new Source();
                source.setAddress("podsense");
                source.setFilter(Collections.singletonMap(Symbol.getSymbol("testpodsense"), "working"));
                ProtonReceiver receiver = connection.createReceiver("podsense").setSource(source);
                receiver.handler((protonDelivery, message) -> {
                    List<String> pods = new ArrayList<>();
                    AmqpValue val = (AmqpValue) message.getBody();
                    for (Object obj : (List)val.getValue()) {
                        Map<String, Object> pod = (Map<String, Object>) obj;
                        pods.add((String) pod.get("host"));
                    }
                    System.out.println("Got pods: " + pods);
                    try {
                        latestPods.put(pods);
                    } catch (InterruptedException e) {
                        fail();
                    }
                });
                receiver.open();
            }
        });

        createPod("pod1");
        assertPods(latestPods, 1, new TimeoutBudget(2, TimeUnit.MINUTES));
        createPod("pod2");
        createPod("pod3");
        assertPods(latestPods, 3, new TimeoutBudget(2, TimeUnit.MINUTES));
        deletePod("pod2");
        assertPods(latestPods, 2, new TimeoutBudget(2, TimeUnit.MINUTES));
    }

    private void assertPods(BlockingQueue<List<String>> latestPods, int numPods, TimeoutBudget timeoutBudget) throws InterruptedException {
        List<String> pods = null;
        do {
            pods = latestPods.poll(timeoutBudget.timeLeft(), TimeUnit.MILLISECONDS);
        } while ((pods == null || pods.size() != numPods) && timeoutBudget.timeLeft() >= 0);
        assertNotNull(pods);
        assertThat(pods.size(), is(numPods));
    }

    private Endpoint getConfigServEndpoint() {
        if (openShift.isFullTemplate()) {
            return openShift.getEndpoint("configuration", "amqp");
        } else {
            return openShift.getEndpoint("admin", "configuration");
        }
    }

    @Override
    protected String getInstanceName() {
        return ConfigServTest.class.getSimpleName();
    }
}

