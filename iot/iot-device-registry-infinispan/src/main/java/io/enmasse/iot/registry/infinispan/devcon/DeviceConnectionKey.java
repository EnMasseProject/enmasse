/*
 * Copyright 2019, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package io.enmasse.iot.registry.infinispan.devcon;

import java.io.Serializable;
import java.util.Objects;

public class DeviceConnectionKey implements Serializable {

    private static final long serialVersionUID = 1L;

    private String tenantId;
    private String deviceId;

    public DeviceConnectionKey() {}

    public DeviceConnectionKey(String tenantId, String deviceId) {
        this.tenantId = tenantId;
        this.deviceId = deviceId;
    }

    public String getTenantId() {
        return tenantId;
    }

    public void setTenantId(String tenantId) {
        this.tenantId = tenantId;
    }

    public String getDeviceId() {
        return deviceId;
    }

    public void setDeviceId(String deviceId) {
        this.deviceId = deviceId;
    }

    @Override
    public int hashCode() {
        return Objects.hash(deviceId, tenantId);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        DeviceConnectionKey other = (DeviceConnectionKey) obj;
        return Objects.equals(deviceId, other.deviceId) && Objects.equals(tenantId, other.tenantId);
    }

}
