/*
 * Copyright 2019, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package io.enmasse.iot.registry.infinispan;

import io.vertx.core.json.JsonObject;
import org.eclipse.hono.util.CredentialsObject;

import java.io.Serializable;
import org.infinispan.protostream.annotations.ProtoDoc;
import org.infinispan.protostream.annotations.ProtoField;

/**
 * A custom class to be used as value in the backend key-value storage.
 * This store credentials details.
 *
 *  See {@link CacheTenantService CacheTenantService} class.
 */
@ProtoDoc("@Indexed")
public class RegistryCredentialObject implements Serializable {

    private static final long serialVersionUID = 1L;

    private String tenantId;
    private String deviceId;
    private String originalJson;

    /**
     * Constructor without arguments for the protobuilder.
     */
    public RegistryCredentialObject(){
    }

    /**
     * Create a a RegistryCredentialObject with the credentials details.
     *
     * @param honoCredential the credential object, in a {@link org.eclipse.hono.util.CredentialsObject Hono CredentialsObject util class}.
     * @param tenantId the tenant ID associated with the credential.
     * @param originalJson the raw JSON object contained in the original creation request.
     */
    public RegistryCredentialObject(final CredentialsObject honoCredential, final String tenantId, final JsonObject originalJson){
        this.tenantId = tenantId;
        this.deviceId = honoCredential.getDeviceId();
        this.originalJson = originalJson.encode();
    }

    @ProtoDoc("@Field")
    @ProtoField(number = 3, required = true)
    public String getOriginalJson() {
        return originalJson;
    }

    @ProtoDoc("@Field")
    @ProtoField(number = 2, required = true)
    public String getDeviceId(){
        return deviceId;
    }

    @ProtoDoc("@Field")
    @ProtoField(number = 1, required = true)
    public String getTenantId() {
        return tenantId;
    }

    public void setTenantId(String tenantId) {
        this.tenantId = tenantId;
    }

    public void setDeviceId(String deviceId) {
        this.deviceId = deviceId;
    }

    public void setOriginalJson(String originalJson) {
        this.originalJson = originalJson;
    }
}
