/*
 * Copyright 2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package io.enmasse.systemtest.iot;

import java.util.List;

public interface StandardIoTTests extends IoTTests {

    /**
     * Test the test session.
     */
    IoTTestSession getSession();

    /**
     * Get a list of devices which must succeed.
     */
    List<DeviceSupplier> getDevices();

    /**
     * Get a list of devices which must fail.
     */
    List<DeviceSupplier> getInvalidDevices();

}
