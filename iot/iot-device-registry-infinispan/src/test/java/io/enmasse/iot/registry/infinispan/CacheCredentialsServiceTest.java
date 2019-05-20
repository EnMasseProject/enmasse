/*
 * Copyright 2019, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.iot.registry.infinispan;

import io.vertx.ext.unit.junit.VertxUnitRunner;
import org.eclipse.hono.auth.SpringBasedHonoPasswordEncoder;
import org.eclipse.hono.service.credentials.AbstractCompleteCredentialsServiceTest;
import org.eclipse.hono.service.credentials.CompleteCredentialsService;

import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.rules.Timeout;
import org.junit.runner.RunWith;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

/**
 * Tests verifying behavior of {@link CacheCredentialService}.
 *
 */
@Ignore
@RunWith(VertxUnitRunner.class)
public class CacheCredentialsServiceTest extends AbstractCompleteCredentialsServiceTest {

    private static CacheCredentialService service;
    private static EmbeddedHotRodServer server;

    /**
     * Global timeout for all test cases.
     */
    @Rule
    public Timeout globalTimeout = new Timeout(30, TimeUnit.SECONDS);


    /**
     * Spin up the service using Infinispan EmbeddedCache.
     */
    @Before
    public void setUp() throws IOException {

        server = new EmbeddedHotRodServer();
        service = new CacheCredentialService(server.getCache(), new SpringBasedHonoPasswordEncoder());
    }

    /**
     * Stop the Embedded Infinispan Server.
     */
    @After
    public void cleanUp() {
        server.stop();
    }

    @Override
    public CompleteCredentialsService getCompleteCredentialsService() {
        return service;
    }
}
