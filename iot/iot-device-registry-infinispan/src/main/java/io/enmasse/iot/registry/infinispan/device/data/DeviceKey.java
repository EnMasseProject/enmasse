/*
 * Copyright 2019, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package io.enmasse.iot.registry.infinispan.device.data;

import java.io.Serializable;
import java.util.Objects;

import org.infinispan.protostream.annotations.ProtoField;
import org.infinispan.protostream.annotations.ProtoMessage;

import com.google.common.base.MoreObjects;
import com.google.common.base.MoreObjects.ToStringHelper;

/**
 * A custom class to be used as key in the backend key-value storage.
 * This uses the unique values of a registration to create a unique key to store the registration
 * details.
 *
 * See {@link CacheRegistrationService CacheRegistrationService} class.
 */
@ProtoMessage
public class DeviceKey implements Serializable {

    private static final long serialVersionUID = 1L;

    @ProtoField(number = 1, required = true)
    protected String tenantId;
    @ProtoField(number = 2, required = true)
    protected String deviceId;

    protected DeviceKey() {}

    /**
     * Creates a new RegistrationKey. Used by CacheRegistrationService.
     *
     * @param tenantId the id of the tenant owning the registration key.
     * @param deviceId the id of the device being registered.
     */
    private DeviceKey(final String tenantId, final String deviceId) {
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
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        final DeviceKey that = (DeviceKey) o;
        return Objects.equals(tenantId, that.tenantId) &&
                Objects.equals(deviceId, that.deviceId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(
                this.tenantId,
                this.deviceId);
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

    public static DeviceKey deviceKey(final String tenantId, final String deviceId) {
        Objects.requireNonNull(tenantId);
        Objects.requireNonNull(deviceId);

        return new DeviceKey(tenantId, deviceId);
    }
}
