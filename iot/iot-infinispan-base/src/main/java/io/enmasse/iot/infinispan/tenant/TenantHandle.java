/*
 * Copyright 2019, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package io.enmasse.iot.infinispan.tenant;

import java.util.Objects;

import org.eclipse.hono.service.management.tenant.Tenant;

import com.google.common.base.MoreObjects;
import com.google.common.base.MoreObjects.ToStringHelper;

public class TenantHandle {

    private final String name;
    private final String id;
    private final Tenant tenant;

    private TenantHandle(final String name, final String id, final Tenant tenant) {
        this.name = name;
        this.id = id;
        this.tenant = tenant;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public Tenant getTenant() {
        return tenant;
    }

    protected ToStringHelper toStringHelper() {
        return MoreObjects.toStringHelper(this)
                .add("name", this.name)
                .add("id", this.id)
                .add("tenant", this.tenant);
    }

    @Override
    public String toString() {
        return toStringHelper().toString();
    }

    public static TenantHandle of(final String name, final String id, final Tenant tenant) {
        Objects.requireNonNull(name);
        Objects.requireNonNull(id);
        return new TenantHandle(name, id, tenant);
    }

}
