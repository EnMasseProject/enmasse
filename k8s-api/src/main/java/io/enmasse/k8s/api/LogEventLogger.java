/*
 * Copyright 2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.k8s.api;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LogEventLogger implements EventLogger {
    private static final Logger log = LoggerFactory.getLogger(LogEventLogger.class);

    @Override
    public void log(Reason reason, String message, Type type, ObjectKind objectKind, String objectName) {
        log(log, reason, message, type, objectKind, objectName);
    }

    public static void log(Logger logger, Reason reason, String message, Type type, ObjectKind objectKind, String objectName) {
        String line = String.format("%s (kind=%s name=%s): %s", reason.name(), objectKind.name(), objectName, message);
        switch (type) {
            case Warning:
                logger.warn(line);
                break;
            case Normal:
                logger.info(line);
                break;
        }
    }
}
