/*
 * Copyright 2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package io.enmasse.systemtest.iot;

import java.util.List;

import io.enmasse.systemtest.iot.IoTTestSession.Device;

public interface StandardIoTTests extends IoTTests {

    /**
     * Test the test session.
     */
    public IoTTestSession getSession();

    /**
     * Get a list of devices which must succeed.
     */
    public List<Device> getDevices() throws Exception;

    /**
     * Get a list of devices which must fail.
     */
    public List<Device> getInvalidDevices() throws Exception;

}
