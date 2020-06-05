/*
 * Copyright 2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package io.enmasse.systemtest.iot.amqp;

import static io.enmasse.systemtest.TestTag.ACCEPTANCE;
import static io.vertx.proton.ProtonQoS.AT_LEAST_ONCE;
import static io.vertx.proton.ProtonQoS.AT_MOST_ONCE;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.Duration;
import java.util.concurrent.TimeoutException;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import io.enmasse.systemtest.iot.AmqpAdapterClient;
import io.enmasse.systemtest.iot.IoTTestSession.Device;
import io.enmasse.systemtest.iot.MessageSendTester;
import io.enmasse.systemtest.iot.MessageSendTester.ConsumerFactory;
import io.enmasse.systemtest.iot.StandardIoTTests;

public interface StandardIoTAmqpTests extends StandardIoTTests {

    /**
     * Single telemetry message with attached consumer (at most once).
     */
    @Tag(ACCEPTANCE)
    @ParameterizedTest(name = "testAmqpTelemetrySingleAtMostOnce-{0}")
    @MethodSource("getDevices")
    default void testAmqpTelemetrySingleAtMostOnce(final Device device) throws Exception {

        try (AmqpAdapterClient client = device.createAmqpAdapterClient(AT_MOST_ONCE)) {
            new MessageSendTester()
                    .type(MessageSendTester.Type.TELEMETRY)
                    .delay(Duration.ofSeconds(1))
                    .consumerFactory(ConsumerFactory.of(getSession().getConsumerClient(), getSession().getTenantId()))
                    .sender(client)
                    .amount(1)
                    .consume(MessageSendTester.Consume.BEFORE)
                    .execute();
        }

    }

    /**
     * Single telemetry message with attached consumer (at least once).
     */
    @Tag(ACCEPTANCE)
    @ParameterizedTest(name = "testAmqpTelemetrySingleAtLeastOnce-{0}")
    @MethodSource("getDevices")
    default void testAmqpTelemetrySingleAtLeastOnce(final Device device) throws Exception {

        try (AmqpAdapterClient client = device.createAmqpAdapterClient(AT_LEAST_ONCE)) {
            new MessageSendTester()
                    .type(MessageSendTester.Type.TELEMETRY)
                    .delay(Duration.ofSeconds(1))
                    .consumerFactory(ConsumerFactory.of(getSession().getConsumerClient(), getSession().getTenantId()))
                    .sender(client)
                    .amount(1)
                    .consume(MessageSendTester.Consume.BEFORE)
                    .execute();
        }

    }

    /**
     * Test a single event message.
     * <br>
     * Send a single message, no consumer attached. The message gets delivered
     * when the consumer attaches.
     */
    @ParameterizedTest(name = "testAmqpEventSingle-{0}")
    @MethodSource("getDevices")
    default void testAmqpEventSingle(final Device device) throws Exception {

        try (AmqpAdapterClient client = device.createAmqpAdapterClient(AT_LEAST_ONCE)) {
            new MessageSendTester()
                    .type(MessageSendTester.Type.EVENT)
                    .delay(Duration.ofSeconds(1))
                    .consumerFactory(ConsumerFactory.of(getSession().getConsumerClient(), getSession().getTenantId()))
                    .sender(client)
                    .amount(1)
                    .consume(MessageSendTester.Consume.AFTER)
                    .execute();
        }

    }

    /**
     * Test a batch of telemetry messages, consumer is started before sending (at most once).
     * <br>
     * This is the normal telemetry case.
     */
    @ParameterizedTest(name = "testAmqpTelemetryBatch50AtMostOnce-{0}")
    @MethodSource("getDevices")
    default void testAmqpTelemetryBatch50AtMostOnce(final Device device) throws Exception {

        try (AmqpAdapterClient client = device.createAmqpAdapterClient(AT_MOST_ONCE)) {
            new MessageSendTester()
                    .type(MessageSendTester.Type.TELEMETRY)
                    .delay(Duration.ofSeconds(1))
                    .consumerFactory(ConsumerFactory.of(getSession().getConsumerClient(), getSession().getTenantId()))
                    .sender(client)
                    .amount(50)
                    .consume(MessageSendTester.Consume.BEFORE)
                    .execute();
        }

    }

    /**
     * Test a batch of telemetry messages, consumer is started before sending (at least once).
     * <br>
     * This is the normal telemetry case.
     */
    @ParameterizedTest(name = "testAmqpTelemetryBatch50AtLeastOnce-{0}")
    @MethodSource("getDevices")
    default void testAmqpTelemetryBatch50AtLeastOnce(final Device device) throws Exception {

        try (AmqpAdapterClient client = device.createAmqpAdapterClient(AT_LEAST_ONCE)) {
            new MessageSendTester()
                    .type(MessageSendTester.Type.TELEMETRY)
                    .delay(Duration.ofSeconds(1))
                    .consumerFactory(ConsumerFactory.of(getSession().getConsumerClient(), getSession().getTenantId()))
                    .sender(client)
                    .amount(50)
                    .consume(MessageSendTester.Consume.BEFORE)
                    .execute();
        }

    }

    /**
     * Test a batch of events, having no consumer attached.
     * <br>
     * As events get buffered by the broker, there is no requirement to start
     * a consumer before sending the messages. However when the consumer is
     * attached, it should receive those messages.
     */
    @ParameterizedTest(name = "testAmqpEventBatch5After-{0}")
    @MethodSource("getDevices")
    default void testAmqpEventBatch5After(final Device device) throws Exception {

        try (AmqpAdapterClient client = device.createAmqpAdapterClient(AT_MOST_ONCE)) {
            new MessageSendTester()
                    .type(MessageSendTester.Type.EVENT)
                    .delay(Duration.ofMillis(100))
                    .additionalSendTimeout(Duration.ofSeconds(10))
                    .consumerFactory(ConsumerFactory.of(getSession().getConsumerClient(), getSession().getTenantId()))
                    .sender(client)
                    .amount(5)
                    .consume(MessageSendTester.Consume.AFTER)
                    .execute();
        }

    }

    /**
     * Test a batch of events, starting the consumer before sending.
     * <br>
     * This is the default use case with events, and should simply work
     * as with telemetry.
     */
    @ParameterizedTest(name = "testAmqpEventBatch5Before-{0}")
    @MethodSource("getDevices")
    default void testAmqpEventBatch5Before(final Device device) throws Exception {

        try (AmqpAdapterClient client = device.createAmqpAdapterClient(AT_LEAST_ONCE)) {
            new MessageSendTester()
                    .type(MessageSendTester.Type.EVENT)
                    .delay(Duration.ZERO)
                    .additionalSendTimeout(Duration.ofSeconds(10))
                    .consumerFactory(ConsumerFactory.of(getSession().getConsumerClient(), getSession().getTenantId()))
                    .sender(client)
                    .amount(5)
                    .consume(MessageSendTester.Consume.BEFORE)
                    .execute();
        }

    }

    /**
     * Test for an invalid device.
     * <br>
     * With an invalid device, no messages must pass.
     */
    @ParameterizedTest(name = "testAmqpDeviceFails-{0}")
    @MethodSource("getInvalidDevices")
    default void testAmqpDeviceFails(final Device device) throws Exception {

        /*
         * We test an invalid device by trying to send either telemetry or event messages.
         * Two separate connections, and more than one message.
         */

        try (AmqpAdapterClient client = device.createAmqpAdapterClient(AT_LEAST_ONCE)) {
            assertThrows(TimeoutException.class, () -> {
                new MessageSendTester()
                        .type(MessageSendTester.Type.TELEMETRY)
                        .delay(Duration.ofSeconds(1))
                        .consumerFactory(ConsumerFactory.of(getSession().getConsumerClient(), getSession().getTenantId()))
                        .sender(client)
                        .amount(5)
                        .consume(MessageSendTester.Consume.BEFORE)
                        .execute();
            });
        }

        try (AmqpAdapterClient client = device.createAmqpAdapterClient(AT_LEAST_ONCE)) {
            assertThrows(TimeoutException.class, () -> {
                new MessageSendTester()
                        .type(MessageSendTester.Type.EVENT)
                        .delay(Duration.ofSeconds(1))
                        .consumerFactory(ConsumerFactory.of(getSession().getConsumerClient(), getSession().getTenantId()))
                        .sender(client)
                        .amount(5)
                        .consume(MessageSendTester.Consume.BEFORE)
                        .execute();
            });
        }

    }
}
