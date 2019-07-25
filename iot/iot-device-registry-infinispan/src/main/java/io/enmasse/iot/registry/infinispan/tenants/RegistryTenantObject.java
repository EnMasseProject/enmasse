/*
 * Copyright 2019, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package io.enmasse.iot.registry.infinispan.tenants;

import io.vertx.core.json.JsonObject;
import java.io.Serializable;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import org.eclipse.hono.util.TenantConstants;

/**
 * A custom class to be used as value in the backend key-value storage.
 * This store tenants details.
 *
 *  See {@link CacheTenantService CacheTenantService} class.
 */
public class RegistryTenantObject  implements Serializable {

    private static final long serialVersionUID = 1L;

    private String tenantId;
    private String tenantObject;
    private String version;

    /**
     * Create a a RegistryTenantObject with the Tenant details.
     * @param tenant the tenant object, in a {@link org.eclipse.hono.util.TenantObject Hono TenantObject util class}.
     */
    public RegistryTenantObject(final JsonObject tenant) {
        this.tenantObject = tenant.encode();
        this.tenantId = tenant.getString(TenantConstants.FIELD_PAYLOAD_TENANT_ID);
        this.version = UUID.randomUUID().toString();
    }

    public String getTenantObject() {
        return tenantObject;
    }

    public String getTenantId() {
        return tenantId;
    }

    public void setTenantId(String tenantId) {
        this.tenantId = tenantId;
    }

    public void setTenantObject(String tenantObject) {
        this.tenantObject = tenantObject;
    }

    public String getVersion() {
        return version;
    }

    public String getCertName(){
        final JsonObject tenant = new JsonObject(tenantObject);
        final JsonObject trustedCa = tenant.getJsonObject(TenantConstants.FIELD_PAYLOAD_SUBJECT_DN);

        if (trustedCa != null) {
            return trustedCa.getString(TenantConstants.FIELD_PAYLOAD_SUBJECT_DN);
        } else {
            return null;
        }
    }

    /**
     * Tests if this version matches the provided version.
     *
     * @param resourceVersion The provided version to check, may be {@link Optional#empty()}.
     * @return {@code true} if the provided version to check is empty or matches the current version, {@code false}
     *         otherwise.
     */
    public boolean isVersionMatch(final Optional<String> resourceVersion) {

        Objects.requireNonNull(resourceVersion);
        return resourceVersion.isEmpty() || resourceVersion.get().equals(this.version);
    }
}
