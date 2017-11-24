/*
 * Copyright 2017 Red Hat Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
