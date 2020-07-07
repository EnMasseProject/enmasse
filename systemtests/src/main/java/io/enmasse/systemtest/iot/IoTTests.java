/*
 * Copyright 2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package io.enmasse.systemtest.iot;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.TestInstance;

import io.enmasse.systemtest.IndicativeSentences;
import io.enmasse.systemtest.TestCleaner;
import io.enmasse.systemtest.framework.ITestSeparator;
import io.enmasse.systemtest.platform.Kubernetes;

/**
 * Base interface for IoT tests.
 */
@DisplayNameGeneration(IndicativeSentences.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public interface IoTTests extends ITestSeparator, TestCleaner {

    String IOT_PROJECT_NAMESPACE = System.getProperty("io.enmasse.systemtest.iot.namespace", "iot-system-tests");

    @BeforeAll
    static void deployDefaultCerts() {
        IoTTestSession.deployDefaultCerts();
    }

    @BeforeAll
    static void createDeviceManager() throws Exception {
        DeviceManagementApi.createManagementServiceAccount();
    }

    @BeforeEach
    default void createNamespace() {
        Kubernetes.getInstance().createNamespace(IOT_PROJECT_NAMESPACE);
    }

}
