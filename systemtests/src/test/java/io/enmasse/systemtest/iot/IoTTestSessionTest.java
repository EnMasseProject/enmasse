/*
 * Copyright 2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package io.enmasse.systemtest.iot;

import static org.junit.Assert.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import io.enmasse.systemtest.TestTag;
import io.enmasse.systemtest.iot.IoTTestSession.Adapter;

@Tag(TestTag.FRAMEWORK)
public class IoTTestSessionTest {

    @Test
    public void testEnableAdapter() throws Exception {
        AtomicBoolean called = new AtomicBoolean();

        IoTTestSession
                .create()
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
