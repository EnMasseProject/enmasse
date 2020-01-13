/*
 * Copyright 2019, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package io.enmasse.iot.registry.jdbc.device.impl;

import java.util.concurrent.CompletableFuture;

import org.eclipse.hono.util.RegistrationResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import io.enmasse.iot.registry.device.AbstractRegistrationService;
import io.enmasse.iot.registry.device.DeviceKey;
import io.opentracing.Span;

@Component
public class RegistrationServiceImpl extends AbstractRegistrationService {

    private static final Logger log = LoggerFactory.getLogger(RegistrationServiceImpl.class);

    public RegistrationServiceImpl() {
    }

    @Override
    protected CompletableFuture<RegistrationResult> processGetDevice(final DeviceKey key, final Span span) {
        return null;
    }

}
