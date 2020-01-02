/*
 * Copyright 2019, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.systemtest.bases;

import io.enmasse.systemtest.logs.CustomLogger;
import io.enmasse.systemtest.resolvers.ExtensionContextParameterResolver;
import io.enmasse.systemtest.time.SystemtestsOperation;
import io.enmasse.systemtest.time.TimeMeasuringSystem;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.TestInfo;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.slf4j.Logger;

import java.util.Collections;
import java.util.Map;

@ExtendWith(ExtensionContextParameterResolver.class)
public interface ITestSeparator {
    Logger log = CustomLogger.getLogger();
    String separatorChar = "#";

    static void printThreadDump() {
        Map<Thread, StackTraceElement[]> allThreads = Thread.getAllStackTraces();
        for (Thread thread : allThreads.keySet()) {
            StringBuilder sb = new StringBuilder();
            Thread key = thread;
            StackTraceElement[] trace = allThreads.get(key);
            sb.append(key).append("\r\n");
            for (StackTraceElement aTrace : trace) {
                sb.append(" ").append(aTrace).append("\r\n");
            }
            log.error(sb.toString());
        }
    }

    @BeforeEach
    default void beforeEachTest(TestInfo testInfo) {
        TimeMeasuringSystem.setTestName(testInfo.getTestClass().get().getName(), testInfo.getTestMethod().get().getName());
        TimeMeasuringSystem.startOperation(SystemtestsOperation.TEST_EXECUTION);
        log.info(String.join("", Collections.nCopies(100, separatorChar)));
        log.info(String.format("%s.%s-STARTED", testInfo.getTestClass().get().getName(), testInfo.getDisplayName()));
    }

    @AfterEach
    default void afterEachTest(TestInfo testInfo, ExtensionContext context) {
        if (context.getExecutionException().isPresent()) { // on failed
            Throwable ex = context.getExecutionException().get();
            if (ex instanceof OutOfMemoryError) {
                log.error("Got OOM, dumping thread info");
                printThreadDump();
            } else {
                log.error("Caught exception", ex);
            }
        }
        TimeMeasuringSystem.stopOperation(SystemtestsOperation.TEST_EXECUTION);
        log.info(String.format("%s.%s-FINISHED", testInfo.getTestClass().get().getName(), testInfo.getDisplayName()));
        log.info(String.join("", Collections.nCopies(100, separatorChar)));
    }
}