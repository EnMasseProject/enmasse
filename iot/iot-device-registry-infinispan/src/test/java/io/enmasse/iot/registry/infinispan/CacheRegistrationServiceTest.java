/*
 * Copyright 2019, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package io.enmasse.iot.registry.infinispan;

import org.eclipse.hono.service.registration.AbstractCompleteRegistrationServiceTest;
import org.eclipse.hono.service.registration.CompleteRegistrationService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.extension.ExtendWith;

import io.vertx.junit5.VertxExtension;

import java.io.IOException;

/**
 * Tests verifying behavior of {@link CacheRegistrationService}.
 *
 */
@Disabled
@ExtendWith(VertxExtension.class)
public class CacheRegistrationServiceTest extends AbstractCompleteRegistrationServiceTest {

    private static CacheRegistrationService service;
    private static EmbeddedHotRodServer server;

    /**
     * Spin up the service using Infinispan EmbeddedCache.
     * @throws IOException if the Protobuf spec file cannot be found.
     */
    @BeforeEach
    public void setUp() throws IOException {

        server = new EmbeddedHotRodServer();
        service = new CacheRegistrationService(server.getCache());
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
    public CompleteRegistrationService getCompleteRegistrationService() {
        return service;
    }


}
