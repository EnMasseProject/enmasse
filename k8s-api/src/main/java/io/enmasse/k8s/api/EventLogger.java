/*
 * Copyright 2017-2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.k8s.api;

/**
 * Interface for handling events
 */
public interface EventLogger {

    interface Reason {
        String name();
    }

    enum Type {
        Normal,
        Warning
    }

    interface ObjectKind {
        String name();
    }

    /**
     * Log an event.
     *
     * @param reason any of {@link Reason}
     * @param message Descriptive message
     * @param type any of {@link Type}
     * @param objectKind any of {@link ObjectKind}
     * @param objectName Name of object involved in event
     */
    void log(Reason reason, String message, Type type, ObjectKind objectKind, String objectName);
}
