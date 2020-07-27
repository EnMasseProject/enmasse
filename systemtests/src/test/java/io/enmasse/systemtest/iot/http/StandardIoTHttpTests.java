/*
 * Copyright 2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package io.enmasse.systemtest.iot.http;

import io.enmasse.systemtest.iot.CommandTester;
import io.enmasse.systemtest.iot.CommandTester.CommanderFactory;
import io.enmasse.systemtest.iot.DeviceSupplier;
import io.enmasse.systemtest.iot.HttpAdapterClient;
import io.enmasse.systemtest.iot.MessageSendTester;
import io.enmasse.systemtest.iot.MessageSendTester.ConsumerFactory;
import io.enmasse.systemtest.iot.StandardIoTTests;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.SSLHandshakeException;
import java.time.Duration;
import java.util.concurrent.TimeoutException;

import static io.enmasse.systemtest.framework.TestTag.ACCEPTANCE;
import static io.enmasse.systemtest.iot.CommandTester.Mode.ONE_WAY;
import static io.enmasse.systemtest.iot.CommandTester.Mode.REQUEST_RESPONSE;
import static io.enmasse.systemtest.iot.HttpAdapterClient.ResponseException.statusCode;
import static io.enmasse.systemtest.iot.HttpAdapterClient.causedBy;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public interface StandardIoTHttpTests extends StandardIoTTests {

    Logger log = LoggerFactory.getLogger(StandardIoTHttpTests.class);

    /**
     * Single telemetry message with attached consumer.
     */
    @Tag(ACCEPTANCE)
    @ParameterizedTest(name = "testHttpTelemetrySingle-{0}")
    @MethodSource("getDevices")
    default void testHttpTelemetrySingle(final DeviceSupplier device) throws Exception {

        try (HttpAdapterClient client = device.get().createHttpAdapterClient()) {
            new MessageSendTester()
                    .type(MessageSendTester.Type.TELEMETRY)
                    .delay(Duration.ofSeconds(1))
                    .consumerFactory(ConsumerFactory.of(getSession().getConsumerClient(), getSession().getTenantId()))
                    .sender(client::send)
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
    @ParameterizedTest(name = "testHttpEventSingle-{0}")
    @MethodSource("getDevices")
    default void testHttpEventSingle(final DeviceSupplier device) throws Exception {
        try (HttpAdapterClient client = device.get().createHttpAdapterClient()) {
            new MessageSendTester()
                    .type(MessageSendTester.Type.EVENT)
                    .delay(Duration.ofSeconds(1))
                    .consumerFactory(ConsumerFactory.of(getSession().getConsumerClient(), getSession().getTenantId()))
                    .sender(client::send)
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
    @ParameterizedTest(name = "testHttpTelemetryBatch50-{0}")
    @MethodSource("getDevices")
    default void testHttpTelemetryBatch50(final DeviceSupplier device) throws Exception {

        try (HttpAdapterClient client = device.get().createHttpAdapterClient()) {
            new MessageSendTester()
                    .type(MessageSendTester.Type.TELEMETRY)
                    .delay(Duration.ofSeconds(1))
                    .consumerFactory(ConsumerFactory.of(getSession().getConsumerClient(), getSession().getTenantId()))
                    .sender(client::send)
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
    @ParameterizedTest(name = "testHttpEventBatch5After-{0}")
    @MethodSource("getDevices")
    default void testHttpEventBatch5After(final DeviceSupplier device) throws Exception {

        try (HttpAdapterClient client = device.get().createHttpAdapterClient()) {
            new MessageSendTester()
                    .type(MessageSendTester.Type.EVENT)
                    .delay(Duration.ofMillis(100))
                    .additionalSendTimeout(Duration.ofSeconds(10))
                    .consumerFactory(ConsumerFactory.of(getSession().getConsumerClient(), getSession().getTenantId()))
                    .sender(client::send)
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
    @ParameterizedTest(name = "testHttpEventBatch5Before-{0}")
    @MethodSource("getDevices")
    default void testHttpEventBatch5Before(final DeviceSupplier device) throws Exception {

        try (HttpAdapterClient client = device.get().createHttpAdapterClient()) {
            new MessageSendTester()
                    .type(MessageSendTester.Type.EVENT)
                    .delay(Duration.ZERO)
                    .additionalSendTimeout(Duration.ofSeconds(10))
                    .consumerFactory(ConsumerFactory.of(getSession().getConsumerClient(), getSession().getTenantId()))
                    .sender(client::send)
                    .amount(5)
                    .consume(MessageSendTester.Consume.BEFORE)
                    .execute();
        }

    }

    /**
     * Test a simple one-way command pattern.
     */
    @ParameterizedTest(name = "testHttpOneWayCommand-{0}")
    @MethodSource("getDevices")
    default void testHttpOneWayCommand(final DeviceSupplier deviceSupplier) throws Exception {

        var device = deviceSupplier.get();
        var vertx = getSession().getVertx();

        try (
                var subordinate = new HttpSubordinate(getSession(), device);
        ) {

            new CommandTester()
                    .vertx(vertx)
                    .mode(ONE_WAY)
                    .subordinate(subordinate)
                    .initiator(subordinate::initiate)
                    .commanderFactory(CommanderFactory.of(
                            getSession().getConsumerClient(),
                            getSession().getTenantId()
                    ))
                    .amount(5)
                    .delay(Duration.ofSeconds(1))
                    .execute();

        }

    }

    /**
     * Test a simple request/response command pattern.
     */
    @ParameterizedTest(name = "testHttpRequestResponseCommand-{0}")
    @MethodSource("getDevices")
    default void testHttpRequestResponseCommand(final DeviceSupplier deviceSupplier) throws Exception {

        var device = deviceSupplier.get();
        var vertx = getSession().getVertx();

        try (
                var subordinate = new HttpSubordinate(getSession(), device);
        ) {

            new CommandTester()
                    .vertx(vertx)
                    .mode(REQUEST_RESPONSE)
                    .subordinate(subordinate)
                    .initiator(subordinate::initiate)
                    .commanderFactory(CommanderFactory.of(
                            getSession().getConsumerClient(),
                            getSession().getTenantId()
                    ))
                    .amount(5)
                    .delay(Duration.ofSeconds(1))
                    .execute();

        }

    }

    /**
     * Test for an invalid device.
     * <p>
     * With an invalid device, no messages must pass.
     */
    @ParameterizedTest(name = "testHttpDeviceFails-{0}")
    @MethodSource("getInvalidDevices")
    default void testHttpDeviceFails(final DeviceSupplier deviceSupplier) throws Exception {

        log.info("Testing invalid devices, the following exception may be expected");
        var device = deviceSupplier.get();

        /*
         * We test an invalid device by trying to send either telemetry or event messages.
         * Two separate connections, and more than one message.
         */

        try (HttpAdapterClient client = device.createHttpAdapterClient()) {
            client.suppressExceptions(
                    statusCode(401)
                            .or(causedBy(SSLHandshakeException.class)));
            assertThrows(TimeoutException.class, () -> {
                new MessageSendTester()
                        .type(MessageSendTester.Type.TELEMETRY)
                        .delay(Duration.ofSeconds(1))
                        .consumerFactory(ConsumerFactory.of(getSession().getConsumerClient(), getSession().getTenantId()))
                        .sender(client::send)
                        .amount(5)
                        .consume(MessageSendTester.Consume.BEFORE)
                        .execute();
            });
        }

        try (HttpAdapterClient client = device.createHttpAdapterClient()) {
            client.suppressExceptions(
                    statusCode(401)
                            .or(causedBy(SSLHandshakeException.class)));
            assertThrows(TimeoutException.class, () -> {
                new MessageSendTester()
                        .type(MessageSendTester.Type.EVENT)
                        .delay(Duration.ofSeconds(1))
                        .consumerFactory(ConsumerFactory.of(getSession().getConsumerClient(), getSession().getTenantId()))
                        .sender(client::send)
                        .amount(5)
                        .consume(MessageSendTester.Consume.BEFORE)
                        .execute();
            });
        }

    }
}
