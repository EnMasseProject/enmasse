package io.enmasse.systemtest.marathon;

import io.enmasse.systemtest.Logging;
import io.enmasse.systemtest.TestBase;
import org.apache.http.util.ExceptionUtils;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.rules.ErrorCollector;

import java.util.Date;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class MarathonTestBase extends TestBase {

    @BeforeClass
    public static void initThreadDump() {
        Logging.log.info("Setting default exception handler for threads");
        Thread.setDefaultUncaughtExceptionHandler((thread, e) -> {
            if (e instanceof OutOfMemoryError) {
                Logging.log.error("Got OOM, dumping thread info");
                printThreadDump();
            } else {
                Logging.log.error("Caught exception {}", e);
            }
        });
    }

    public static void printThreadDump() {
        Map<Thread, StackTraceElement[]> allThreads = Thread.getAllStackTraces();
        Iterator<Thread> iterator = allThreads.keySet().iterator();
        while (iterator.hasNext()) {
            StringBuilder sb = new StringBuilder();
            Thread key = iterator.next();
            StackTraceElement[] trace = allThreads.get(key);
            sb.append(key + "\r\n");
            for (int i = 0; i < trace.length; i++) {
                sb.append(" " + trace[i] + "\r\n");
            }
            Logging.log.error(sb.toString());
        }
    }


    @Rule
    public ErrorCollector collector = new ErrorCollector();

    protected void runTestInLoop(int durationMinutes, TestLoop test) {
        Logging.log.info(String.format("Starting test running for %d minutes at %s",
                durationMinutes, new Date().toString()));
        for(long stop = System.nanoTime() + TimeUnit.MINUTES.toNanos(durationMinutes); stop > System.nanoTime();) {
            try {
                test.run();
            }catch (Exception ex){
                collector.addError(ex);
            }
        }
    }

    protected interface TestLoop{
        void run() throws Exception;
    }
}
