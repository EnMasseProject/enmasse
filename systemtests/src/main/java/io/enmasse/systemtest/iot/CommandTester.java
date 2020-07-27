/*
 * Copyright 2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package io.enmasse.systemtest.iot;

import com.google.common.base.MoreObjects;
import com.google.common.io.BaseEncoding;
import io.enmasse.systemtest.amqp.AmqpClient;
import io.enmasse.systemtest.framework.LoggerUtils;
import io.enmasse.systemtest.utils.TestUtils;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import org.apache.qpid.proton.Proton;
import org.apache.qpid.proton.amqp.Binary;
import org.apache.qpid.proton.amqp.messaging.Data;
import org.assertj.core.api.Condition;
import org.assertj.core.api.SoftAssertions;
import org.slf4j.Logger;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.OptionalLong;
import java.util.UUID;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

import static io.enmasse.iot.utils.MoreFutures.await;
import static io.enmasse.systemtest.iot.CommandTester.Commander.fullResponseAddress;
import static io.enmasse.systemtest.iot.MessageType.COMMAND;
import static io.enmasse.systemtest.iot.MessageType.COMMAND_RESPONSE;
import static io.vertx.core.Future.succeededFuture;
import static java.lang.System.lineSeparator;
import static java.time.Instant.now;
import static java.util.Optional.ofNullable;
import static java.util.stream.Collectors.joining;
import static org.assertj.core.api.Assertions.assertThat;

public class CommandTester {

    private static final Logger log = LoggerUtils.getLogger();

    private static final Condition<? super Executor.RequestResponse> unsolicitedResponse = new Condition<>(
            p -> p.command == null, "unsolicited response"
    );

    private static final String MIME_TYPE = "application/octet-stream";

    public enum Mode {
        ONE_WAY,
        REQUEST_RESPONSE,
    }

    public static class ReceivedCommand {

        private final String id;
        private final String command;
        private final String deviceId;
        private final String contentType;
        private final Buffer payload;

        public ReceivedCommand(final String id, final String command, final String deviceId, final String contentType, final Buffer payload) {
            this.id = id;
            this.command = command;
            this.deviceId = deviceId;
            this.contentType = contentType;
            this.payload = payload;
        }

        public String getId() {
            return this.id;
        }

        public String getContentType() {
            return this.contentType;
        }

        public Buffer getPayload() {
            return this.payload;
        }

        public String getDeviceId() {
            return this.deviceId;
        }

        public String getCommand() {
            return this.command;
        }

        @Override
        public String toString() {
            return MoreObjects.toStringHelper(this)
                    .add("id", this.id)
                    .add("command", this.command)
                    .add("deviceId", this.deviceId)
                    .add("contentType", this.contentType)
                    .add("payload", this.payload != null ? BaseEncoding.base16().encode(this.payload.getBytes()) : null)
                    .toString();
        }
    }

    public static class Command {

        private final String command;
        private final String replyId;
        private final String deviceId;
        private final String contentType;
        private final Buffer payload;

        public Command(final String command, final String replyId, final String deviceId, final String contentType, final Buffer payload) {
            this.command = command;
            this.replyId = replyId;
            this.deviceId = deviceId;
            this.contentType = contentType;
            this.payload = payload;
        }

        public String getDeviceId() {
            return this.deviceId;
        }

        public String getContentType() {
            return this.contentType;
        }

        public Buffer getPayload() {
            return this.payload;
        }

        public String getCommand() {
            return this.command;
        }

        public String getReplyId() {
            return this.replyId;
        }

        @Override
        public String toString() {
            return MoreObjects.toStringHelper(this)
                    .add("command", this.command)
                    .add("replyId", this.replyId)
                    .add("deviceId", this.deviceId)
                    .add("contentType", this.contentType)
                    .add("payload", BaseEncoding.base16().encode(this.payload.getBytes()))
                    .toString();
        }
    }

    public static class ReceivedCommandResponse {
        private final String id;
        private final String contentType;
        private final Buffer payload;
        private final String status;
        private final String deviceId;

        public ReceivedCommandResponse(
                final String id,
                final String contentType,
                final Buffer payload,
                final String status,
                final String deviceId
        ) {
            this.id = id;
            this.contentType = contentType;
            this.payload = payload;
            this.status = status;
            this.deviceId = deviceId;
        }

        public String getId() {
            return this.id;
        }

        public String getContentType() {
            return this.contentType;
        }

        public Buffer getPayload() {
            return this.payload;
        }

        public String getStatus() {
            return this.status;
        }

        public String getDeviceId() {
            return this.deviceId;
        }

        @Override
        public String toString() {
            return MoreObjects.toStringHelper(this)
                    .add("id", this.id)
                    .add("payload", BaseEncoding.base16().encode(this.payload.getBytes()))
                    .add("status", this.status)
                    .add("deviceId", this.deviceId)
                    .toString();
        }
    }

    public static class CommandResponse {
        private final String id;
        private final String contentType;
        private final Buffer payload;
        private final int status;
        private final String deviceId;

        public CommandResponse(
                final String id,
                final String contentType,
                final Buffer payload,
                final int status,
                final String deviceId
        ) {
            this.id = id;
            this.contentType = contentType;
            this.payload = payload;
            this.status = status;
            this.deviceId = deviceId;
        }

        public String getId() {
            return this.id;
        }

        public String getContentType() {
            return this.contentType;
        }

        public Buffer getPayload() {
            return this.payload;
        }

        public int getStatus() {
            return this.status;
        }

        public String getDeviceId() {
            return this.deviceId;
        }

        @Override
        public String toString() {
            return MoreObjects.toStringHelper(this)
                    .add("id", this.id)
                    .add("contentType", this.contentType)
                    .add("payload", BaseEncoding.base16().encode(this.payload.getBytes()))
                    .add("status", this.status)
                    .add("deviceId", this.deviceId)
                    .toString();
        }
    }

    public interface Subordinate {

        String getDeviceId();

        Future<?> subscribe(Context context, Consumer<ReceivedCommand> commandReceiver);

        Future<?> respond(Context context, CommandResponse response);

    }

    @FunctionalInterface
    public interface Initiator {

        /**
         * Initiate the next command.
         * <p>
         * Some protocols like HTTP require an action before the command can be sent. The initiator has the
         * responsibility of performing such action. As the action itself can take some time, a future is returned.
         * The outcome of the future signals if and when the command can be sent.
         * <p>
         * The initiator itself is triggered by a periodic timer.
         *
         * @param context The test context.
         * @return A future which indicates that the command is ready to be sent.
         */
        Future<?> initiate(Context context);

    }

    public static abstract class AbstractSubordinate implements Subordinate {

        private final String deviceId;

        protected AbstractSubordinate(final String deviceId) {
            this.deviceId = deviceId;
        }

        @Override
        public String getDeviceId() {
            return this.deviceId;
        }

    }

    public interface Commander extends AutoCloseable {
        Future<?> command(Command command);

        static String fullResponseAddress(final String tenantId, final String replyTo) {
            return COMMAND_RESPONSE.address(tenantId) + "/" + replyTo;
        }
    }

    @FunctionalInterface
    public interface CommanderFactory {

        static CommanderFactory of(final AmqpClient client, final String tenantId) {
            return (context, replyTo, commandResponseConsumer) -> {

                var address = fullResponseAddress(tenantId, replyTo);
                var receiver = client.recvMessagesWithStatus(address, message -> {
                    final Buffer payload;
                    if (message.getBody() instanceof Data) {
                        payload = Buffer.buffer(((Data) message.getBody()).getValue().getArray());
                    } else {
                        payload = null;
                    }

                    var contentType = message.getContentType();
                    var id = ofNullable(message.getCorrelationId()).map(Object::toString).orElse(null);
                    var status = ofNullable(message.getApplicationProperties().getValue().get("status")).map(Object::toString).orElse(null);
                    var receivedDeviceId = ofNullable(message.getApplicationProperties().getValue().get("device_id")).map(Object::toString).orElse(null);

                    log.info("Received - '{}' <- {}", payload, message);
                    commandResponseConsumer.accept(new ReceivedCommandResponse(
                            id,
                            contentType,
                            payload,
                            status,
                            receivedDeviceId
                    ));
                });

                return new Commander() {

                    public Future<?> command(Command command) {
                        var message = Proton.message();
                        message.setSubject(command.getCommand());
                        message.setCorrelationId(command.getReplyId());
                        message.setReplyTo(fullResponseAddress(tenantId, command.getReplyId()));
                        message.setBody(new Data(new Binary(command.getPayload().getBytes())));
                        message.setAddress(COMMAND.address(tenantId) + "/" + command.getDeviceId());
                        return client
                                .sendMessage(context.vertx(), COMMAND.address(tenantId), message)
                                .onSuccess(r -> log.info("Command processed -> {}", r.getRemoteState()))
                                .onFailure(e -> log.info("Command failed", e));
                    }

                    @Override
                    public void close() throws Exception {
                        receiver.close();
                    }
                };
            };
        }

        Commander start(Context context, String replyTo, Consumer<ReceivedCommandResponse> messageConsumer);
    }

    public interface Context {
        SoftAssertions runtimeAssertions();

        Vertx vertx();

        default void runtimeAssert(final Consumer<SoftAssertions> code) {
            code.accept(runtimeAssertions());
        }
    }

    private Vertx vertx;
    private Mode mode = Mode.REQUEST_RESPONSE;
    private String deviceId;
    private Duration delay = Duration.ofSeconds(1);
    private Duration operationDuration = Duration.ofSeconds(1);
    private Subordinate subordinate;
    private CommanderFactory commanderFactory;
    private Initiator initiator = Future::succeededFuture;
    private int amount;
    private double acceptableLoss = 0.0;

    public CommandTester() {
    }

    public CommandTester vertx(final Vertx vertx) {
        this.vertx = vertx;
        return this;
    }

    public CommandTester mode(final Mode mode) {
        this.mode = mode;
        return this;
    }

    public CommandTester targetDevice(final String deviceId) {
        this.deviceId = deviceId;
        return this;
    }

    public CommandTester amount(final int amount) {
        this.amount = amount;
        return this;
    }

    public CommandTester acceptableLoss(final double acceptableLoss) {
        this.acceptableLoss = acceptableLoss;
        return this;
    }

    public CommandTester delay(final Duration delay) {
        this.delay = delay;
        return this;
    }

    public CommandTester operationDuration(final Duration operationDuration) {
        this.operationDuration = operationDuration;
        return this;
    }

    public CommandTester initiator(final Initiator initiator) {
        this.initiator = initiator;
        return this;
    }

    public CommandTester subordinate(final Subordinate subordinate) {
        this.subordinate = subordinate;
        return this;
    }

    public CommandTester commanderFactory(final CommanderFactory commanderFactory) {
        this.commanderFactory = commanderFactory;
        return this;
    }

    public void execute() throws Exception {
        try (Executor executor = new Executor()) {
            executor.execute();
        }
    }

    private class Executor implements AutoCloseable {

        class RequestResponse {
            final Promise<?> responseReceived = Promise.promise();
            final Command command;

            OptionalLong timer = OptionalLong.empty();
            ReceivedCommandResponse response;
            ReceivedCommandResponse invalidResponse;

            RequestResponse(final Command command) {
                this.command = command;
            }

            @Override
            public String toString() {
                return MoreObjects.toStringHelper(this)
                        .add("command", this.command)
                        .add("response", this.response)
                        .add("invalidResponse", this.invalidResponse)
                        .toString();
            }
        }

        private final AtomicReference<RequestResponse> currentRequest = new AtomicReference<>();
        private final AtomicReference<Future<?>> lastResponseFuture = new AtomicReference<>();
        private final String replyId = UUID.randomUUID().toString();
        private final Promise<?> complete = Promise.promise();

        /**
         * Assertions which are accumulated during the runtime of the test execution.
         */
        private final SoftAssertions runtimeAssertions = new SoftAssertions();
        private final Context context;

        private final Mode mode;
        private final String deviceId;
        private final Duration delay;
        private final Duration operationDuration;
        private final int amount;
        private final Initiator initiator;
        private final Duration timeout;
        private final Subordinate subordinate;
        private final CommanderFactory commanderFactory;
        private final List<RequestResponse> result;
        private final Vertx vertx;
        private final Vertx vertxToClose;

        // the index of the current iteration
        private int current;
        // the attempt number for the current iteration
        private int attempt;
        // the timestamp when we must be finished
        private Instant end;

        Executor() {

            this.mode = CommandTester.this.mode != null ? CommandTester.this.mode : Mode.REQUEST_RESPONSE;
            this.delay = Objects.requireNonNull(CommandTester.this.delay);
            this.amount = CommandTester.this.amount;
            this.commanderFactory = Objects.requireNonNull(CommandTester.this.commanderFactory, "CommandFactory must be provided");
            this.subordinate = Objects.requireNonNull(CommandTester.this.subordinate, "Need subordinate implementation");
            this.initiator = CommandTester.this.initiator != null ? CommandTester.this.initiator : x -> Future.succeededFuture();
            this.operationDuration = CommandTester.this.operationDuration;

            // get device ID for testing

            if (CommandTester.this.deviceId == null)
                this.deviceId = this.subordinate.getDeviceId();
            else {
                this.deviceId = CommandTester.this.deviceId;
            }

            // validate amount

            if (this.amount <= 0) {
                throw new IllegalArgumentException("Amount must be a positive integer, was: " + this.amount);
            }

            // calculate timeout - start with base delay
            double timeout = this.delay.toMillis() + operationDuration.toMillis();
            // times the number commands we want to process
            timeout *= this.amount;
            // plus the acceptable loss
            timeout *= (1.0 + CommandTester.this.acceptableLoss);
            this.timeout = Duration.ofMillis((long) timeout);
            log.info("Calculated timeout as: {}", TestUtils.format(this.timeout));

            // create result list

            this.result = new ArrayList<>(this.amount);

            // create vertx last, allocates resources

            if (CommandTester.this.vertx != null) {
                this.vertx = CommandTester.this.vertx;
                this.vertxToClose = null;
            } else {
                this.vertx = Vertx.vertx();
                this.vertxToClose = this.vertx;
            }

            // create the context

            this.context = new Context() {
                @Override
                public SoftAssertions runtimeAssertions() {
                    return Executor.this.runtimeAssertions;
                }

                @Override
                public Vertx vertx() {
                    return Executor.this.vertx;
                }
            };
        }

        @Override
        public void close() {
            if (this.vertxToClose != null) {
                this.vertxToClose.close();
            }
        }

        private void scheduleNext(final Commander commander) {

            this.vertx.getOrCreateContext().runOnContext(v -> {

                // check if we really need to re-schedule another run

                if (this.current >= this.amount) {
                    // we can abort
                    log.info("Mission accomplished, abort...");
                    complete.complete();
                    return;
                } else if (now().isAfter(this.end)) {
                    // we can abort
                    log.info("Timed out, abort...");
                    complete.fail(new TimeoutException("Test timed out after " + TestUtils.format(this.timeout)));
                    return;
                }

                // keep going

                log.info("Scheduling next run...");

                var lastResponseFuture = this.lastResponseFuture.getAndSet(null);
                if (lastResponseFuture == null) {
                    // we use a succeeded future to keep the following code simpler, not requiring an "if"
                    lastResponseFuture = succeededFuture();
                }

                lastResponseFuture.onComplete(c -> {

                    this.vertx.setTimer(this.delay.toMillis(), t -> {
                        log.info("Timer expired, calling initiator...");
                        var next = this.initiator.initiate(this.context);
                        next
                                .onComplete(r -> log.info("Initiator completed: {}", r))
                                .flatMap(x -> nextRun(commander))
                                .onComplete(r -> log.info("Run completed - {}", r))
                                .onComplete(x -> scheduleNext(commander));
                    });

                });

            });

        }

        /**
         * Create the current command, increasing the attempt counter.
         */
        private Command createCommand() {

            this.attempt++;

            var payload = Buffer.buffer(8)
                    .appendInt(this.current)
                    .appendInt(this.attempt);

            return new Command(
                    "TEST",
                    this.mode == Mode.REQUEST_RESPONSE ? this.replyId : null,
                    this.deviceId,
                    MIME_TYPE,
                    payload
            );

        }

        /**
         * Create the response to a received command.
         */
        private Optional<CommandResponse> createResponse(final ReceivedCommand command) {

            if (command.getId() == null) {
                return Optional.empty();
            }

            // we simply pass back what we received

            return Optional.of(new CommandResponse(
                    command.getId(),
                    command.getContentType(),
                    command.getPayload(),
                    200,
                    command.getDeviceId()
            ));

        }

        private Future<?> nextRun(final Commander commander) {

            // do the next run

            var command = createCommand();

            log.info("Sending command: {}", command);

            switch (this.mode) {

                case ONE_WAY:
                    return commander
                            .command(command)
                            .onSuccess(x -> {
                                // sending the command was successful, we are done here
                                this.current++;
                                this.attempt = 0;
                            });

                case REQUEST_RESPONSE:

                    // record for the response

                    var newRequest = new RequestResponse(command);
                    var oldRequest = this.currentRequest.getAndSet(newRequest);
                    if (oldRequest != null) {
                        // no response
                        log.info("Recording expired command: {}", oldRequest.command);
                        this.result.add(oldRequest);
                    }

                    // prepare cancellation, waiting for the response

                    var timer = this.vertx.setTimer(this.operationDuration.toMillis(), x -> {
                        if (newRequest.responseReceived.tryFail(new TimeoutException("Response timed out"))) {
                            if (this.currentRequest.compareAndSet(newRequest, null)) {
                                log.info("Recording cancelled command: {}", newRequest.command);
                                this.result.add(newRequest);
                            }
                            log.info("Cancelled command: {}", command);
                        }
                    });
                    newRequest.timer = OptionalLong.of(timer);

                    // finally send the command

                    return commander
                            .command(command)
                            .flatMap(x -> newRequest.responseReceived.future());

            }

            return Future.failedFuture("Unknown mode: " + this.mode);

        }

        private void receivedResponse(final ReceivedCommandResponse response) {

            log.info("Received response: {}", response);

            if (this.mode == Mode.ONE_WAY) {
                // record as unsolicited response
                log.info("Recording unsolicited response when using one way commands: {}", response);
                var r = new RequestResponse(null);
                r.response = response;
                this.result.add(r);
                return;
            }

            // remove the current pending request

            var oldRequest = this.currentRequest.getAndSet(null);
            if (oldRequest == null) {
                // response without request, record as well
                var p = new RequestResponse(null);
                p.response = response;
                result.add(p);
                log.info("Unsolicited response");
                return;
            }

            this.result.add(oldRequest);

            // cancel timeout, if present

            oldRequest.timer.ifPresent(this.vertx::cancelTimer);

            // ensure the response is valid

            var len = response.getPayload().length();
            if (len != 8) {
                log.info("Unexpected response length: {}", len);
                oldRequest.invalidResponse = response;
                oldRequest.responseReceived.tryFail("Unexpected response length");
                return;
            }
            var idx = response.getPayload().getInt(0);
            var att = response.getPayload().getInt(4);
            log.debug("Index counter: {}, attempt: {}", idx, att);
            if (this.current != idx) {
                log.info("Payload mismatch - expected: {}, actual: {}", this.current, idx);
                oldRequest.invalidResponse = response;
                oldRequest.responseReceived.tryFail("Unexpected payload content");
                return;
            }

            // try to complete (timeout might have been first

            oldRequest.response = response;
            if (oldRequest.responseReceived.tryComplete()) {
                log.info("Accepted command response");
                // we received our response in time, we are done here
                this.current++;
                this.attempt = 0;
            } else {
                log.info("Unable to complete response, got canceled first");
            }

        }

        public void execute() throws Exception {

            this.end = now().plus(this.timeout);

            try (final Commander commander = this.commanderFactory.start(this.context, this.replyId, this::receivedResponse)) {

                // subscribe, then schedule next (first) command

                this.subordinate
                        .subscribe(context, this::handleCommand)
                        .onSuccess(x -> scheduleNext(commander))
                        .onFailure(this.complete::fail);

                // await result, or time out

                await(this.complete.future(), this.timeout.plusSeconds(10));

            }

            assertResult();
        }

        private void handleCommand(ReceivedCommand command) {

            var response = createResponse(command);

            log.info("Responding - command: {}, response: {}", command, response);

            if (response.isPresent()) {
                var newFuture = this.subordinate.respond(this.context, response.get());
                var oldFuture = this.lastResponseFuture.getAndSet(newFuture);
                if (oldFuture != null) {
                    log.warn("Dangling response future detected: {}", oldFuture);
                }
            } else if (this.mode == Mode.REQUEST_RESPONSE) {
                this.runtimeAssertions.fail(
                        "Unable to generate response for received command in request/response mode - receivedCommand: %s",
                        command);
            }

        }

        /**
         * Assert the result after a successful run.
         */
        private void assertResult() {

            // copy, just in case something append in the background

            var result = new ArrayList<>(this.result);

            if (log.isInfoEnabled()) {
                log.info("Result:{} {}", lineSeparator(), result.stream().map(Object::toString).collect(joining(lineSeparator())));
            }

            // start asserting, append to existing runtime assertions

            var softly = this.runtimeAssertions;

            // there must be no unsolicited responses

            softly.assertThat(result)
                    .areNot(unsolicitedResponse);

            switch (this.mode) {

                case REQUEST_RESPONSE:

                    var numMissingResponses = result.stream().filter(p -> p.response == null).count();
                    var numResponses = result.size() - numMissingResponses;
                    var acceptableLoss = (long) (this.amount * CommandTester.this.acceptableLoss);

                    log.info("Result - responses: {}, missing responses: {}", numResponses, numMissingResponses);
                    log.info("   Acceptable loss: {}", acceptableLoss);

                    softly.assertThat(numResponses)
                            .as("exact number of responses")
                            .isEqualTo(this.amount);


                    softly.assertThat(numMissingResponses)
                            .as("Not more than %s%% (%s) of missing responses", CommandTester.this.acceptableLoss * 100.0, acceptableLoss)
                            .isLessThanOrEqualTo(acceptableLoss);

                    break;

                case ONE_WAY:

                    softly.assertThat(result)
                            .as("We must not have any response for one-way tests")
                            .allSatisfy(p -> assertThat(p.response).isNull());

                    break;

            }

            // hard assert gathered assertions errors

            softly.assertAll();

        }

    }

}
