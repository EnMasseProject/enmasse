package io.enmasse.systemtest.marathon;

import io.enmasse.systemtest.Logging;
import io.enmasse.systemtest.TestBase;
import org.junit.Rule;
import org.junit.rules.ErrorCollector;

import java.util.Date;
import java.util.concurrent.TimeUnit;

public class MarathonTestBase extends TestBase {

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
            }
        }
    }

    protected interface TestLoop {
        void run() throws Exception;
    }
}
