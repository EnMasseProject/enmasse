/*
 * Copyright 2019, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package io.enmasse.iot.registry.infinispan.devcon;

import org.eclipse.hono.util.DeviceConnectionConstants;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import io.enmasse.iot.registry.infinispan.EmbeddedHotRodServer;
import io.opentracing.noop.NoopSpan;
import io.vertx.core.Future;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;

import static java.net.HttpURLConnection.HTTP_NOT_FOUND;
import static java.net.HttpURLConnection.HTTP_OK;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.UUID;

/**
 * Tests verifying behavior of {@link CacheDeviceConnectionService}.
 *
 */
@ExtendWith(VertxExtension.class)
public class CacheDeviceConnectionServiceTest {

    private static final String DEFAULT_TENANT = "my.tenant";

    private CacheDeviceConnectionService service;
    private EmbeddedHotRodServer server;

    @BeforeEach
    public void setUp() throws Exception {
        this.server = new EmbeddedHotRodServer();
        this.service = new CacheDeviceConnectionService(server.getDeviceStateCache());
    }

    @AfterEach
    public void cleanUp() throws Exception {
        if (this.server != null) {
            this.server.stop();
        }
    }

    @Test
    public void testGetNotFound(final VertxTestContext ctx) {

        service.getLastKnownGatewayForDevice(DEFAULT_TENANT, "unknown-device", NoopSpan.INSTANCE,
                ctx.succeeding(r -> ctx.verify(() -> {
                    assertEquals(HTTP_NOT_FOUND, r.getStatus());
                    ctx.completeNow();
                })));

    }

    @Test
    public void testCreateAndGet(final VertxTestContext ctx) {

        final String deviceId = UUID.randomUUID().toString();

        Future<Object> phase1 = Future.future();
        service.setLastKnownGatewayForDevice(DEFAULT_TENANT, deviceId, "gw1", NoopSpan.INSTANCE, ctx.succeeding(phase1::complete));

        Future<Object> phase2 = Future.future();
        phase1.setHandler(x -> {
            service.getLastKnownGatewayForDevice(DEFAULT_TENANT, deviceId, NoopSpan.INSTANCE, ctx.succeeding(r -> {
                ctx.verify(() -> {
                    assertEquals(HTTP_OK, r.getStatus());
                    String gw = r.getPayload().getString(DeviceConnectionConstants.FIELD_GATEWAY_ID);
                    assertEquals("gw1", gw);
                    phase2.complete();
                });
            }));
        });

        Future<Object> phase3 = Future.future();
        phase2.setHandler(x -> {
            service.setLastKnownGatewayForDevice(DEFAULT_TENANT, deviceId, "gw2", NoopSpan.INSTANCE, ctx.succeeding(phase3::complete));
        });

        Future<Object> phase4 = Future.future();
        phase3.setHandler(x -> {
            service.getLastKnownGatewayForDevice(DEFAULT_TENANT, deviceId, NoopSpan.INSTANCE, ctx.succeeding(r -> {
                ctx.verify(() -> {
                    assertEquals(HTTP_OK, r.getStatus());
                    String gw = r.getPayload().getString(DeviceConnectionConstants.FIELD_GATEWAY_ID);
                    assertEquals("gw2", gw);
                    phase4.complete();
                });
            }));
        });

        phase4.setHandler(ctx.succeeding(r -> ctx.completeNow()));

    }

}
