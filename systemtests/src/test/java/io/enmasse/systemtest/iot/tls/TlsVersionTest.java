/*
 * Copyright 2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package io.enmasse.systemtest.iot.tls;

import io.enmasse.systemtest.iot.HttpAdapterClient;
import io.enmasse.systemtest.iot.IoTTestSession;
import io.enmasse.systemtest.iot.IoTTestSession.Adapter;
import io.enmasse.systemtest.iot.IoTTests;
import io.enmasse.systemtest.iot.MessageSendTester;
import io.enmasse.systemtest.iot.MessageSendTester.ConsumerFactory;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.concurrent.TimeoutException;

import static io.enmasse.systemtest.framework.TestTag.IOT;
import static java.util.Collections.singleton;
import static org.junit.jupiter.api.Assertions.assertThrows;

@Tag(IOT)
public class TlsVersionTest implements IoTTests {

    @Test
    public void testWithExplicitVersion() throws Exception {
        IoTTestSession
                .createDefault()
                .adapters(Adapter.HTTP)
                .config(config -> config
                        .editOrNewSpec()
                        .editOrNewTls()
                        .withVersions("TLSv1.3")
                        .endTls()
                        .endSpec())
                // we accept 1.2 and 1.3 for all connections we don't test explicitly
                .defaultTlsVersions("TLSv1.2", "TLSv1.3")
                .run(session -> {

                    var device = session.registerNewRandomDeviceWithPassword();

                    try (HttpAdapterClient client = device.createHttpAdapterClient(singleton("TLSv1.3"))) {

                        new MessageSendTester()
                                .type(MessageSendTester.Type.TELEMETRY)
                                .delay(Duration.ofSeconds(1))
                                .consumerFactory(ConsumerFactory.of(session.getConsumerClient(), session.getTenantId()))
                                .sender(client::send)
                                .amount(50)
                                .consume(MessageSendTester.Consume.BEFORE)
                                .execute();

                    }

                    try (HttpAdapterClient client = device.createHttpAdapterClient(singleton("TLSv1.2"))) {

                        // this must fail, as we offer TLSv1.2, but have only 1.3 configured
                        assertThrows(TimeoutException.class, () -> {
                            new MessageSendTester()
                                    .type(MessageSendTester.Type.TELEMETRY)
                                    .delay(Duration.ofSeconds(1))
                                    .consumerFactory(ConsumerFactory.of(session.getConsumerClient(), session.getTenantId()))
                                    .sender(client::send)
                                    .amount(50)
                                    .consume(MessageSendTester.Consume.BEFORE)
                                    .execute();
                        });

                    }

                });
    }

}
