/*
 * Copyright 2019, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package io.enmasse.iot.registry.infinispan.device.data;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

import org.infinispan.protostream.annotations.ProtoDoc;
import org.infinispan.protostream.annotations.ProtoFactory;
import org.infinispan.protostream.annotations.ProtoField;
import org.infinispan.protostream.annotations.ProtoMessage;

import com.google.common.base.MoreObjects;
import com.google.common.base.MoreObjects.ToStringHelper;

import io.vertx.core.json.JsonObject;

@ProtoDoc("@Indexed")
public class DeviceInformation implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * Our own tenant id.
     * <br>
     * Although this information is redundant, it is required for indexing.
     */
    @ProtoDoc("@Field(index=Index.YES)")
    @ProtoField(number = 1, required = true)
    protected String tenantId;

    /**
     * Our own device id.
     * <br>
     * Required only because the query API of Infinispan doesn't return the key for a value when doing a
     * query.
     */
    @ProtoDoc("@Field(index=Index.NO)")
    @ProtoField(number = 2, required = true)
    protected String deviceId;

    /**
     * Resource version for the device registration info.
     */
    @ProtoDoc("@Field(index=Index.NO)")
    @ProtoField(number = 3)
    protected String version = UUID.randomUUID().toString();

    /**
     * A Json Object containing the registration information.
     */
    @ProtoDoc("@Field(index=Index.NO)")
    @ProtoField(number = 4)
    protected String registrationInformation;

    /**
     * The credentials, in our internal encoding.
     */
    @ProtoDoc("@Field(index=Index.YES)")
    @ProtoField(number = 5, collectionImplementation = ArrayList.class)
    protected List<DeviceCredential> credentials;

    public DeviceInformation() {}

    public void setTenantId(String tenantId) {
        this.tenantId = tenantId;
    }

    public String getTenantId() {
        return tenantId;
    }

    public void setDeviceId(String deviceId) {
        this.deviceId = deviceId;
    }

    public String getDeviceId() {
        return deviceId;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public void setCredentials(List<DeviceCredential> credentials) {
        this.credentials = credentials;
    }

    public List<DeviceCredential> getCredentials() {
        return credentials;
    }

    /**
     * Tests if this version matches the provided version.
     *
     * @param resourceVersion The provided version to check, may be {@link Optional#empty()}.
     * @return {@code true} if the provided version to check is empty or matches the current version,
     *         {@code false}
     *         otherwise.
     */
    public boolean isVersionMatch(final Optional<String> resourceVersion) {

        Objects.requireNonNull(resourceVersion);

        if (resourceVersion.isEmpty()) {
            return true;
        }

        return resourceVersion.get().equals(this.version);
    }

    public void setRegistrationInformation(String registrationInformation) {
        this.registrationInformation = registrationInformation;
    }

    public String getRegistrationInformation() {
        return registrationInformation;
    }

    public JsonObject getRegistrationInformationAsJson() {
        return registrationInformation != null ? new JsonObject(registrationInformation) : null;
    }

    public DeviceInformation newVersion() {
        final var result = new DeviceInformation();
        result.tenantId = this.tenantId;
        result.deviceId = this.deviceId;
        result.setVersion(UUID.randomUUID().toString());
        result.registrationInformation = this.registrationInformation;
        result.credentials = this.credentials != null ? new ArrayList<>(this.credentials) : null;
        return result;
    }

    protected ToStringHelper toStringHelper() {
        return MoreObjects.toStringHelper(this)
                .add("tenantId", this.tenantId)
                .add("deviceId", this.deviceId)
                .add("version", this.version)
                .add("registrationInformation", this.registrationInformation)
                .add("credentials", this.credentials);
    }

    @Override
    public String toString() {
        return toStringHelper().toString();
    }
}
