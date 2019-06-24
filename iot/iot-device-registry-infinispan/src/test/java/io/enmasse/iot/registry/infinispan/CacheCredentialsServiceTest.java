/*
 * Copyright 2019, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.iot.registry.infinispan;

import org.eclipse.hono.auth.SpringBasedHonoPasswordEncoder;
import org.eclipse.hono.service.credentials.AbstractCompleteCredentialsServiceTest;
import org.eclipse.hono.service.credentials.CompleteCredentialsService;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.extension.ExtendWith;

import io.vertx.junit5.VertxExtension;

import java.io.IOException;

/**
 * Tests verifying behavior of {@link CacheCredentialService}.
 *
 */
@Disabled
@ExtendWith(VertxExtension.class)
public class CacheCredentialsServiceTest extends AbstractCompleteCredentialsServiceTest {

    private static CacheCredentialService service;
    private static EmbeddedHotRodServer server;

    /**
     * Spin up the service using Infinispan EmbeddedCache.
     */
    @BeforeEach
    public void setUp() throws IOException {

        server = new EmbeddedHotRodServer();
        service = new CacheCredentialService(server.getCache(), new SpringBasedHonoPasswordEncoder());
    }

    /**
     * Stop the Embedded Infinispan Server.
     */
    @AfterEach
    public void cleanUp() {
        server.stop();
    }

    @Override
    public CompleteCredentialsService getCompleteCredentialsService() {
        return service;
    }
}
