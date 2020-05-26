/*
 * Copyright 2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package io.enmasse.systemtest.iot.mqtt;

import static io.enmasse.systemtest.TestTag.ACCEPTANCE;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.Duration;
import java.util.concurrent.TimeoutException;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import io.enmasse.systemtest.iot.IoTTestSession.Device;
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

    /**
     * Single telemetry message with attached consumer. QoS 1.
     * <br>
     * Sending with QoS 1 is ok.
     */
    @Tag(ACCEPTANCE)
    @ParameterizedTest(name = "testMqttTelemetrySingleQos1-{0}")
    @MethodSource("getDevices")
    default void testMqttTelemetrySingleQos1(final Device device) throws Exception {

        try (MqttAdapterClient client = device.createMqttAdapterClient()) {
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
    default void testMqttEventSingle(final Device device) throws Exception {

        try (MqttAdapterClient client = device.createMqttAdapterClient()) {
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
    default void testMqttTelemetryBatch50Qos0(final Device device) throws Exception {

        try (MqttAdapterClient client = device.createMqttAdapterClient()) {
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
    default void testMqttTelemetryBatch50Qos1(final Device device) throws Exception {

        try (MqttAdapterClient client = device.createMqttAdapterClient()) {
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
    default void testMqttEventBatch5After(final Device device) throws Exception {

        try (MqttAdapterClient client = device.createMqttAdapterClient()) {
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
    default void testMqttEventBatch5Before(final Device device) throws Exception {

        try (MqttAdapterClient client = device.createMqttAdapterClient()) {
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
    default void testMqttInvalidDevice(final Device device) throws Exception {

        /*
         * We test an invalid device by trying to send either telemetry or event messages.
         * Two separate connections, and more than one message.
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
        }

    }

}
