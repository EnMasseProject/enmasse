/*
 * Copyright 2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package io.enmasse.systemtest.iot.http;

import io.enmasse.systemtest.amqp.ReceiverStatus;
import io.enmasse.systemtest.iot.CommandTester;
import io.enmasse.systemtest.iot.CommandTester.AbstractSubordinate;
import io.enmasse.systemtest.iot.CommandTester.ReceivedCommand;
import io.enmasse.systemtest.iot.HttpAdapterClient;
import io.enmasse.systemtest.iot.IoTTestSession;
import io.enmasse.systemtest.iot.IoTTestSession.ProjectInstance.Device;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import org.apache.qpid.proton.message.Message;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

import static io.enmasse.systemtest.iot.MessageType.COMMAND_RESPONSE;
import static io.enmasse.systemtest.iot.MessageType.TELEMETRY;

public class HttpSubordinate extends AbstractSubordinate implements  AutoCloseable  {

    private static final Logger log = LoggerFactory.getLogger(HttpSubordinate.class);

    private final AtomicReference<Promise<?>> nextCommand = new AtomicReference<>();

    private final Duration ttd = Duration.ofSeconds(10);

    private final Vertx vertx;
    private final ReceiverStatus receiver;
    private final HttpAdapterClient client;

    private Consumer<ReceivedCommand> commandReceiver;

    HttpSubordinate(final IoTTestSession session, final Device device) throws Exception {
        super(device.getDeviceId());

        this.vertx = session.getVertx();
        this.receiver = session
                .getConsumerClient()
                .recvMessagesWithStatus(
                        TELEMETRY.address(session.getTenantId()),
                        this::receivedMessage);
        this.client = device.createHttpAdapterClient();
    }

    private void receivedMessage(final Message message) {
        // received a telemetry message ...
        var f = this.nextCommand.getAndSet(null);
        // ... get current command initiator
        if (f != null) {
            // ... initiate, this will trigger sending the command
            f.tryComplete();
        }
    }

    @Override
    public void close() throws Exception {
        this.receiver.close();
    }

    public Future<?> initiate() {
        var p = Promise.promise();

        this.vertx.setTimer(
                this.ttd.toMillis(), x -> {
                    p.tryFail("Failed to wait for inbound telemetry");
                });

        this.client.sendAsync(TELEMETRY, null, ttd.plusSeconds(2), request -> {
            request.putHeader("hono-ttd", Long.toString(ttd.toSeconds()));
        })
                .onSuccess(response -> {

                    log.info("Response - headers: {}", response.headers());

                    var contentType = response.getHeader("content-type");
                    var command = response.getHeader("hono-command");
                    var id = response.getHeader("hono-cmd-req-id");
                    var deviceId = response.getHeader("hono-cmd-target-device");
                    var payload = response.bodyAsBuffer();

                    this.commandReceiver.accept(new ReceivedCommand(id, command, deviceId, contentType, payload));

                    // FIXME: assert response.statusCode()
                });

        this.nextCommand.set(p);
        return p.future();
    }

    @Override
    public Future<?> subscribe(final Consumer<ReceivedCommand> commandReceiver) {
        this.commandReceiver = commandReceiver;
        // we do not subscribe, but trigger via the initiator
        return Future.succeededFuture();
    }

    @Override
    public void respond(final CommandTester.CommandResponse response) {

        this.client.sendAsync(
                COMMAND_RESPONSE,
                "/" + response.getId(),
                response.getPayload(),
                Duration.ofSeconds(5),
                request -> {
                    request.putHeader("hono-cmd-status", Integer.toString(response.getStatus()));
                });

    }
}
