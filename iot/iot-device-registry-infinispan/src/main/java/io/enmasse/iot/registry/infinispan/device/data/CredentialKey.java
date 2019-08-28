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

import io.enmasse.iot.registry.infinispan.tenant.TenantHandle;

/**
 * A custom class to be used as key in the backend key-value storage.
 * This uses the uniques values of a credential to create a unique key to store the credentials
 * details.
 *
 * See {@link CacheCredentialService CacheCredentialService} class.
 */
@ProtoMessage
public class CredentialKey implements Serializable {

    private static final long serialVersionUID = 1L;

    @ProtoField(number = 1, required = true)
    protected String tenantId;
    @ProtoField(number = 2, required = true)
    protected String authId;
    @ProtoField(number = 3, required = true)
    protected String type;

    /**
     * No-arg constructor for protobuf.
     */
    protected CredentialKey() {}

    /**
     * Creates a new CredentialsKey. Used by CacheCredentialsService.
     *
     * @param tenantId the id of the tenant owning the registration key.
     * @param authId the auth-id used in the credential.
     * @param type the the type of the credential.
     */
    private CredentialKey(final String tenantId, final String authId, final String type) {
        this.tenantId = tenantId;
        this.authId = authId;
        this.type = type;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        final CredentialKey that = (CredentialKey) o;
        return Objects.equals(this.tenantId, that.tenantId) &&
                Objects.equals(this.authId, that.authId) &&
                Objects.equals(this.type, that.type);
    }

    @Override
    public int hashCode() {
        return Objects.hash(tenantId, authId, type);
    }

    public void setTenantId(String tenantId) {
        this.tenantId = tenantId;
    }

    public void setAuthId(String authId) {
        this.authId = authId;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getTenantId() {
        return tenantId;
    }

    public String getAuthId() {
        return authId;
    }

    public String getType() {
        return type;
    }

    protected ToStringHelper toStringHelper() {
        return MoreObjects.toStringHelper(this)
                .add("tenantId", this.tenantId)
                .add("authId", this.authId)
                .add("type", this.type);
    }

    @Override
    public String toString() {
        return toStringHelper().toString();
    }

    public static CredentialKey credentialKey(final String tenantId, final String authId, final String type) {
        Objects.requireNonNull(tenantId);
        Objects.requireNonNull(authId);
        Objects.requireNonNull(type);
        return new CredentialKey(tenantId, authId, type);
    }

    public static CredentialKey credentialKey(final TenantHandle tenantHandle, final String authId, final String type) {
        Objects.requireNonNull(tenantHandle);
        Objects.requireNonNull(authId);
        Objects.requireNonNull(type);
        return new CredentialKey(tenantHandle.getId(), authId, type);
    }
}
