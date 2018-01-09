package io.enmasse.systemtest;

import org.junit.runner.Description;
import org.junit.runner.Result;
import org.junit.runner.notification.Failure;
import org.junit.runner.notification.RunListener;

import java.util.Iterator;
import java.util.Map;

public class SystemTestRunListener extends RunListener {


    @Override
    public void testStarted(Description description) throws Exception {
        Logging.log.info("###########################################################");
        Logging.log.info(description + "STARTED");
    }

    @Override
    public void testFinished(Description description) throws Exception {
        Logging.log.info(description + "FINISHED");
        Logging.log.info("###########################################################");
    }

    @Override
    public void testRunFinished(Result result) throws Exception {
        TestBaseWithDefault.defaultAddressSpaces.forEach((name, addrSpace) -> {
            Logging.log.info("default address space '{}' will be removed", addrSpace);
            try {
                TestBase.deleteAddressSpace(addrSpace);
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    @Override
    public void testFailure(Failure failure) throws Exception {
        Throwable ex = failure.getException();
        if (ex instanceof OutOfMemoryError) {
            Logging.log.error("Got OOM, dumping thread info");
            printThreadDump();
        } else {
            Logging.log.error("Caught exception {}", ex);
        }
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
}
