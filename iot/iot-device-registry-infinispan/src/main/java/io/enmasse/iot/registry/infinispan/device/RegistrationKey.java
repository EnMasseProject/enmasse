/*
 * Copyright 2019, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package io.enmasse.iot.registry.infinispan.device;

import java.io.Serializable;
import java.util.Objects;

/**
 * A custom class to be used as key in the backend key-value storage.
 * This uses the unique values of a registration to create a unique key to store the registration details.
 *
 *  See {@link CacheRegistrationService CacheRegistrationService} class.
 */
public class RegistrationKey implements Serializable {

    private static final long serialVersionUID = 1L;

    private String tenantId;
    private String deviceId;

    public RegistrationKey() {
    }

    /**
     * Creates a new RegistrationKey. Used by CacheRegistrationService.
     *
     * @param tenantId the id of the tenant owning the registration key.
     * @param deviceId the id of the device being registered.
     */
    public RegistrationKey(final String tenantId, final String deviceId) {
        this.tenantId = tenantId;
        this.deviceId = deviceId;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o){
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        final RegistrationKey that = (RegistrationKey) o;
        return Objects.equals(tenantId, that.tenantId) &&
                Objects.equals(deviceId, that.deviceId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(tenantId, deviceId);
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
}
