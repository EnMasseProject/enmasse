/*
 * Copyright 2019-2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package io.enmasse.systemtest.iot.isolated.http;

import io.enmasse.systemtest.iot.DeviceSupplier;
import io.enmasse.systemtest.iot.IoTTestSession;
import io.enmasse.systemtest.iot.http.StandardIoTHttpTests;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;

import java.util.Arrays;
import java.util.List;

import static io.enmasse.systemtest.framework.TestTag.IOT;
import static io.enmasse.systemtest.iot.DeviceSupplier.named;
import static io.enmasse.systemtest.iot.IoTTestSession.Adapter.HTTP;

@Tag(IOT)
class HttpAdapterTest implements StandardIoTHttpTests {

    private static IoTTestSession session;

    @BeforeAll
    public static void setup() throws Exception {
        session = IoTTestSession.createDefault()
                .adapters(HTTP)
                .deploy();
    }

    @AfterAll
    public static void cleanup() throws Exception {
        if (session != null) {
            session.close();
            session = null;
        }
    }

    @Override
    public List<DeviceSupplier> getDevices() {
        return Arrays.asList(
                named("default", () -> session.newDevice()
                        .register()
                        .setPassword()));
    }

    @Override
    public List<DeviceSupplier> getInvalidDevices() {
        return Arrays.asList(
                named("invalidPassword", () -> session.newDevice()
                        .register()
                        .setPassword()
                        .overridePassword()));
    }

    @Override
    public IoTTestSession getSession() {
        return session;
    }
}
