package enmasse.queue.scheduler;

import io.vertx.core.Vertx;
import io.vertx.proton.ProtonClient;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.concurrent.*;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

public class ArtemisTest {

    private Vertx vertx;
    private TestArtemis server;

    @Before
    public void setup() throws Exception {
        vertx = Vertx.vertx();
        server = new TestArtemis("localhost", 12345);
        server.start();
    }

    @After
    public void teardown() {
        vertx.close();
    }

    @Test
    public void testManagement() throws InterruptedException, ExecutionException, TimeoutException {
        ProtonClient client = ProtonClient.create(vertx);
        CompletableFuture<Future<Artemis>> promise = new CompletableFuture<>();
        client.connect("localhost", 12345, h -> {
            assertTrue(h.succeeded());
            promise.complete(Artemis.create(vertx, h.result().open()));
        });

        Artemis artemis = promise.get().get(10, TimeUnit.SECONDS);

        artemis.deployQueue("queue1");
        artemis.deployQueue("queue2");

        assertThat(artemis.numQueues(), is(2L));
    }
}
