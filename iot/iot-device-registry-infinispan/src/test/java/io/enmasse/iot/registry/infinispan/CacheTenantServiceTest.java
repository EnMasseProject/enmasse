/*
 * Copyright 2019, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

/*******************************************************************************
 * Copyright (c) 2019 Contributors to the Eclipse Foundation
 *
 * See the NOTICE file(s) distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.enmasse.iot.registry.infinispan;

import io.vertx.ext.unit.junit.VertxUnitRunner;
import org.eclipse.hono.service.tenant.AbstractCompleteTenantServiceTest;
import org.eclipse.hono.service.tenant.CompleteTenantService;

import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.rules.Timeout;
import org.junit.runner.RunWith;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

/**
 * Tests verifying behavior of {@link CacheTenantService}.
 *
 */
@Ignore
@RunWith(VertxUnitRunner.class)
public class CacheTenantServiceTest extends AbstractCompleteTenantServiceTest {

    private static CacheTenantService service;
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
        service = new CacheTenantService(server.getCache());
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
    public CompleteTenantService getCompleteTenantService() {
        return service;
    }
}