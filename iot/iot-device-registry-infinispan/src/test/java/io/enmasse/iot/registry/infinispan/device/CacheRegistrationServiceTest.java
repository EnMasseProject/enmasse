/*
 * Copyright 2019, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

//TODO write unit tests for DevicesCredentialsCacheService
//package io.enmasse.iot.registry.infinispan.device;
//
//import org.eclipse.hono.service.registration.AbstractCompleteRegistrationServiceTest;
//import org.eclipse.hono.service.registration.CompleteRegistrationService;
//import org.junit.jupiter.api.AfterEach;
//import org.junit.jupiter.api.BeforeEach;
//import org.junit.jupiter.api.extension.ExtendWith;
//
//import io.enmasse.iot.registry.infinispan.EmbeddedHotRodServer;
//import io.enmasse.iot.registry.infinispan.device.CacheRegistrationService;
//import io.vertx.junit5.VertxExtension;
//
//import java.io.IOException;
//
///**
// * Tests verifying behavior of {@link CacheRegistrationService}.
// *
// */
//@ExtendWith(VertxExtension.class)
//public class CacheRegistrationServiceTest extends AbstractCompleteRegistrationServiceTest {
//
//    private CacheRegistrationService service;
//    private EmbeddedHotRodServer server;
//
//    @BeforeEach
//    public void setUp() throws IOException {
//        server = new EmbeddedHotRodServer();
//        service = new CacheRegistrationService(server.getCache("devices"));
//    }
//
//    @AfterEach
//    public void cleanUp() throws Exception {
//        server.stop();
//    }
//
//    @Override
//    public CompleteRegistrationService getCompleteRegistrationService() {
//        return service;
//    }
//
//
//}
