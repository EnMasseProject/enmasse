/*
 * Copyright 2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package io.enmasse.systemtest.iot;

import io.enmasse.systemtest.iot.IoTTestSession.ProjectInstance.Device;
import io.enmasse.systemtest.utils.TestUtils;

import java.util.UUID;

public interface DeviceFactory {

    /**
     * Start creating a new device.
     *
     * @param deviceId The ID of the device to create.
     * @return The new device creation instance. The device will only be created when the
     * {@link Device#register()} method is being called.
     */
    Device newDevice(final String deviceId);

    default Device newDevice() {
        return newDevice(Names.randomDevice());
    }

    default Device registerNewRandomDeviceWithPassword() throws Exception {
        return newDevice()
                .register()
                .setPassword();
    }

}
