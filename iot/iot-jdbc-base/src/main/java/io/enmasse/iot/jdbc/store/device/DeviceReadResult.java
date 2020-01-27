/*
 * Copyright 2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package io.enmasse.iot.jdbc.store.device;

import java.util.Optional;

import org.eclipse.hono.service.management.device.Device;

import com.google.common.base.MoreObjects;

public class DeviceReadResult {
    private Device device;
    private Optional<String> resourceVersion;

    public DeviceReadResult(Device device, Optional<String> resourceVersion) {
        this.device = device;
        this.resourceVersion = resourceVersion;
    }

    public Device getDevice() {
        return this.device;
    }

    public Optional<String> getResourceVersion() {
        return this.resourceVersion;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("resourceVersion", this.resourceVersion)
                .add("device", this.device)
                .toString();
    }
}
