/*
 * Copyright 2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.systemtest.ability;

import io.enmasse.systemtest.*;
import io.enmasse.systemtest.resolvers.ExtensionContextParameterResolver;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.slf4j.Logger;

import java.io.File;
import java.util.Collections;
import java.util.Iterator;
import java.util.Map;

@ExtendWith(ExtensionContextParameterResolver.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public interface ITestSeparator {
    Logger log = CustomLogger.getLogger();
    String separatorChar = "#";

    @AfterAll
    default void afterAllTests() throws Exception {
        Environment env = new Environment();
        if (!env.skipCleanup()) {
            Kubernetes kube = Kubernetes.create(env);
            AddressApiClient apiClient = new AddressApiClient(kube);
            GlobalLogCollector logCollector = new GlobalLogCollector(kube, new File(env.testLogDir()));
            TestUtils.getAddressSpacesObjects(apiClient).forEach((addrSpace) -> {
                log.info("address space '{}' will be removed", addrSpace);
                try {
                    TestUtils.deleteAddressSpace(apiClient, addrSpace, logCollector);
                    TestUtils.waitForAddressSpaceDeleted(kube, addrSpace);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });
            apiClient.close();
        } else {
            log.warn("Remove address spaces when test run finished - SKIPPED!");
        }
        log.info("After all tests");
    }

    @BeforeEach
    default void beforeEachTest(TestInfo testInfo) {
        log.info(String.join("", Collections.nCopies(100, separatorChar)));
        log.info(String.format("%s.%s-STARTED", testInfo.getTestClass().get().getName(), testInfo.getTestMethod().get().getName()));
    }

    @AfterEach
    default void afterEachTest(TestInfo testInfo, ExtensionContext context) {
        if (context.getExecutionException().isPresent()) { // on failed
            Throwable ex = context.getExecutionException().get();
            if (ex instanceof OutOfMemoryError) {
                log.error("Got OOM, dumping thread info");
                printThreadDump();
            } else {
                log.error("Caught exception {}", ex);
            }
        }
        log.info(String.format("%s.%s-FINISHED", testInfo.getTestClass().get().getName(), testInfo.getTestMethod().get().getName()));
        log.info(String.join("", Collections.nCopies(100, separatorChar)));
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
            log.error(sb.toString());
        }
    }

}