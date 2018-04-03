/*
 * Copyright 2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.systemtest.bases;

import io.enmasse.systemtest.CustomLogger;
import org.junit.runner.Description;
import org.junit.runner.Result;
import org.junit.runner.notification.Failure;
import org.junit.runner.notification.RunListener;
import org.slf4j.Logger;

import java.util.Iterator;
import java.util.Map;

public class SystemTestRunListener extends RunListener {
    private static Logger log = CustomLogger.getLogger();

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
            log.error(sb.toString());
        }
    }

    @Override
    public void testStarted(Description description) throws Exception {
        log.info("###########################################################");
        log.info(description + "STARTED");
    }

    @Override
    public void testFinished(Description description) throws Exception {
        log.info(description + "FINISHED");
        log.info("###########################################################");
    }

    @Override
    public void testRunFinished(Result result) throws Exception {
        if (!TestBase.environment.skipCleanup()) {
            TestBaseWithShared.sharedAddressSpaces.forEach((name, addrSpace) -> {
                log.info("shared address space '{}' will be removed", addrSpace);
                try {
                    TestBase.deleteAddressSpace(addrSpace);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });
        } else {
            log.warn("Remove address spaces when test run finished - SKIPPED!");
        }
    }

    @Override
    public void testFailure(Failure failure) throws Exception {
        Throwable ex = failure.getException();
        if (ex instanceof OutOfMemoryError) {
            log.error("Got OOM, dumping thread info");
            printThreadDump();
        } else {
            log.error("Caught exception {}", ex);
        }
    }
}
