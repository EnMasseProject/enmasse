package enmasse.systemtest;

import io.vertx.proton.ProtonClient;
import io.vertx.proton.ProtonConnection;
import io.vertx.proton.ProtonReceiver;
import org.apache.qpid.proton.amqp.Symbol;
import org.apache.qpid.proton.amqp.messaging.AmqpSequence;
import org.apache.qpid.proton.amqp.messaging.Source;
import org.junit.Test;

import java.util.*;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.TimeUnit;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

public class ConfigServTest extends VertxTestBase {
    @SuppressWarnings("unchecked")
    @Test
    public void testPodSense() throws Exception {
        Endpoint configserv = getConfigServEndpoint();
        BlockingQueue<List<String>> latestPods = new LinkedBlockingDeque<>();
        ProtonClient.create(vertx).connect(configserv.getHost(), configserv.getPort(), event -> {
            if (event.succeeded()) {
                ProtonConnection connection = event.result().open();
                Source source = new Source();
                source.setAddress("podsense");
                source.setFilter(Collections.singletonMap(Symbol.getSymbol("role"), "broker"));
                ProtonReceiver receiver = connection.createReceiver("podsense").setSource(source);
                receiver.handler((protonDelivery, message) -> {
                    List<String> pods = new ArrayList<>();
                    AmqpSequence seq = (AmqpSequence) message.getBody();
                    for (Object obj : seq.getValue()) {
                        Map<String, Object> pod = (Map<String, Object>) obj;
                        pods.add((String) pod.get("host"));
                    }
                    try {
                        latestPods.put(pods);
                    } catch (InterruptedException e) {
                        fail();
                    }
                });
                receiver.open();
            }
        });
        deploy(Destination.queue("myqueue"));
        assertPods(latestPods, 1, new TimeoutBudget(2, TimeUnit.MINUTES));
        deploy(Destination.queue("myqueue"), Destination.queue("anotherqueue"));
        assertPods(latestPods, 2, new TimeoutBudget(2, TimeUnit.MINUTES));
        deploy(Destination.queue("anotherqueue"));
        assertPods(latestPods, 1, new TimeoutBudget(2, TimeUnit.MINUTES));
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
}

