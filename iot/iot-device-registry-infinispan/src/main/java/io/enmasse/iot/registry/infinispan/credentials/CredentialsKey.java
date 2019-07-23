/*
 * Copyright 2019, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package io.enmasse.iot.registry.infinispan.credentials;

import java.io.Serializable;
import java.util.Objects;

/**
 * A custom class to be used as key in the backend key-value storage.
 * This uses the uniques values of a credential to create a unique key to store the credentials details.
 *
 *  See {@link CacheCredentialService CacheCredentialService} class.
 */
public class CredentialsKey implements Serializable {

    private static final long serialVersionUID = 1L;

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

    public String getTenantId() {
        return tenantId;
    }

    public String getAuthId() {
        return authId;
    }

    public String getType() {
        return type;
    }
}
