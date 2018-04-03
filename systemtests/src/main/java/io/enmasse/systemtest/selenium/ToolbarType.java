/*
 * Copyright 2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.systemtest.selenium;

public enum ToolbarType {
    ADDRESSES {
        public String toString() {
            return "exampleToolbar";
        }
    },
    CONNECTIONS {
        public String toString() {
            return "connectionToolbar";
        }
    };
}
