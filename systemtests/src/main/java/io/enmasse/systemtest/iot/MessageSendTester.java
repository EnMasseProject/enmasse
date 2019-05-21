/*
 * Copyright 2019, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package io.enmasse.systemtest.iot;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.Duration;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.TimeoutException;
import java.util.function.Consumer;

import org.apache.qpid.proton.amqp.messaging.Data;
import org.apache.qpid.proton.message.Message;
import org.slf4j.Logger;

import io.enmasse.systemtest.CustomLogger;
import io.enmasse.systemtest.TimeoutBudget;
import io.enmasse.systemtest.amqp.AmqpClient;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.json.JsonObject;

public class MessageSendTester {

    private static final Logger log = CustomLogger.getLogger();

    public static enum Type {
        TELEMETRY(MessageType.TELEMETRY), EVENT(MessageType.EVENT);

        private MessageType type;

        private Type(final MessageType type) {
            this.type = type;
        }

        public MessageType type() {
            return this.type;
        }
    }

    public static enum Consume {
        BEFORE, AFTER;
    }

    @FunctionalInterface
    public interface Sender {
        /**
         * Send a single message.
         *
         * @param type the type to send.
         * @param payload the payload to send, may be {@code null}.
         * @return {@code true} if the message was accepted, {@code false} otherwise.
         * @throws Exception In case anything went wrong.
         */
        public boolean send(Type type, JsonObject payload) throws Exception;
    }

    @FunctionalInterface
    public interface ConsumerFactory {

        public AutoCloseable start(Type type, Consumer<Message> messageConsumer);

        public static ConsumerFactory of(final AmqpClient client, final String tenantId) {
            return new ConsumerFactory() {

                @Override
                public AutoCloseable start(final Type type, final Consumer<Message> messageConsumer) {

                    var receiver = client.recvMessagesWithStatus(type.type().address() + "/" + tenantId, msg -> {
                        messageConsumer.accept(msg);
                        return false;
                    });

                    return new AutoCloseable() {

                        @Override
                        public void close() throws Exception {
                            receiver.close();
                        }
                    };
                }
            };
        }
    }

    private Type type = Type.TELEMETRY;
    private int amount = 1;
    private Consume consume = Consume.BEFORE;
    private Duration delay = Duration.ofSeconds(1);
    private Duration additionalSendTimeout = Duration.ZERO;

    private double sendRepeatFactor = 4.0; // 4 times each message

    private Duration defaultReceiveSlot = Duration.ofMillis(100);
    private Duration receiveTimeout;

    private Sender sender;
    private ConsumerFactory consumerFactory;

    public MessageSendTester type(final Type type) {
        this.type = type;
        return this;
    }

    public MessageSendTester sendRepeatFactor(final double sendRepeatFactor) {
        this.sendRepeatFactor = sendRepeatFactor;
        return this;
    }

    public MessageSendTester additionalSendTimeout(final Duration additionalSendTimeout) {
        this.additionalSendTimeout = additionalSendTimeout;
        return this;
    }

    public MessageSendTester amount(final int amount) {
        this.amount = amount;
        return this;
    }

    public MessageSendTester consume(final Consume consume) {
        this.consume = consume;
        return this;
    }

    public MessageSendTester sender(final Sender sender) {
        this.sender = sender;
        return this;
    }

    public MessageSendTester delay(final Duration delay) {
        this.delay = delay;
        return this;
    }

    public MessageSendTester receiveTimeout(final Duration receiveTimeout) {
        this.receiveTimeout = receiveTimeout;
        return this;
    }

    public MessageSendTester consumerFactory(final ConsumerFactory consumerFactory) {
        this.consumerFactory = consumerFactory;
        return this;
    }

    public void execute() throws Exception {
        try (Executor executor = new Executor()) {
            executor.execute();
        }
    }

    public static class ReceivedMessage {

        private Message message;
        private JsonObject payload;

        public ReceivedMessage(final Message message, final JsonObject payload) {
            this.message = message;
            this.payload = payload;
        }

        public Message getMessage() {
            return this.message;
        }

        public JsonObject getPayload() {
            return this.payload;
        }
    }

    /**
     * Implements to actual test execution.
     * <br>
     * This method does the actual work of the {@link MessageSendTester}. Although it is a non-static
     * nested class, it must not alter the state of the {@link MessageSendTester} instance. Any mutable
     * state must go into the instance of this class.
     *
     */
    private class Executor implements AutoCloseable {

        private final String testId;
        private final List<ReceivedMessage> receivedMessages = new LinkedList<>();

        private AutoCloseable consumer;

        public Executor() {
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
            var sendDuration = Duration.ofMillis((long) (delay * amount * MessageSendTester.this.sendRepeatFactor));
            sendDuration = sendDuration.plus(MessageSendTester.this.additionalSendTimeout);
            log.info("Sending messages - total timeout: {}", sendDuration);
            var sendTimeout = TimeoutBudget.ofDuration(sendDuration);

            // send
            int i = 0;
            while (i < amount) {

                if (sendTimeout.timeoutExpired()) {
                    throw new TimeoutException("Failed to execute message send test due to send timeout.");
                }

                var payload = new JsonObject();
                payload.put("test-id", this.testId);
                payload.put("index", i);
                if (MessageSendTester.this.sender.send(MessageSendTester.this.type, payload)) {
                    i++;
                }

                Thread.sleep(delay);

            }

            // setup consumer (after)
            if (Consume.AFTER == MessageSendTester.this.consume) {
                startConsumer();
            }

            // consumer ready?
            final Duration receiveTimeout = calcReceiveTimeout();
            log.info("Receive timeout: {}", receiveTimeout);
            var receiveBudget = TimeoutBudget.ofDuration(receiveTimeout);
            while (!isConsumerReady()) {
                if (receiveBudget.timeoutExpired()) {
                    throw new TimeoutException("Failed to execute message send test due to receive timeout.");
                }
                Thread.sleep(1_000);
            }

            // stop consumer
            stopConsumer();

            // assert result
            assertResult();
        }

        private Duration calcReceiveTimeout() {
            if ( MessageSendTester.this.receiveTimeout != null ) {
                return MessageSendTester.this.receiveTimeout;
            }
            return MessageSendTester.this.defaultReceiveSlot.multipliedBy(MessageSendTester.this.amount);
        }

        private boolean isConsumerReady() {
            return this.receivedMessages.size() >= MessageSendTester.this.amount;
        }

        private void assertResult() {
            assertEquals(MessageSendTester.this.amount, this.receivedMessages.size());
        }

        private void startConsumer() {
            this.consumer = MessageSendTester.this.consumerFactory.start(MessageSendTester.this.type, this::handleMessage);
        }

        private void handleMessage(final Message message) {

            log.info("Received message - {}", message);

            var body = message.getBody();
            if (!(body instanceof Data)) {
                handleInvalidMessage(message);
                return;
            }

            var json = new JsonObject(Buffer.buffer(((Data) body).getValue().getArray()));
            var testId = json.getString("test-id");
            if (!this.testId.equals(testId)) {
                handleInvalidMessage(message);
                return;
            }

            handleValidMessage(message, json);
        }

        private void handleInvalidMessage(final Message message) {}

        private void handleValidMessage(final Message message, final JsonObject payload) {
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
