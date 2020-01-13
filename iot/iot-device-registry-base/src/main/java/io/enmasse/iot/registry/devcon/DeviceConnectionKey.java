/*
 * Copyright 2019, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package io.enmasse.iot.registry.devcon;

import java.util.Objects;

import com.google.common.base.MoreObjects;
import com.google.common.base.MoreObjects.ToStringHelper;

public final class DeviceConnectionKey {

    private final String tenantId;
    private final String deviceId;

    private DeviceConnectionKey(String tenantId, String deviceId) {
        this.tenantId = tenantId;
        this.deviceId = deviceId;
    }

    public String getTenantId() {
        return this.tenantId;
    }

    public String getDeviceId() {
        return this.deviceId;
    }

    @Override
    public int hashCode() {
        return Objects.hash(
                this.deviceId,
                this.tenantId);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        DeviceConnectionKey other = (DeviceConnectionKey) obj;
        return Objects.equals(this.deviceId, other.deviceId) &&
                Objects.equals(this.tenantId, other.tenantId);
    }

    protected ToStringHelper toStringHelper() {
        return MoreObjects.toStringHelper(this)
                .add("tenantId", this.tenantId)
                .add("deviceId", this.deviceId);
    }

    @Override
    public String toString() {
        return toStringHelper().toString();
    }

    public static DeviceConnectionKey deviceConnectionKey(final String tenantId, final String deviceId) {
        Objects.requireNonNull(tenantId);
        Objects.requireNonNull(deviceId);
        return new DeviceConnectionKey(tenantId, deviceId);
    }

}
