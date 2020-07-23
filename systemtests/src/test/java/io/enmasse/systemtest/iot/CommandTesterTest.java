/*
 * Copyright 2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package io.enmasse.systemtest.iot;

import io.enmasse.systemtest.iot.CommandTester.Command;
import io.enmasse.systemtest.iot.CommandTester.CommandResponse;
import io.enmasse.systemtest.iot.CommandTester.CommanderFactory;
import io.enmasse.systemtest.iot.CommandTester.Context;
import io.enmasse.systemtest.iot.CommandTester.ReceivedCommand;
import io.enmasse.systemtest.iot.CommandTester.ReceivedCommandResponse;
import io.enmasse.systemtest.iot.CommandTester.Subordinate;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.util.concurrent.TimeoutException;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Stream;

import static io.enmasse.systemtest.framework.TestTag.FRAMEWORK;
import static io.enmasse.systemtest.iot.CommandTester.Mode.ONE_WAY;
import static io.enmasse.systemtest.iot.CommandTester.Mode.REQUEST_RESPONSE;
import static io.vertx.core.Future.failedFuture;
import static io.vertx.core.Future.succeededFuture;
import static io.vertx.core.buffer.Buffer.buffer;
import static java.util.UUID.randomUUID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.catchThrowable;

@Tag(FRAMEWORK)
public class CommandTesterTest {

    private static class MyException extends Exception {
    }

    /**
     * Create an interceptor which skips the first X items.
     */
    <T> Function<T, Stream<T>> skip(int skip) {
        return new Function<>() {

            private int skipped;

            @Override
            public Stream<T> apply(T t) {
                if (this.skipped < skip) {
                    this.skipped++;
                    return Stream.empty();
                } else {
                    return Stream.of(t);
                }
            }
        };
    }

    <T> Function<T, Stream<T>> drop() {
        return x -> Stream.empty();
    }

    static class Mock {

        private static final Function<Command, Stream<Command>> DEFAULT_COMMAND_INTERCEPTOR = Stream::of;
        private static final Function<CommandResponse, Stream<CommandResponse>> DEFAULT_RESPONSE_INTERCEPTOR = Stream::of;

        final CommanderFactory commander;
        final Subordinate subordinate;

        private Consumer<ReceivedCommand> commandReceiver;
        private Consumer<ReceivedCommandResponse> responseReceiver;

        private Function<Command, Stream<Command>> interceptCommand = DEFAULT_COMMAND_INTERCEPTOR;
        private Function<CommandResponse, Stream<CommandResponse>> interceptResponse = DEFAULT_RESPONSE_INTERCEPTOR;

        Mock() {
            this(randomUUID().toString());
        }

        Mock(final String deviceId) {
            this.commander = (vertx, replyTo, messageConsumer) -> {

                Mock.this.responseReceiver = response -> {
                    if (response.getId() == null) {
                        // drop
                        return;
                    }
                    if (response.getId().equals(replyTo)) {
                        // drop
                        return;
                    }
                    messageConsumer.accept(response);
                };

                return new CommandTester.Commander() {

                    @Override
                    public Future<?> command(Command command) {
                        Mock.this.interceptCommand
                                .apply(command)
                                .forEach(Mock.this::sendCommand);
                        return succeededFuture();
                    }

                    @Override
                    public void close() {
                    }
                };
            };

            this.subordinate = new Subordinate() {

                @Override
                public Future<?> subscribe(final Context context, final Consumer<ReceivedCommand> commandReceiver) {
                    Mock.this.commandReceiver = commandReceiver;
                    return succeededFuture();
                }

                @Override
                public Future<?> respond(final Context context, final CommandResponse response) {
                    Mock.this.interceptResponse
                            .apply(response)
                            .forEach(Mock.this::sendResponse);
                    return succeededFuture();
                }

                @Override
                public String getDeviceId() {
                    return deviceId;
                }
            };
        }

        /**
         * Mock sending a command to the device.
         */
        public void sendCommand(final Command command) {
            this.commandReceiver.accept(new ReceivedCommand(
                    randomUUID().toString(),
                    command.getCommand(),
                    command.getDeviceId(),
                    command.getContentType(),
                    command.getPayload()
            ));
        }

        /**
         * Mock sending a response to the tester.
         */
        public void sendResponse(final CommandResponse response) {
            this.responseReceiver.accept(new ReceivedCommandResponse(
                    response.getId(),
                    response.getContentType(),
                    response.getPayload(),
                    Integer.toString(response.getStatus()),
                    response.getDeviceId()
            ));
        }

        public Mock interceptCommand(Function<Command, Stream<Command>> interceptor) {
            this.interceptCommand = interceptor != null ? interceptor : DEFAULT_COMMAND_INTERCEPTOR;
            return this;
        }

        public Mock interceptResponse(Function<CommandResponse, Stream<CommandResponse>> interceptor) {
            this.interceptResponse = interceptor != null ? interceptor : DEFAULT_RESPONSE_INTERCEPTOR;
            return this;
        }
    }

    @Test
    public void testFailsMissingSetup() {

        assertThatThrownBy(() -> {
            new CommandTester()
                    .execute();
        })

                .isInstanceOf(NullPointerException.class);

    }

    @Test
    public void testBasicRequestResponse() throws Exception {

        final Mock mock = new Mock();

        new CommandTester()
                .amount(5)
                .commanderFactory(mock.commander)
                .subordinate(mock.subordinate)
                .execute();

    }

    @Test
    public void testBasicOneWay() throws Exception {

        final Mock mock = new Mock()
                .interceptResponse(drop());

        new CommandTester()
                .amount(5)
                .mode(ONE_WAY)
                .commanderFactory(mock.commander)
                .subordinate(mock.subordinate)
                .execute();

    }

    @Test
    public void testRequestResponseSlowResponder() throws Exception {

        final Vertx vertx = Vertx.vertx();
        try {

            final Mock mock = new Mock();
            mock.interceptResponse(r -> {
                // delay response by 500 ms (acceptable)
                vertx.setTimer(500, x -> {
                    mock.sendResponse(r);
                });
                return Stream.empty();
            });

            new CommandTester()
                    .vertx(vertx)
                    .amount(5)
                    .commanderFactory(mock.commander)
                    .subordinate(mock.subordinate)
                    .execute();
        } finally {
            vertx.close();
        }

    }

    @Test
    public void testRequestResponseTooSlowResponder() {

        final Vertx vertx = Vertx.vertx();
        try {

            final Mock mock = new Mock();
            mock.interceptResponse(r -> {
                // delay response by 2 seconds (not acceptable)
                vertx.setTimer(2_000, x -> {
                    mock.sendResponse(r);
                });
                return Stream.empty();
            });

            assertThatThrownBy(() -> {
                new CommandTester()
                        .vertx(vertx)
                        .amount(5)
                        .commanderFactory(mock.commander)
                        .subordinate(mock.subordinate)
                        .execute();
            })

                    .hasRootCauseInstanceOf(TimeoutException.class);

        } finally {
            vertx.close();
        }

    }

    @Test
    public void testOneWayReceivingResponses() {

        final Mock mock = new Mock();

        assertThatThrownBy(() -> {
            new CommandTester()
                    .amount(5)
                    .mode(ONE_WAY)
                    .commanderFactory(mock.commander)
                    .subordinate(mock.subordinate)
                    .execute();
        })

                // we received responses when doing one way commands
                .isInstanceOf(AssertionError.class);

    }

    /**
     * Test what happens when two commands get sent, rather than one.
     * <p>
     * This must fail the test, as we would get two responses, while having a single active command in progress.
     * <p>
     * <strong>Note:</strong> This should also cause warnings in the log about "dangling response futures", which we accept.
     */
    @Test
    public void testDuplicateCommand() {

        final Mock mock = new Mock()
                .interceptCommand(command -> Stream.of(
                        command, command
                ));

        assertThatThrownBy(() -> {
            new CommandTester()
                    .amount(5)
                    .commanderFactory(mock.commander)
                    .subordinate(mock.subordinate)
                    .execute();
        })

                // we detect unsolicited responses, and fail when asserting
                .isInstanceOf(AssertionError.class);

    }

    /**
     * Simulate that we receive two responses for each command, rather than one.
     * <p>
     * This must fail the test, as this should be counted as unsolicited response. Which must not happen.
     */
    @Test
    public void testDuplicateResponse() {

        final Mock mock = new Mock()
                .interceptResponse(response -> Stream.of(
                        response, response
                ));

        assertThatThrownBy(() -> {
            new CommandTester()
                    .amount(5)
                    .commanderFactory(mock.commander)
                    .subordinate(mock.subordinate)
                    .execute();
        })

                // we detect unsolicited responses, and fail when asserting
                .isInstanceOf(AssertionError.class);

    }

    @Test
    public void testRequestResponseMissingResponse() {

        final Mock mock = new Mock()
                .interceptResponse(drop());

        assertThatThrownBy(() -> {
            new CommandTester()
                    .amount(5)
                    .mode(REQUEST_RESPONSE)
                    .commanderFactory(mock.commander)
                    .subordinate(mock.subordinate)
                    .execute();
        })

                // receiving no responses will not advance the internal counter
                // thus we will time out
                .hasRootCauseInstanceOf(TimeoutException.class);

    }


    @Test
    public void testRequestResponseWrongResponse() {

        final Mock mock = new Mock()
                .interceptResponse(response -> Stream.of(new CommandResponse(
                        response.getId(),
                        response.getContentType(),
                        buffer(),
                        response.getStatus(),
                        response.getDeviceId()
                ))); // empty buffer

        assertThatThrownBy(() -> {
            new CommandTester()
                    .amount(5)
                    .mode(REQUEST_RESPONSE)
                    .commanderFactory(mock.commander)
                    .subordinate(mock.subordinate)
                    .execute();
        })

                // receiving wrong responses will not advance the internal counter
                // thus we will time out
                .hasRootCauseInstanceOf(TimeoutException.class);

    }

    @Test
    public void testRequestResponseSingleWrongResponse() throws Exception {

        final Mock mock = new Mock()
                .interceptResponse(response -> {

                    var idx = response.getPayload().getInt(0);
                    var attempt = response.getPayload().getInt(4);
                    if (idx == 1 && attempt == 1) {
                        // for the response #1, we send back an illegal response
                        return Stream.of(new CommandResponse(
                                response.getId(),
                                response.getContentType(),
                                buffer(),
                                response.getStatus(),
                                response.getDeviceId()
                        ));
                    }

                    return Stream.of(response);
                });

        new CommandTester()
                .amount(5)
                .acceptableLoss(0.2)
                .mode(REQUEST_RESPONSE)
                .commanderFactory(mock.commander)
                .subordinate(mock.subordinate)
                .execute();

    }

    @ParameterizedTest(name = "testLossDetection-{0}-{1}-{2}")
    @CsvSource({
            "10,0.2,1,true",
            "10,0.2,2,true",
            "10,0.2,3,false",
    })
    public void testLossDetection(final int amount, final double acceptableLoss, final int skip, final boolean accepted) {

        final Mock mock = new Mock()
                .interceptCommand(skip(skip));

        var result = catchThrowable(() -> new CommandTester()
                .amount(amount)
                .acceptableLoss(acceptableLoss)
                .commanderFactory(mock.commander)
                .subordinate(mock.subordinate)
                .execute());

        if (accepted) {
            assertThat(result)
                    .as("Outcome must be accepted")
                    .isNull();
        } else {
            assertThat(result)
                    .as("Expected assertion error")
                    .isInstanceOf(AssertionError.class);
        }

    }

    @Test
    public void testSubscriptionFailure() {

        assertThatThrownBy(() -> {

            new CommandTester()
                    .amount(5)
                    .commanderFactory((vertx, replyTo, messageConsumer) -> new CommandTester.Commander() {
                        @Override
                        public Future<?> command(Command command) {
                            return succeededFuture();
                        }

                        @Override
                        public void close() {
                        }
                    })
                    .subordinate(new Subordinate() {
                        @Override
                        public Future<?> subscribe(final Context context, final Consumer<ReceivedCommand> commandReceiver) {
                            return failedFuture(new MyException());
                        }

                        @Override
                        public Future<?> respond(final Context context, final CommandResponse response) {
                            return succeededFuture();
                        }

                        @Override
                        public String getDeviceId() {
                            return randomUUID().toString();
                        }
                    })
                    .execute();
        })

                .hasCauseInstanceOf(MyException.class);

    }

    @Test
    public void testCommandFailure() {

        assertThatThrownBy(() -> {

            new CommandTester()
                    .amount(5)
                    .commanderFactory((vertx, replyTo, messageConsumer) -> new CommandTester.Commander() {
                        @Override
                        public Future<?> command(Command command) {
                            return failedFuture(new MyException());
                        }

                        @Override
                        public void close() {
                        }
                    })
                    .subordinate(new Subordinate() {
                        @Override
                        public Future<?> subscribe(final Context context, final Consumer<ReceivedCommand> commandReceiver) {
                            return succeededFuture();
                        }

                        @Override
                        public Future<?> respond(final Context context, final CommandResponse response) {
                            return succeededFuture();
                        }

                        @Override
                        public String getDeviceId() {
                            return randomUUID().toString();
                        }
                    })
                    .execute();
        })

                .hasCauseInstanceOf(TimeoutException.class);

    }

}
