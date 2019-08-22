/*
 * Copyright 2019, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package io.enmasse.iot.registry.infinispan.autowire;

import org.eclipse.hono.service.management.device.DeviceManagementService;
import org.eclipse.hono.service.management.device.EventBusDeviceManagementAdapter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Component;

/**
 * A default event bus based service implementation of the {@link DeviceManagementService}.
 * <p>
 * This wires up the actual service instance with the mapping to the event bus implementation. It is intended to be used
 * in a Spring Boot environment.
 */
@Component
@ConditionalOnBean(DeviceManagementService.class)
public final class AutowiredDeviceManagementAdapter extends EventBusDeviceManagementAdapter<Void> {

    private DeviceManagementService service;

    @Autowired
    public void setService(final DeviceManagementService service) {
        this.service = service;
    }

    @Override
    protected DeviceManagementService getService() {
        return this.service;
    }

    @Override
    public void setConfig(final Void configuration) {
    }

}
