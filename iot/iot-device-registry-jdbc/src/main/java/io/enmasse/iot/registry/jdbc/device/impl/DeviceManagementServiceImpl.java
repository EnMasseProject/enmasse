/*
 * Copyright 2019, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package io.enmasse.iot.registry.jdbc.device.impl;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import org.eclipse.hono.service.management.Id;
import org.eclipse.hono.service.management.OperationResult;
import org.eclipse.hono.service.management.Result;
import org.eclipse.hono.service.management.device.Device;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import io.enmasse.iot.registry.device.AbstractDeviceManagementService;
import io.enmasse.iot.registry.device.DeviceKey;
import io.opentracing.Span;

@Component
public class DeviceManagementServiceImpl extends AbstractDeviceManagementService {

    private static final Logger log = LoggerFactory.getLogger(DeviceManagementServiceImpl.class);

    @Autowired
    public DeviceManagementServiceImpl() {
    }

    @Override
    protected CompletableFuture<OperationResult<Id>> processCreateDevice(final DeviceKey key, final Device device, final Span span) {
        return null;
    }

    @Override
    protected CompletableFuture<OperationResult<Device>> processReadDevice(final DeviceKey key, final Span span) {
        return null;
    }

    @Override
    protected CompletableFuture<OperationResult<Id>> processUpdateDevice(final DeviceKey key, final Device device, final Optional<String> resourceVersion, final Span span) {
        return null;
    }

    protected CompletableFuture<Result<Void>> processDeleteDevice(final DeviceKey key, final Optional<String> resourceVersion, final Span span) {
        return null;
    }

}
