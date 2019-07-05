/*
 * Copyright 2019, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.iot.registry.infinispan;

import org.eclipse.hono.service.credentials.AbstractCredentialsServiceTest;

import org.eclipse.hono.service.credentials.CredentialsService;
import org.eclipse.hono.service.management.credentials.CredentialsManagementService;
import org.eclipse.hono.service.management.device.DeviceManagementService;
import io.vertx.junit5.VertxExtension;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.extension.ExtendWith;

import java.io.IOException;

/**
 * Tests verifying behavior of {@link CacheCredentialsService}.
 *
 */
//fixme : this is disabled due to a bug in infinispan : https://issues.jboss.org/browse/ISPN-10073
@Disabled
@ExtendWith(VertxExtension.class)
public class CacheCredentialsServiceTest extends AbstractCredentialsServiceTest {

    private static CacheCredentialsService credentialService;
    private static CacheRegistrationService registrationService;
    private static EmbeddedHotRodServer server;

    /**
     * Spin up the service using Infinispan EmbeddedCache.
     */
    @BeforeEach
    public void setUp() throws IOException {

        server = new EmbeddedHotRodServer();
        credentialService = new CacheCredentialsService(server.getCache(), server.getCache());
        registrationService = new CacheRegistrationService(server.getCache());
    }

    /**
     * Stop the Embedded Infinispan Server.
     */
    @AfterEach
    public void cleanUp() {
        server.stop();
    }

    @Override
    public CredentialsService getCredentialsService() {
        return this.credentialService;
    }

    @Override
    public CredentialsManagementService getCredentialsManagementService() {
        return this.credentialService;
    }

    @Override
    public DeviceManagementService getDeviceManagementService() {
        return this.registrationService;
    }

}
