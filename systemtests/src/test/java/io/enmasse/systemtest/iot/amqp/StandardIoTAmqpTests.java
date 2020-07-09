/*
 * Copyright 2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package io.enmasse.systemtest.iot.amqp;

import static io.enmasse.systemtest.framework.TestTag.ACCEPTANCE;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.fail;

import java.time.Duration;
import java.util.concurrent.TimeoutException;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Throwables;

import io.enmasse.systemtest.iot.AmqpAdapterClient;
import io.enmasse.systemtest.iot.DeviceSupplier;
import io.enmasse.systemtest.iot.MessageSendTester;
import io.enmasse.systemtest.iot.MessageSendTester.ConsumerFactory;
import io.enmasse.systemtest.iot.StandardIoTTests;

public interface StandardIoTAmqpTests extends StandardIoTTests {

    final static Logger log = LoggerFactory.getLogger(StandardIoTAmqpTests.class);

    /**
     * Single telemetry message with attached consumer.
     */
    @Tag(ACCEPTANCE)
    @ParameterizedTest(name = "testAmqpTelemetrySingle-{0}")
    @MethodSource("getDevices")
    default void testAmqpTelemetrySingle(final DeviceSupplier device) throws Exception {

        try (AmqpAdapterClient client = device.get().createAmqpAdapterClient()) {
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
     * <p>
     * Send a single message, no consumer attached. The message gets delivered
     * when the consumer attaches.
     */
    @ParameterizedTest(name = "testAmqpEventSingle-{0}")
    @MethodSource("getDevices")
    default void testAmqpEventSingle(final DeviceSupplier device) throws Exception {

        try (AmqpAdapterClient client = device.get().createAmqpAdapterClient()) {
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
     * Test a batch of telemetry messages, consumer is started before sending.
     * <p>
     * This is the normal telemetry case.
     */
    @ParameterizedTest(name = "testAmqpTelemetryBatch50-{0}")
    @MethodSource("getDevices")
    default void testAmqpTelemetryBatch50(final DeviceSupplier device) throws Exception {

        try (AmqpAdapterClient client = device.get().createAmqpAdapterClient()) {
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
     * <p>
     * As events get buffered by the broker, there is no requirement to start
     * a consumer before sending the messages. However when the consumer is
     * attached, it should receive those messages.
     */
    @ParameterizedTest(name = "testAmqpEventBatch5After-{0}")
    @MethodSource("getDevices")
    default void testAmqpEventBatch5After(final DeviceSupplier device) throws Exception {

        try (AmqpAdapterClient client = device.get().createAmqpAdapterClient()) {
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
     * <p>
     * This is the default use case with events, and should simply work
     * as with telemetry.
     */
    @ParameterizedTest(name = "testAmqpEventBatch5Before-{0}")
    @MethodSource("getDevices")
    default void testAmqpEventBatch5Before(final DeviceSupplier device) throws Exception {

        try (AmqpAdapterClient client = device.get().createAmqpAdapterClient()) {
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
     * <p>
     * With an invalid device, no messages must pass.
     */
    @ParameterizedTest(name = "testAmqpDeviceFails-{0}")
    @MethodSource("getInvalidDevices")
    default void testAmqpDeviceFails(final DeviceSupplier deviceSupplier) throws Exception {

        var device = deviceSupplier.get();

        /*
         * We test an invalid device by trying to send either telemetry or event messages.
         * Two separate connections, and more than one message.
         */

        try (AmqpAdapterClient client = device.createAmqpAdapterClient()) {
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
        } catch (Exception e) {
            assertConnectionException(e);
            log.debug("Accepting AMQP exception", e);
        }

        try (AmqpAdapterClient client = device.createAmqpAdapterClient()) {
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
        } catch (Exception e) {
            assertConnectionException(e);
            log.debug("Accepting AMQP exception", e);
        }

    }

    static void assertConnectionException(final Throwable e) {

        // if we get an exception, it must be an AuthenticationException or SSLHandshakeException

        final Throwable cause = Throwables.getRootCause(e);
        if (cause instanceof javax.security.sasl.AuthenticationException) {
            return;
        } else if (cause instanceof javax.net.ssl.SSLHandshakeException) {
            return;
        }

        fail("Failed to connect with non-permitted exception", e);

    }
}
