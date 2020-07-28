/*
 * Copyright 2019, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package io.enmasse.iot.registry.tenant;

import java.util.Objects;
import java.util.Optional;

import org.eclipse.hono.service.management.tenant.Tenant;

import com.google.common.base.MoreObjects.ToStringHelper;

public class TenantInformation extends TenantHandle {

    private final Optional<Tenant> tenant;

    private final String namespace;
    private final String tenantName;

    protected TenantInformation(final String namespace, final String tenantName, final String id, final Tenant tenant) {
        super(namespace + "." + tenantName, id);
        this.namespace = namespace;
        this.tenantName = tenantName;
        this.tenant = Optional.ofNullable(tenant);
    }

    public Optional<Tenant> getTenant() {
        return this.tenant;
    }

    public String getNamespace() {
        return namespace;
    }

    public String getTenantName() {
        return tenantName;
    }

    @Override
    protected ToStringHelper toStringHelper() {
        return super.toStringHelper()
                .add("namespace", this.namespace)
                .add("projectName", this.tenantName)
                .add("tenant", this.tenant);
    }

    public static TenantInformation of(final String namespace, final String projectName, final String id, final Tenant tenant) {
        Objects.requireNonNull(namespace);
        Objects.requireNonNull(projectName);
        Objects.requireNonNull(id);
        return new TenantInformation(namespace, projectName, id, tenant);
    }

}
