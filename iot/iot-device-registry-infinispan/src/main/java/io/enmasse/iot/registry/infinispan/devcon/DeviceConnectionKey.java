/*
 * Copyright 2019, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package io.enmasse.iot.registry.infinispan.devcon;

import java.io.Serializable;
import java.util.Objects;

import com.google.common.base.MoreObjects;
import com.google.common.base.MoreObjects.ToStringHelper;
import org.infinispan.protostream.annotations.ProtoFactory;
import org.infinispan.protostream.annotations.ProtoField;

public class DeviceConnectionKey implements Serializable {

    private static final long serialVersionUID = 1L;

    @ProtoField(number = 1, required = true)
    protected String tenantId;
    @ProtoField(number = 2, required = true)
    protected String deviceId;


    @ProtoFactory
    public DeviceConnectionKey(String tenantId, String deviceId) {
        this.tenantId = tenantId;
        this.deviceId = deviceId;
    }

    public String getTenantId() {
        return this.tenantId;
    }

    public void setTenantId(String tenantId) {
        this.tenantId = tenantId;
    }

    public String getDeviceId() {
        return this.deviceId;
    }

    public void setDeviceId(String deviceId) {
        this.deviceId = deviceId;
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

}
