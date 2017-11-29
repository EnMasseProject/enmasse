package io.enmasse.systemtest.marathon;

import io.enmasse.systemtest.Logging;
import io.enmasse.systemtest.TestBase;
import io.enmasse.systemtest.amqp.AmqpClient;
import org.junit.Rule;
import org.junit.rules.ErrorCollector;

import java.util.ArrayList;
import java.util.Date;
import java.util.concurrent.TimeUnit;

public class MarathonTestBase extends TestBase {
    ArrayList<AmqpClient> clients = new ArrayList<>();

    @Rule
    public ErrorCollector collector = new ErrorCollector();

    protected void runTestInLoop(int durationMinutes, TestLoop test) {
        Logging.log.info(String.format("Starting test running for %d minutes at %s",
                durationMinutes, new Date().toString()));
        for (long stop = System.nanoTime() + TimeUnit.MINUTES.toNanos(durationMinutes); stop > System.nanoTime(); ) {
            try {
                test.run();
            } catch (Exception ex) {
                collector.addError(ex);
            } finally {
                closeClients();
            }
        }
    }

    private void closeClients() {
        try {
            for (AmqpClient client : clients) {
                client.close();
                Logging.log.info("Clients are closed.");
            }
        } catch (Exception ex) {
            collector.addError(ex);
        } finally {
            clients.clear();
        }
    }

    protected interface TestLoop {
        void run() throws Exception;
    }
}
