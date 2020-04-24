/*
 * Copyright 2019-2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package io.enmasse.iot.registry.infinispan.devcon;

import static java.net.HttpURLConnection.HTTP_NOT_FOUND;
import static java.net.HttpURLConnection.HTTP_OK;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.UUID;

import org.eclipse.hono.util.DeviceConnectionConstants;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import io.enmasse.iot.infinispan.EmbeddedHotRodServer;
import io.opentracing.noop.NoopSpan;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import io.vertx.rxjava.core.Promise;

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

        service.getLastKnownGatewayForDevice(DEFAULT_TENANT, "unknown-device", NoopSpan.INSTANCE)
                .setHandler(ctx.succeeding(r -> ctx.verify(() -> {
                    assertEquals(HTTP_NOT_FOUND, r.getStatus());
                    ctx.completeNow();
                })));
    }

    @Test
    public void testCreateAndGet(final VertxTestContext ctx) {

        final String deviceId = UUID.randomUUID().toString();

        Promise<Object> phase1 = Promise.promise();
        service.setLastKnownGatewayForDevice(DEFAULT_TENANT, deviceId, "gw1", NoopSpan.INSTANCE)
                .setHandler(ctx.succeeding(phase1::complete));

        Promise<Object> phase2 = Promise.promise();
        phase1.future().setHandler(x -> {
            service.getLastKnownGatewayForDevice(DEFAULT_TENANT, deviceId, NoopSpan.INSTANCE)
                    .setHandler(ctx.succeeding(r -> { ctx.verify(() -> {
                        assertEquals(HTTP_OK, r.getStatus());
                        String gw = r.getPayload().getString(DeviceConnectionConstants.FIELD_GATEWAY_ID);
                        assertEquals("gw1", gw);
                        phase2.complete();
                    });
            }));
        });

        Promise<Object> phase3 = Promise.promise();
        phase2.future().setHandler(x -> {
            service.setLastKnownGatewayForDevice(DEFAULT_TENANT, deviceId, "gw2", NoopSpan.INSTANCE)
                    .setHandler(ctx.succeeding(phase3::complete));
        });

        Promise<Object> phase4 = Promise.promise();
        phase3.future().setHandler(x -> {
            service.getLastKnownGatewayForDevice(DEFAULT_TENANT, deviceId, NoopSpan.INSTANCE)
                    .setHandler(ctx.succeeding(r -> { ctx.verify(() -> {
                        assertEquals(HTTP_OK, r.getStatus());
                        String gw = r.getPayload().getString(DeviceConnectionConstants.FIELD_GATEWAY_ID);
                        assertEquals("gw2", gw);
                        phase4.complete();
                    });
            }));
        });

        phase4.future().setHandler(ctx.succeeding(r -> ctx.completeNow()));

    }

}
