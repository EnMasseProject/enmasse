/*
 * Copyright 2019, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package io.enmasse.iot.registry.jdbc.device.impl;

import java.util.concurrent.CompletableFuture;
import org.eclipse.hono.util.CredentialsResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import io.enmasse.iot.registry.device.AbstractCredentialsService;
import io.enmasse.iot.registry.device.CredentialKey;
import io.enmasse.iot.registry.jdbc.config.DeviceServiceProperties;
import io.enmasse.iot.registry.tenant.TenantInformation;
import io.opentracing.Span;
import io.vertx.core.json.JsonObject;

@Component
public class CredentialsServiceImpl extends AbstractCredentialsService {

    private static final Logger log = LoggerFactory.getLogger(CredentialsServiceImpl.class);

    private final DeviceServiceProperties properties;

    @Autowired
    public CredentialsServiceImpl(final DeviceServiceProperties properties) {
        this.properties = properties;
    }

    @Override
    protected CompletableFuture<CredentialsResult<JsonObject>> processGet(final TenantInformation tenant, final CredentialKey key, final Span span) {
        return null;
    }

}
