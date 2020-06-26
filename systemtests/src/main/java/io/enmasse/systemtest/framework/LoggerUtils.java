/*
 * Copyright 2019, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.systemtest.framework;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;

public class LoggerUtils {

    public static Logger getLogger() {

        StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();
        String callersClassName = stackTrace[2].getClassName();
        return LoggerFactory.getLogger(callersClassName);
    }

    public static void logDelimiter(String separatorChar) {
        getLogger().info(String.join("", Collections.nCopies(100, separatorChar)));
    }
}
