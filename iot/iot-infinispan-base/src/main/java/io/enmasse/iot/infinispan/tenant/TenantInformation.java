/*
 * Copyright 2019, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package io.enmasse.iot.infinispan.tenant;

import java.util.Objects;
import java.util.Optional;

import org.eclipse.hono.service.management.tenant.Tenant;

import com.google.common.base.MoreObjects.ToStringHelper;

public class TenantInformation extends TenantHandle {

    private final Optional<Tenant> tenant;

    protected TenantInformation(final String name, final String id, final Tenant tenant) {
        super(name,id);
        this.tenant = Optional.ofNullable(tenant);
    }

    public Optional<Tenant> getTenant() {
        return this.tenant;
    }

    @Override
    protected ToStringHelper toStringHelper() {
        return super.toStringHelper()
                .add("tenant", this.tenant);
    }

    public static TenantInformation of(final String name, final String id, final Tenant tenant) {
        Objects.requireNonNull(name);
        Objects.requireNonNull(id);
        return new TenantInformation(name, id, tenant);
    }

}
