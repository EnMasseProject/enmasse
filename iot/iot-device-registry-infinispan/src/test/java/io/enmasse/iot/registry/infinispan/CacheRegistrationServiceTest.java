/*
 * Copyright 2019, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package io.enmasse.iot.registry.infinispan;

import io.vertx.ext.unit.junit.VertxUnitRunner;
import org.eclipse.hono.service.registration.AbstractCompleteRegistrationServiceTest;
import org.eclipse.hono.service.registration.CompleteRegistrationService;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.rules.Timeout;
import org.junit.runner.RunWith;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

/**
 * Tests verifying behavior of {@link CacheRegistrationService}.
 *
 */
@Ignore
@RunWith(VertxUnitRunner.class)
public class CacheRegistrationServiceTest extends AbstractCompleteRegistrationServiceTest {

    private static CacheRegistrationService service;
    private static EmbeddedHotRodServer server;


    /**
     * Global timeout for all test cases.
     */
    @Rule
    public Timeout globalTimeout = new Timeout(5, TimeUnit.SECONDS);

    /**
     * Spin up the service using Infinispan EmbeddedCache.
     * @throws IOException if the Protobuf spec file cannot be found.
     */
    @Before
    public void setUp() throws IOException {

        server = new EmbeddedHotRodServer();
        service = new CacheRegistrationService(server.getCache());
    }

    /**
     *
     *
     * @return
     */
    @After
    public void cleanUp() {
        server.stop();
    }

    @Override
    public CompleteRegistrationService getCompleteRegistrationService() {
        return service;
    }


}
