/*
 * Copyright 2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package io.enmasse.systemtest.iot.mqtt;

import io.enmasse.iot.utils.MoreFutures;
import io.enmasse.systemtest.iot.CommandTester.AbstractSubordinate;
import io.enmasse.systemtest.iot.CommandTester.CommandResponse;
import io.enmasse.systemtest.iot.CommandTester.Context;
import io.enmasse.systemtest.iot.CommandTester.ReceivedCommand;
import io.enmasse.systemtest.iot.IoTTestSession;
import io.enmasse.systemtest.iot.MqttAdapterClient;
import io.vertx.core.Future;
import io.vertx.core.buffer.Buffer;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;
import java.util.function.Consumer;

public class MqttSubordinate extends AbstractSubordinate implements AutoCloseable {

    private static final Logger log = LoggerFactory.getLogger(MqttSubordinate.class);

    private final MqttAdapterClient client;

    MqttSubordinate(final IoTTestSession.ProjectInstance.Device device) throws Exception {
        super(device.getDeviceId());

        this.client = device.createMqttAdapterClient();
    }

    @Override
    public void close() throws Exception {
        this.client.close();
    }

    Optional<ReceivedCommand> processCommandMessage(final String topic, final MqttMessage message) {

        var toks = topic.split("/", 6);
        if (toks.length != 6) {
            handleIllegalCommand(topic, message, String.format("Command topic has '%s' segments, expected: 6", toks.length));
            return Optional.empty();
        }

        var deviceId = toks[2];
        var id = toks[4];
        var command = toks[5];
        var payload = message.getPayload();

        return Optional.of(new ReceivedCommand(
                id,
                command,
                deviceId,
                "application/octet-stream",
                Buffer.buffer(payload)
        ));

    }

    private void handleIllegalCommand(final String topic, final MqttMessage message, final String reason) {
        log.warn("Received illegal command: {} - topic: {}, message: {}", reason, topic, message);
    }

    @Override
    public Future<?> subscribe(final Context context, final Consumer<ReceivedCommand> commandReceiver) {

        var result = this.client.subscribe("command///req/#", 1, (topic, message) -> {
            processCommandMessage(topic, message).ifPresent(commandReceiver);
        });

        return MoreFutures.map(result);

    }

    @Override
    public Future<?> respond(final Context context, final CommandResponse response) {

        var address = String.format("command///res/%s/%s", response.getId(), response.getStatus());
        var result = this.client
                .sendAsync(1, address, response.getPayload());

        return MoreFutures.map(result);

    }
}
