/*
 * Copyright 2019, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package io.enmasse.iot.registry.infinispan;

import io.vertx.junit5.VertxExtension;

import org.eclipse.hono.service.tenant.AbstractCompleteTenantServiceTest;
import org.eclipse.hono.service.tenant.CompleteTenantService;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.extension.ExtendWith;
import java.io.IOException;

/**
 * Tests verifying behavior of {@link CacheTenantService}.
 *
 */
@Disabled
@ExtendWith(VertxExtension.class)
public class CacheTenantServiceTest extends AbstractCompleteTenantServiceTest {

    private static CacheTenantService service;
    private static EmbeddedHotRodServer server;

    /**
     * Spin up the service using Infinispan EmbeddedCache.
     * @throws IOException if the Protobuf spec file cannot be found.
     */
    @BeforeEach
    public void setUp() throws IOException {
        server = new EmbeddedHotRodServer();
        service = new CacheTenantService(server.getCache());
    }

    /**
     *
     *
     * @return
     */
    @AfterEach
    public void cleanUp() {
        server.stop();
    }

    @Override
    public CompleteTenantService getCompleteTenantService() {
        return service;
    }
}