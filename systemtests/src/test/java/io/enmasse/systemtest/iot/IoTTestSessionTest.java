/*
 * Copyright 2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package io.enmasse.systemtest.iot;

import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import io.enmasse.iot.model.v1.IoTConfig;
import io.enmasse.systemtest.iot.IoTTestSession.Adapter;

import static io.enmasse.systemtest.framework.TestTag.FRAMEWORK;
import static io.enmasse.systemtest.iot.IoTTestSession.Adapter.HTTP;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Tag(FRAMEWORK)
public class IoTTestSessionTest {

    @Test
    public void testNameInDefaultConfig() {
        var config = IoTTestSession.createDefaultConfig("default-ns", true).build();

        assertDefaultConfig(config);
    }

    @Test
    public void testNameInDefaultConfigAfterChange() {
        var configBuilder = IoTTestSession.createDefaultConfig("default-ns", true);

        assertDefaultConfig(configBuilder.build());

        for (Adapter adapter : Adapter.values()) {
            configBuilder = adapter.disable(configBuilder);
        }
        configBuilder = HTTP.enable(configBuilder);

        assertDefaultConfig(configBuilder.build());

    }

    private void assertDefaultConfig(IoTConfig config) {
        assertNotNull(config);

        assertNotNull(config.getMetadata());
        assertEquals("default", config.getMetadata().getName());
        assertEquals("default-ns", config.getMetadata().getNamespace());
    }

    @Test
    public void testEnableAdapter() throws Exception {
        AtomicBoolean called = new AtomicBoolean();

        IoTTestSession
                .create("default-ns", true)
                .adapters(Adapter.HTTP)
                .config(configBuilder -> {

                    var config = configBuilder.build();

                    assertNotNull(config.getSpec());
                    assertNotNull(config.getSpec().getAdapters());

                    assertNotNull(config.getSpec().getAdapters().getHttp());
                    assertEquals(Boolean.TRUE, config.getSpec().getAdapters().getHttp().getEnabled());

                    assertNotNull(config.getSpec().getAdapters().getMqtt());
                    assertEquals(Boolean.FALSE, config.getSpec().getAdapters().getMqtt().getEnabled());

                    called.set(true);

                });

        assertTrue(called.get());
    }

}
