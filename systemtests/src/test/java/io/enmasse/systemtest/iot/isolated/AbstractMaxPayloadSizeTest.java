/*
 * Copyright 2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package io.enmasse.systemtest.iot.isolated;

import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.Duration;
import java.util.concurrent.TimeoutException;

import io.enmasse.systemtest.iot.IoTTests;
import org.junit.jupiter.api.Test;

import io.enmasse.systemtest.iot.IoTTestSession;
import io.enmasse.systemtest.iot.IoTTestSession.Adapter;
import io.enmasse.systemtest.iot.IoTTestSession.Device;
import io.enmasse.systemtest.iot.MessageSendTester;
import io.enmasse.systemtest.iot.MessageSendTester.ConsumerFactory;
import io.enmasse.systemtest.iot.MessageSendTester.Sender;
import io.enmasse.systemtest.iot.MessageSendTester.Type;

public abstract class AbstractMaxPayloadSizeTest implements IoTTests {

    protected abstract Adapter adapter();

    protected abstract Sender sender(Device device) throws Exception;

    /**
     * Reduce the payload by this amount of bytes.
     * <p>
     * This is required by e.g. the MQTT protocol, as the topic name counts against the total length of
     * the MQTT payload.
     *
     * @param type the type to send.
     *
     * @return The number of bytes to remove from the payload.
     */
    protected int reducePayloadBy(final Type type) {
        return 0;
    }

    @Test
    public void testDecreasedMaxPayloadDefault() throws Exception {

        IoTTestSession
                .createDefault()
                .adapters(adapter())
                .config(config -> config
                        .editOrNewSpec()
                        .editOrNewAdapters()
                        .editOrNewDefaults()
                        .withMaxPayloadSize(256)
                        .endDefaults()
                        .endAdapters()
                        .endSpec())
                .run(session -> {

                    var device = session
                            .newDevice("4711")
                            .register()
                            .setPassword("auth-1", "123456");

                    // we expect the sending to fail, due to timeouts, as the payload exceeds the maximum

                    assertThrows(TimeoutException.class, () -> {
                        new MessageSendTester()
                                .type(MessageSendTester.Type.TELEMETRY)
                                .payloadSize(1024 - reducePayloadBy(MessageSendTester.Type.TELEMETRY))
                                .delay(Duration.ofSeconds(1))
                                .consumerFactory(ConsumerFactory.of(session.getConsumerClient(), session.getTenantId()))
                                .sender(sender(device))
                                .amount(50)
                                .consume(MessageSendTester.Consume.BEFORE)
                                .execute();
                    });

                });

    }

    @Test
    public void testIncreasedMaxPayloadDefault() throws Exception {

        IoTTestSession
                .createDefault()
                .adapters(adapter())
                .config(config -> config
                        .editOrNewSpec()
                        .editOrNewAdapters()
                        .editOrNewDefaults()
                        .withMaxPayloadSize(16 * 1024)
                        .endDefaults()
                        .endAdapters()
                        .endSpec())
                .run(session -> {

                    var device = session
                            .newDevice("4711")
                            .register()
                            .setPassword("auth-1", "123456");

                    new MessageSendTester()
                            .type(MessageSendTester.Type.TELEMETRY)
                            .payloadSize((16 * 1024) - reducePayloadBy(MessageSendTester.Type.TELEMETRY))
                            .delay(Duration.ofSeconds(1))
                            .consumerFactory(ConsumerFactory.of(session.getConsumerClient(), session.getTenantId()))
                            .sender(sender(device))
                            .amount(50)
                            .consume(MessageSendTester.Consume.BEFORE)
                            .execute();
                });

    }

    @Test
    public void testStandardMaxPayloadDefault() throws Exception {

        IoTTestSession
                .createDefault()
                .adapters(adapter())
                .run(session -> {

                    var device = session
                            .newDevice("4711")
                            .register()
                            .setPassword("auth-1", "123456");

                    new MessageSendTester()
                            .type(MessageSendTester.Type.TELEMETRY)
                            .payloadSize(1024 - reducePayloadBy(MessageSendTester.Type.TELEMETRY))
                            .delay(Duration.ofSeconds(1))
                            .consumerFactory(ConsumerFactory.of(session.getConsumerClient(), session.getTenantId()))
                            .sender(sender(device))
                            .amount(50)
                            .consume(MessageSendTester.Consume.BEFORE)
                            .execute();
                });

    }

}
