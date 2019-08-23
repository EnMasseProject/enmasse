/*
 * Copyright 2018, 2019, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package io.enmasse.iot.tenant.impl;

import org.eclipse.hono.service.management.tenant.TenantManagementService;
import org.eclipse.hono.service.tenant.EventBusTenantAdapter;
import org.eclipse.hono.service.tenant.TenantService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * A default event bus based service implementation of the {@link TenantManagementService}.
 * <p>
 * This wires up the actual service instance with the mapping to the event bus implementation. It is intended to be used
 * in a Spring Boot environment.
 */
@Component
public final class AutowiredTenantAdapter extends EventBusTenantAdapter {

    private TenantService service;

    @Autowired
    public void setService(final TenantService service) {
        this.service = service;
    }

    @Override
    protected TenantService getService() {
        return this.service;
    }

}
