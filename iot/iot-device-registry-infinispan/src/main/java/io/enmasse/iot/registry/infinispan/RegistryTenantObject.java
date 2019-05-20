/*
 * Copyright 2019, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package io.enmasse.iot.registry.infinispan;

import io.vertx.core.json.JsonObject;
import org.eclipse.hono.util.TenantObject;

import java.io.Serializable;
import org.infinispan.protostream.annotations.ProtoDoc;
import org.infinispan.protostream.annotations.ProtoField;

/**
 * A custom class to be used as value in the backend key-value storage.
 * This store tenants details.
 *
 *  See {@link CacheTenantService CacheTenantService} class.
 */
@ProtoDoc("@Indexed")
public class RegistryTenantObject implements Serializable {

    private static final long serialVersionUID = 1L;

    private String tenantId;
    private String trustedCa;
    private String tenantObject;

    /**
     *  Constructor without arguments for the protobuilder.
     */
    public RegistryTenantObject() {
    }

    /**
     * Create a a RegistryTenantObject with the Tenant details.
     * @param tenant the tenant object, in a {@link org.eclipse.hono.util.TenantObject Hono TenantObject util class}.
     */
    public RegistryTenantObject(final TenantObject tenant) {
        this.tenantId = tenant.getTenantId();

        if (tenant.getTrustedCaSubjectDn() != null ){
            this.trustedCa = tenant.getTrustedCaSubjectDn().getName();
        } else {
            this.trustedCa = null;
        }

        this.tenantObject = JsonObject.mapFrom(tenant).encode();
    }

    @ProtoDoc("@Field")
    @ProtoField(number = 3, required = true)
    public String getTenantObject() {
        return tenantObject;
    }

    @ProtoDoc("@Field")
    @ProtoField(number = 1, required = true)
    public String getTenantId() {
        return tenantId;
    }

    // Matching TenantConstants.FIELD_PAYLOAD_TRUSTED_CA;
    @ProtoDoc("@Field")
    @ProtoField(number = 2)
    public String getTrustedCa() {
        return trustedCa;
    }

    public void setTenantId(String tenantId) {
        this.tenantId = tenantId;
    }

    public void setTrustedCa(String trustedCa) {
        this.trustedCa = trustedCa;
    }

    public void setTenantObject(String tenantObject) {
        this.tenantObject = tenantObject;
    }
}
