/*
 * Copyright 2019, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.iot.registry.infinispan.credentials;

import org.eclipse.hono.auth.SpringBasedHonoPasswordEncoder;
import org.eclipse.hono.service.credentials.AbstractCompleteCredentialsServiceTest;
import org.eclipse.hono.service.credentials.CompleteCredentialsService;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.extension.ExtendWith;

import io.enmasse.iot.registry.infinispan.EmbeddedHotRodServer;
import io.enmasse.iot.registry.infinispan.credentials.CacheCredentialService;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;

import java.io.IOException;

/**
 * Tests verifying behavior of {@link CacheCredentialService}.
 *
 */
@ExtendWith(VertxExtension.class)
public class CacheCredentialsServiceTest extends AbstractCompleteCredentialsServiceTest {

    private CacheCredentialService service;
    private EmbeddedHotRodServer server;

    /**
     * Spin up the service using Infinispan EmbeddedCache.
     */
    @BeforeEach
    public void setUp() throws IOException {
        server = new EmbeddedHotRodServer();
        service = new CacheCredentialService(server.getCache("credentials"), new SpringBasedHonoPasswordEncoder());
    }

    @AfterEach
    public void cleanUp() throws Exception {
        server.stop();
    }

    @Override
    public CompleteCredentialsService getCompleteCredentialsService() {
        return service;
    }

    @Disabled("We don't support access by device-id")
    @Override
    public void testRemoveCredentialsByDeviceSucceeds(VertxTestContext ctx) {
        super.testRemoveCredentialsByDeviceSucceeds(ctx);
    }

    @Disabled("We don't use client contexts")
    @Override
    public void testGetCredentialsFailsForWrongClientContext(VertxTestContext ctx) {
        super.testGetCredentialsFailsForWrongClientContext(ctx);
    }
}
