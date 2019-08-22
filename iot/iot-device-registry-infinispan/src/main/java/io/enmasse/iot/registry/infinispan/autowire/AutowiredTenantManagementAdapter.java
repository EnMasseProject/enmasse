/*
 * Copyright 2019, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package io.enmasse.iot.registry.infinispan.autowire;

import org.eclipse.hono.service.management.tenant.EventBusTenantManagementAdapter;
import org.eclipse.hono.service.management.tenant.TenantManagementService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Component;

/**
 * A default event bus based service implementation of the {@link TenantManagementService}.
 * <p>
 * This wires up the actual service instance with the mapping to the event bus implementation. It is intended to be used
 * in a Spring Boot environment.
 */
@Component
@ConditionalOnBean(TenantManagementService.class)
public final class AutowiredTenantManagementAdapter extends EventBusTenantManagementAdapter<Void> {

    private TenantManagementService service;

    @Autowired
    public void setService(final TenantManagementService service) {
        this.service = service;
    }

    @Override
    protected TenantManagementService getService() {
        return this.service;
    }

    @Override
    public void setConfig(final Void configuration) {
    }

}
