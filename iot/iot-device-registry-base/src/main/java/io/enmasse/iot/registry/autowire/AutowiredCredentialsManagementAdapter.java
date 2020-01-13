/*
 * Copyright 2019, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package io.enmasse.iot.registry.autowire;

import org.eclipse.hono.service.management.credentials.CredentialsManagementService;
import org.eclipse.hono.service.management.credentials.EventBusCredentialsManagementAdapter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Component;

/**
 * A default event bus based service implementation of the {@link CredentialsManagementService}.
 * <p>
 * This wires up the actual service instance with the mapping to the event bus implementation. It is intended to be used
 * in a Spring Boot environment.
 */
@Component
@ConditionalOnBean(CredentialsManagementService.class)
public final class AutowiredCredentialsManagementAdapter extends EventBusCredentialsManagementAdapter {

    private CredentialsManagementService service;

    @Autowired
    public void setService(final CredentialsManagementService service) {
        this.service = service;
    }

    @Override
    protected CredentialsManagementService getService() {
        return this.service;
    }

}

