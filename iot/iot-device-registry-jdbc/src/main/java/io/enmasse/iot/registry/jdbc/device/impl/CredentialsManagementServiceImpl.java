/*
 * Copyright 2019, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package io.enmasse.iot.registry.jdbc.device.impl;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import org.eclipse.hono.auth.HonoPasswordEncoder;
import org.eclipse.hono.service.management.OperationResult;
import org.eclipse.hono.service.management.credentials.CommonCredential;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import io.enmasse.iot.registry.device.AbstractCredentialsManagementService;
import io.enmasse.iot.registry.device.DeviceKey;
import io.opentracing.Span;

@Component
public class CredentialsManagementServiceImpl extends AbstractCredentialsManagementService {

    @Autowired
    public CredentialsManagementServiceImpl(final HonoPasswordEncoder passwordEncoder) {
        super(passwordEncoder);
    }

    @Override
    protected CompletableFuture<OperationResult<Void>> processSet(final DeviceKey key, final Optional<String> resourceVersion,
            final List<CommonCredential> credentials, final Span span) {

        return null;

    }

    @Override
    protected CompletableFuture<OperationResult<List<CommonCredential>>> processGet(final DeviceKey key, final Span span) {

        return null;

    }

}
