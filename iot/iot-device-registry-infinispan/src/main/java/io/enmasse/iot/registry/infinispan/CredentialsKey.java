/*
 * Copyright 2019, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package io.enmasse.iot.registry.infinispan;

import java.util.Objects;
import org.infinispan.protostream.annotations.ProtoDoc;
import org.infinispan.protostream.annotations.ProtoField;

/**
 * A custom class to be used as key in the backend key-value storage.
 * This uses the uniques values of a credential to create a unique key to store the credentials details.
 *
 *  See {@link CacheCredentialsService CacheCredentialService} class.
 */
@ProtoDoc("@Indexed")
public class CredentialsKey {

    private String tenantId;
    private String authId;
    private String type;

    /**
     * Constructor without arguments for the protobuilder.
     */
    public CredentialsKey() {
    }

    /**
     * Creates a new CredentialsKey. Used by CacheCredentialsService.
     *
     * @param tenantId the id of the tenant owning the registration key.
     * @param authId the auth-id used in the credential.
     * @param type the the type of the credential.
     */
    public CredentialsKey(final String tenantId, final String authId, final String type) {
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
        final CredentialsKey that = (CredentialsKey) o;
        return Objects.equals(tenantId, that.tenantId) &&
                Objects.equals(authId, that.authId) &&
                Objects.equals(type, that.type);
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

    @ProtoDoc("@Field")
    @ProtoField(number = 1, required = true)
    public String getTenantId() {
        return tenantId;
    }

    @ProtoDoc("@Field")
    @ProtoField(number = 2, required = true)
    public String getAuthId() {
        return authId;
    }

    @ProtoDoc("@Field")
    @ProtoField(number = 3, required = true)
    public String getType() {
        return type;
    }
}
