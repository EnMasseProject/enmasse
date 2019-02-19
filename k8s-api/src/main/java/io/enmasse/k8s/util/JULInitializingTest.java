/*
 * Copyright 2017-2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.k8s.util;

import org.slf4j.bridge.SLF4JBridgeHandler;

/**
 * Initializes the JUL logger bridge, required for test using OpenShiftServer, or other classes using JUL.
 */
public class JULInitializingTest {

    static {
        SLF4JBridgeHandler.removeHandlersForRootLogger();
        SLF4JBridgeHandler.install();
    }

}
