/*
 * Copyright 2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.systemtest.messagingclients;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.Flow.Subscriber;
import java.util.concurrent.Flow.Subscription;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import org.slf4j.Logger;

import io.enmasse.systemtest.logs.CustomLogger;
import io.enmasse.systemtest.time.TimeoutBudget;
import io.enmasse.systemtest.utils.ThrowingSupplier;

public class ReceiverTester implements Subscriber<String> {

    private Logger LOGGER = CustomLogger.getLogger();

    private static final String TEST_MESSAGE_BODY = "test message";

    private AtomicInteger testMessagesReceived = new AtomicInteger(0);
    private AtomicInteger receivedMessages = new AtomicInteger(0);
    private AtomicBoolean testStarted = new AtomicBoolean(false);

    private CompletableFuture<Boolean> expectedMessagesResult;
    private int expectedMessages;
    private ThrowingSupplier<ExternalMessagingClient> testMessageSenderSupplier;


    public ReceiverTester(int expectedMessages, ThrowingSupplier<ExternalMessagingClient> testMessageSenderSupplier) {
        this.expectedMessages = expectedMessages;
        this.testMessageSenderSupplier = testMessageSenderSupplier;

        expectedMessagesResult = new CompletableFuture<Boolean>();
    }

    @Override
    public void onSubscribe(Subscription subscription) {
        LOGGER.info("ReceiverTester subscription started");
    }

    @Override
    public void onNext(String item) {
        LOGGER.info("DEBUG!!!!! message received");
        if (item.contains(TEST_MESSAGE_BODY)) {
            testMessagesReceived.getAndIncrement();
            if (!testStarted.get()) {
                LOGGER.info("First test message received, receiver is ready");
                testStarted.set(true);
            }
        } else {
            if (receivedMessages.incrementAndGet() == expectedMessages) {
                expectedMessagesResult.complete(true);
            }
        }
    }

    @Override
    public void onError(Throwable throwable) {
        LOGGER.warn("Error in stream", throwable);
        expectedMessagesResult.complete(false);
    }

    @Override
    public void onComplete() {
        LOGGER.info("ReceiverTester subscription completed");
    }

    public void waitForReceiverAttached() throws Exception {
        var timeout = new TimeoutBudget(45, TimeUnit.SECONDS);
        var testSender = testMessageSenderSupplier.get()
                .withCount(1)
                .withMessageBody(TEST_MESSAGE_BODY);

        while (!testStarted.get() && !timeout.timeoutExpired()) {
            try {
                testSender.run();
                Thread.sleep(1000);
            } finally {
                testSender.stop();
            }
        }
        if (!testStarted.get()) {
            //timeout expired, do one last check
            Thread.sleep(15000);
            if (!testStarted.get()) {
                throw new IllegalStateException("Receiver not looking like it's attached");
            }
        }
    }

    public int getTestMessagesReceived() {
        return testMessagesReceived.get();
    }

    public int getReceivedMessages() {
        return receivedMessages.get();
    }

    public Future<Boolean> getExpectedMessagesResult() {
        return expectedMessagesResult;
    }
}
