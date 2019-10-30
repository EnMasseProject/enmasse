/*
 * Copyright 2019, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package io.enmasse.systemtest.iot;

import io.enmasse.systemtest.amqp.AmqpClient;
import io.enmasse.systemtest.logs.CustomLogger;
import io.enmasse.systemtest.time.TimeoutBudget;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.json.JsonObject;
import org.apache.qpid.proton.amqp.messaging.Data;
import org.apache.qpid.proton.message.Message;
import org.opentest4j.AssertionFailedError;
import org.slf4j.Logger;

import java.time.Duration;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.TimeoutException;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.fail;

/**
 * Run a message send test.
 * <br>
 * This is an generic message send tester, which is configured with the send and receive parameters,
 * timeouts, and the actual sender and consumer instances. When everything is set up, the
 * {@link #execute()} methods executes the test. This method may be called multiple times. However
 * it will throws {@link AssertionFailedError}s in case of assertion failures.
 */
public class MessageSendTester {

    private static final Logger LOGGER = CustomLogger.getLogger();
    private Type type = Type.TELEMETRY;
    private int amount = 1;
    private Consume consume = Consume.BEFORE;
    private Duration delay = Duration.ofSeconds(1);
    private Duration additionalSendTimeout = Duration.ZERO;
    private double sendRepeatFactor = 4.0; // 4 times each message
    private Duration defaultReceiveSlot = Duration.ofMillis(100);
    private Duration additionalReceiveTimeout = Duration.ofSeconds(1);
    private Duration receiveTimeout;
    private int acceptableMessageLoss = 0;
    private Sender sender;
    private ConsumerFactory consumerFactory;

    /**
     * Set the type to send.
     */
    public MessageSendTester type(final Type type) {
        this.type = type;
        return this;
    }

    /**
     * Add an additional send timeout. This is added on top of the calculated send timeout.
     */
    public MessageSendTester additionalSendTimeout(final Duration additionalSendTimeout) {
        this.additionalSendTimeout = additionalSendTimeout;
        return this;
    }

    /**
     * Set the number of messages to send.
     */
    public MessageSendTester amount(final int amount) {
        this.amount = amount;
        return this;
    }

    /**
     * Set when to start the message consumer.
     */
    public MessageSendTester consume(final Consume consume) {
        this.consume = consume;
        return this;
    }

    /**
     * The actual message sender.
     */
    public MessageSendTester sender(final Sender sender) {
        this.sender = sender;
        return this;
    }

    /**
     * The delay between sending messages.
     */
    public MessageSendTester delay(final Duration delay) {
        this.delay = delay;
        return this;
    }

    /**
     * The factory to create message consumers.
     */
    public MessageSendTester consumerFactory(final ConsumerFactory consumerFactory) {
        this.consumerFactory = consumerFactory;
        return this;
    }

    /**
     * Set the number of messages accepted to be lost.
     */
    public MessageSendTester acceptableMessageLoss(final int acceptableMessageLoss) {
        this.acceptableMessageLoss = acceptableMessageLoss;
        return this;
    }

    /**
     * Execute the test.
     * <br>
     * This must not change the state of this instance. This method may be called multiple times.
     */
    public void execute() throws Exception, AssertionFailedError {
        try (Executor executor = new Executor()) {
            executor.execute();
        }
    }

    /**
     * Calculate the receive timeout.
     * <br>
     * will be used.
     * Otherwise, the receive timeout is the {@link #defaultReceiveSlot} times the {@link #amount} of
     *
     * @return The receive timeout.
     */
    private Duration calcReceiveTimeout() {
        if (this.receiveTimeout != null) {
            return this.receiveTimeout;
        }
        return this.defaultReceiveSlot.multipliedBy(this.amount).plus(this.additionalReceiveTimeout);
    }

    /**
     * Calculate the send timeout.
     * <br>
     * This is calculated by: {@link #amount} times {@link #sendRepeatFactor} times {@link #delay} plus
     * {@link #additionalSendTimeout}.
     */
    private Duration calcSendTimeout() {
        var sendDuration = Duration.ofMillis((long) (this.delay.toMillis() * this.amount * this.sendRepeatFactor));
        return sendDuration.plus(MessageSendTester.this.additionalSendTimeout);
    }

    public enum Type {
        TELEMETRY(MessageType.TELEMETRY), EVENT(MessageType.EVENT);

        private MessageType type;

        Type(final MessageType type) {
            this.type = type;
        }

        public MessageType type() {
            return this.type;
        }
    }

    public enum Consume {
        BEFORE, AFTER
    }

    @FunctionalInterface
    public interface Sender {
        /**
         * Send a single message.
         *
         * @param type        the type to send.
         * @param payload     the payload to send, may be {@code null}.
         * @param sendTimeout timeout for the send operation.
         * @return {@code true} if the message was accepted, {@code false} otherwise.
         * @throws Exception In case anything went wrong.
         */
        boolean send(Type type, JsonObject payload, Duration sendTimeout) throws Exception;
    }

    @FunctionalInterface
    public interface ConsumerFactory {

        static ConsumerFactory of(final AmqpClient client, final String tenantId) {
            return (type, messageConsumer) -> {

                var receiver = client.recvMessagesWithStatus(type.type().address() + "/" + tenantId, msg -> {
                    messageConsumer.accept(msg);
                    return false;
                });

                return (AutoCloseable) receiver;
            };
        }

        AutoCloseable start(Type type, Consumer<Message> messageConsumer);
    }

    /**
     * A received message.
     */
    public static class ReceivedMessage {

        private Message message;
        private JsonObject payload;

        ReceivedMessage(final Message message, final JsonObject payload) {
            this.message = message;
            this.payload = payload;
        }

        public Message getMessage() {
            return this.message;
        }
    }

    /**
     * Implements to actual test execution.
     * <br>
     * This method does the actual work of the {@link MessageSendTester}. Although it is a non-static
     * nested class, it must not alter the state of the {@link MessageSendTester} instance. Any mutable
     * state must go into the instance of this class.
     */
    private class Executor implements AutoCloseable {

        private final String testId;
        private final List<ReceivedMessage> receivedMessages = new LinkedList<>();
        private long sendTime;

        private AutoCloseable consumer;

        Executor() {
            this.testId = UUID.randomUUID().toString();
        }

        public void execute() throws Exception {

            Objects.requireNonNull(MessageSendTester.this.sender, "'sender' must be set before calling execute()");
            Objects.requireNonNull(MessageSendTester.this.consumerFactory, "'consumerFactory' must be set before calling execute()");

            final long delay = MessageSendTester.this.delay.toMillis();
            final int amount = MessageSendTester.this.amount;

            // setup consumer (before)
            if (Consume.BEFORE == MessageSendTester.this.consume) {
                startConsumer();
            }

            // duration = amount * delay * factor
            var sendDuration = calcSendTimeout();
            LOGGER.info("Sending messages - total timeout: {} - delay: {} ms", sendDuration, delay);
            var sendTimeout = TimeoutBudget.ofDuration(sendDuration);

            // send
            int i = 0;
            while (i < amount) {

                if (sendTimeout.timeoutExpired()) {
                    LOGGER.info("Send timeout");
                    throw new TimeoutException("Failed to execute message send test due to send timeout.");
                }

                var payload = new JsonObject();
                payload.put("timestamp", System.currentTimeMillis());
                payload.put("test-id", this.testId);
                payload.put("index", i);
                if (MessageSendTester.this.sender.send(MessageSendTester.this.type, payload, Duration.ofSeconds(1))) {
                    i++;
                }

                Thread.sleep(delay);

            }

            LOGGER.info("Done sending messages");

            // setup consumer (after)
            if (Consume.AFTER == MessageSendTester.this.consume) {
                startConsumer();
            }

            // consumer ready?
            final Duration receiveTimeout = calcReceiveTimeout();
            LOGGER.info("Receive timeout: {}", receiveTimeout);
            var receiveBudget = TimeoutBudget.ofDuration(receiveTimeout);
            var receiveSleep = Math.min(receiveTimeout.toMillis() / 10, 1_000);
            LOGGER.info("Receive sleep period: {}", receiveSleep);
            while (!isConsumerReady(receiveBudget)) {
                if (receiveBudget.timeoutExpired()) {
                    LOGGER.info("Receive timeout");
                    throw new TimeoutException("Failed to execute message send test due to receive timeout.");
                }
                Thread.sleep(receiveSleep);
            }

            // stop consumer
            stopConsumer();

            // assert result
            assertResult();
        }

        private boolean isConsumerReady(final TimeoutBudget receiveBudget) {

            final int missing = MessageSendTester.this.amount - this.receivedMessages.size();

            if (missing <= 0) {
                // we received all messages we expected - success
                return true;
            }

            // we are still waiting for all messages, but the timeout expired and we are in the acceptable loss range - success
            return receiveBudget.timeoutExpired() && missing <= MessageSendTester.this.acceptableMessageLoss;

            // need to wait longer

        }

        private void assertResult() {

            double avgMessageTime = ((double) this.sendTime) / ((double) this.receivedMessages.size());
            LOGGER.info("Average message RTT: {} ms", String.format("%.2f", avgMessageTime));

            final int missing = MessageSendTester.this.amount - this.receivedMessages.size();
            if (missing > MessageSendTester.this.acceptableMessageLoss) {
                fail(String.format("Unacceptable loss of messages - expected: %s, received: %s, acceptedLoss: %s, actualLoss: %s",
                        MessageSendTester.this.amount, this.receivedMessages.size(),
                        MessageSendTester.this.acceptableMessageLoss, missing));
            }
        }

        private void startConsumer() {
            if (this.consumer != null) {
                throw new IllegalStateException("'startConsumer' called twice");
            }
            this.consumer = MessageSendTester.this.consumerFactory.start(MessageSendTester.this.type, this::handleMessage);
        }

        private void handleMessage(final Message message) {

            LOGGER.info("Received message - {}", message);

            var body = message.getBody();
            if (!(body instanceof Data)) {
                handleInvalidMessage();
                return;
            }

            var json = new JsonObject(Buffer.buffer(((Data) body).getValue().getArray()));
            var testId = json.getString("test-id");
            var timestamp = json.getInteger("timestamp");
            if (!this.testId.equals(testId) || timestamp == null) {
                handleInvalidMessage();
                return;
            }

            handleValidMessage(message, timestamp, json);
        }

        private void handleInvalidMessage() {
        }

        private void handleValidMessage(final Message message, int timestamp, final JsonObject payload) {
            var diff = System.currentTimeMillis() - timestamp;
            sendTime += diff;
            LOGGER.debug("Received message took {} ms", diff);
            this.receivedMessages.add(new ReceivedMessage(message, payload));
        }

        private void stopConsumer() throws Exception {

            if (this.consumer == null) {
                return;
            }

            try {
                // close
                this.consumer.close();
            } finally {
                // ensure we get set to null
                this.consumer = null;
            }
        }

        @Override
        public void close() throws Exception {
            stopConsumer();
        }
    }

}
