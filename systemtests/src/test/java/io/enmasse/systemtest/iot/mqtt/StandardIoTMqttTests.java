/*
 * Copyright 2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package io.enmasse.systemtest.iot.mqtt;

import static io.enmasse.systemtest.TestTag.ACCEPTANCE;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.fail;

import java.time.Duration;
import java.util.concurrent.TimeoutException;

import javax.net.ssl.SSLHandshakeException;

import org.eclipse.paho.client.mqttv3.MqttSecurityException;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Throwables;

import io.enmasse.systemtest.iot.DeviceSupplier;
import io.enmasse.systemtest.iot.MessageSendTester;
import io.enmasse.systemtest.iot.MessageSendTester.ConsumerFactory;
import io.enmasse.systemtest.iot.MqttAdapterClient;
import io.enmasse.systemtest.iot.StandardIoTTests;

/**
 * Standard MQTT IoT tests
 * <p>
 * <strong>Note:</strong> we do not test single telemetry with QoS 0 here, as we don't receive any
 * feedback if the message was accepted or not. So we couldn't re-try and could only assume that a
 * message loss of 100% would be acceptable, which doesn't test much. For bigger batch sizes we can
 * test with an acceptable message loss rate of e.g. 10%.
 */
public interface StandardIoTMqttTests extends StandardIoTTests {

    final static Logger log = LoggerFactory.getLogger(StandardIoTMqttTests.class);

    /**
     * Single telemetry message with attached consumer. QoS 1.
     * <br>
     * Sending with QoS 1 is ok.
     */
    @Tag(ACCEPTANCE)
    @ParameterizedTest(name = "testMqttTelemetrySingleQos1-{0}")
    @MethodSource("getDevices")
    default void testMqttTelemetrySingleQos1(final DeviceSupplier device) throws Exception {

        try (MqttAdapterClient client = device.get().createMqttAdapterClient()) {
            new MessageSendTester()
                    .type(MessageSendTester.Type.TELEMETRY)
                    .delay(Duration.ofSeconds(1))
                    .consumerFactory(ConsumerFactory.of(getSession().getConsumerClient(), getSession().getTenantId()))
                    .sender(client::sendQos1)
                    .amount(1)
                    .consume(MessageSendTester.Consume.BEFORE)
                    .execute();
        }

    }

    /**
     * Single event message with non-attached consumer.
     * <br>
     * This is the normal use case.
     */
    @ParameterizedTest(name = "testMqttEventSingle-{0}")
    @MethodSource("getDevices")
    default void testMqttEventSingle(final DeviceSupplier device) throws Exception {

        try (MqttAdapterClient client = device.get().createMqttAdapterClient()) {
            new MessageSendTester()
                    .type(MessageSendTester.Type.EVENT)
                    .delay(Duration.ofSeconds(1))
                    .consumerFactory(ConsumerFactory.of(getSession().getConsumerClient(), getSession().getTenantId()))
                    .sender(client::sendQos1)
                    .amount(1)
                    .consume(MessageSendTester.Consume.AFTER)
                    .execute();
        }

    }

    /**
     * Batch of telemetry messages with attached consumer. QoS 0.
     * <br>
     * Batched version of the normal use case. We do accept message loss of 10% here.
     */
    @ParameterizedTest(name = "testMqttTelemetryBatch50Qos0-{0}")
    @MethodSource("getDevices")
    default void testMqttTelemetryBatch50Qos0(final DeviceSupplier device) throws Exception {

        try (MqttAdapterClient client = device.get().createMqttAdapterClient()) {
            new MessageSendTester()
                    .type(MessageSendTester.Type.TELEMETRY)
                    .delay(Duration.ofSeconds(1))
                    .consumerFactory(ConsumerFactory.of(getSession().getConsumerClient(), getSession().getTenantId()))
                    .sender(client::sendQos0)
                    .amount(50)
                    .consume(MessageSendTester.Consume.BEFORE)
                    .acceptableMessageLoss(5) // allow for 10%
                    .execute();
        }

    }

    /**
     * Batch of telemetry messages with attached consumer. QoS 1.
     * <br>
     * Compared to QoS 0, we do not accept message loss here.
     */
    @ParameterizedTest(name = "testMqttTelemetryBatch50Qos1-{0}")
    @MethodSource("getDevices")
    default void testMqttTelemetryBatch50Qos1(final DeviceSupplier device) throws Exception {

        try (MqttAdapterClient client = device.get().createMqttAdapterClient()) {
            new MessageSendTester()
                    .type(MessageSendTester.Type.TELEMETRY)
                    .delay(Duration.ofSeconds(1))
                    .consumerFactory(ConsumerFactory.of(getSession().getConsumerClient(), getSession().getTenantId()))
                    .sender(client::sendQos1)
                    .amount(50)
                    .consume(MessageSendTester.Consume.BEFORE)
                    .execute();
        }

    }

    /**
     * Batch of event messages with non-attached consumer.
     * <br>
     * This sends messages without an attached consumer. The broker is expected
     * to take care of that. Still we expect to receive the messages later.
     */
    @ParameterizedTest(name = "testMqttEventBatch5After-{0}")
    @MethodSource("getDevices")
    default void testMqttEventBatch5After(final DeviceSupplier device) throws Exception {

        try (MqttAdapterClient client = device.get().createMqttAdapterClient()) {
            new MessageSendTester()
                    .type(MessageSendTester.Type.EVENT)
                    .delay(Duration.ofMillis(100))
                    .additionalSendTimeout(Duration.ofSeconds(2))
                    .consumerFactory(ConsumerFactory.of(getSession().getConsumerClient(), getSession().getTenantId()))
                    .sender(client::sendQos1)
                    .amount(5)
                    .consume(MessageSendTester.Consume.AFTER)
                    .execute();
        }

    }

    /**
     * Batch of event messages with attached consumer.
     * <br>
     * This is the normal use case.
     */
    @ParameterizedTest(name = "testMqttEventBatch5Before-{0}")
    @MethodSource("getDevices")
    default void testMqttEventBatch5Before(final DeviceSupplier device) throws Exception {

        try (MqttAdapterClient client = device.get().createMqttAdapterClient()) {
            new MessageSendTester()
                    .type(MessageSendTester.Type.EVENT)
                    .delay(Duration.ofSeconds(1))
                    .consumerFactory(ConsumerFactory.of(getSession().getConsumerClient(), getSession().getTenantId()))
                    .sender(client::sendQos1)
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
    @ParameterizedTest(name = "testMqttInvalidDevice-{0}")
    @MethodSource("getInvalidDevices")
    default void testMqttInvalidDevice(final DeviceSupplier deviceSupplier) throws Exception {

        log.info("Testing invalid devices, the following exception may be expected");

        // get the device now, once, throwing out of the test method

        var device = deviceSupplier.get();

        /*
         * We test an invalid device by trying to send either telemetry or event messages.
         * Two separate connections, and more than one message.
         *
         * We do expect a failure, but it must be a specific failure. We do accept
         * an MqttSecurityException when opening the connection, or a TimeoutException
         * when we could open the connection, but not send/receive.
         */

        try (MqttAdapterClient client = device.createMqttAdapterClient()) {
            assertThrows(TimeoutException.class, () -> {
                new MessageSendTester()
                        .type(MessageSendTester.Type.TELEMETRY)
                        .delay(Duration.ofSeconds(1))
                        .consumerFactory(ConsumerFactory.of(getSession().getConsumerClient(), getSession().getTenantId()))
                        .sender(client::sendQos1)
                        .amount(5)
                        .consume(MessageSendTester.Consume.BEFORE)
                        .execute();
            });
        } catch (Exception e) {
            assertConnectionException(e);
            log.info("Accepting MQTT exception", e);
        }

        try (MqttAdapterClient client = device.createMqttAdapterClient()) {
            assertThrows(TimeoutException.class, () -> {
                new MessageSendTester()
                        .type(MessageSendTester.Type.EVENT)
                        .delay(Duration.ofSeconds(1))
                        .consumerFactory(ConsumerFactory.of(getSession().getConsumerClient(), getSession().getTenantId()))
                        .sender(client::sendQos1)
                        .amount(5)
                        .consume(MessageSendTester.Consume.BEFORE)
                        .execute();
            });
        } catch (Exception e) {
            assertConnectionException(e);
            log.info("Accepting MQTT exception", e);
        }

    }

    public static void assertConnectionException(final Throwable e) {

        // if we get an exception, it must be an MqttSecurityException or SSLHandshakeException

        if (e instanceof MqttSecurityException) {
            return;
        }

        final Throwable cause = Throwables.getRootCause(e);
        if (cause instanceof SSLHandshakeException) {
            return;
        }

        fail("Failed to connect with non-permitted exception", e);

    }

}
