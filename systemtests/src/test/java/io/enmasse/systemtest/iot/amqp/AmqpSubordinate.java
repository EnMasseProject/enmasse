/*
 * Copyright 2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package io.enmasse.systemtest.iot.amqp;

import io.enmasse.systemtest.iot.AmqpAdapterClient;
import io.enmasse.systemtest.iot.CommandTester.AbstractSubordinate;
import io.enmasse.systemtest.iot.CommandTester.CommandResponse;
import io.enmasse.systemtest.iot.CommandTester.Context;
import io.enmasse.systemtest.iot.CommandTester.ReceivedCommand;
import io.enmasse.systemtest.iot.IoTTestSession;
import io.vertx.core.Future;
import io.vertx.core.buffer.Buffer;
import org.apache.qpid.proton.amqp.Binary;
import org.apache.qpid.proton.amqp.messaging.ApplicationProperties;
import org.apache.qpid.proton.amqp.messaging.Data;
import org.apache.qpid.proton.message.Message;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Optional;
import java.util.function.Consumer;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Optional.ofNullable;

public class AmqpSubordinate extends AbstractSubordinate implements AutoCloseable {

    private static final Logger log = LoggerFactory.getLogger(AmqpSubordinate.class);

    private final AmqpAdapterClient client;

    AmqpSubordinate(final IoTTestSession.ProjectInstance.Device device) throws Exception {
        super(device.getDeviceId());

        this.client = device.createAmqpAdapterClient();
    }

    @Override
    public void close() throws Exception {
        this.client.close();
    }

    Optional<ReceivedCommand> processCommandMessage(final Message message) {

        log.info("Processing command message: {}", message);

        var command = message.getSubject();
        var deviceId = ofNullable(message.getApplicationProperties())
                .map(ApplicationProperties::getValue)
                .map(v -> v.get("device_id"))
                .map(Object::toString)
                .orElse(null);
        var contentType = message.getContentType();

        final Buffer payload;
        if (message.getBody() instanceof Data) {
            payload = Buffer.buffer(((Data) message.getBody()).getValue().getArray());
        } else {
            payload = null;
        }

        var correlationId = message.getCorrelationId();
        var replyTo = message.getReplyTo();

        final String id;
        if (correlationId != null && replyTo != null) {
            if (!(correlationId instanceof String)) {
                handleIllegalCommand(message, String.format("Correlation ID must be of type String: was %s", correlationId.getClass()));
                return Optional.empty();
            }
            id = new StringBuilder()
                    .append(URLEncoder.encode(((String) correlationId), UTF_8))
                    .append(";")
                    .append(URLEncoder.encode(replyTo, UTF_8))
                    .toString()
            ;
        } else {
            id = null;
        }

        return Optional.of(new ReceivedCommand(
                id,
                command,
                deviceId,
                contentType,
                payload
        ));
    }

    private void handleIllegalCommand(final Message message, final String reason) {
        log.warn("Received illegal command: {} - message: {}", reason, message);
    }

    @Override
    public Future<?> subscribe(final Context context, final Consumer<ReceivedCommand> commandReceiver) {

        return this.client.subscribe("command", message -> {
            processCommandMessage(message).ifPresent(commandReceiver);
        });

    }

    @Override
    public Future<?> respond(final Context context, final CommandResponse response) {

        var id = response.getId();
        if (id == null) {
            return Future.failedFuture("Unable to send response without id");
        }

        var toks = id.split(";");
        if (toks.length != 2) {
            return Future.failedFuture("Wrong format of id: " + id);
        }

        var correlationId = URLDecoder.decode(toks[0], UTF_8);
        var replyTo = URLDecoder.decode(toks[1], UTF_8);

        var message = Message.Factory.create();

        message.setAddress(replyTo);
        message.setCorrelationId(correlationId);

        message.setApplicationProperties(new ApplicationProperties(new HashMap<>()));
        message.getApplicationProperties().getValue().put("status", response.getStatus());

        if (response.getPayload() != null) {
            message.setBody(new Data(new Binary(response.getPayload().getBytes())));
        }

        return this.client.send(message);
    }
}
