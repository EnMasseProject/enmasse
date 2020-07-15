/*
 * Copyright 2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package io.enmasse.systemtest.iot.isolated.mqtt;

import java.nio.charset.StandardCharsets;

import io.enmasse.systemtest.iot.IoTTestSession.Adapter;
import io.enmasse.systemtest.iot.IoTTestSession.ProjectInstance.Device;
import io.enmasse.systemtest.iot.MessageSendTester.Sender;
import io.enmasse.systemtest.iot.MessageSendTester.Type;
import io.enmasse.systemtest.iot.isolated.AbstractMaxPayloadSizeTest;

public class MaxPayloadSizeTest extends AbstractMaxPayloadSizeTest {

    @Override
    protected int reducePayloadBy(final Type type) {
        // reduce by: 2 (length) + x (topic name) bytes + 2 (QoS 1 msg id)
        return 2 + type.type().address().getBytes(StandardCharsets.UTF_8).length + 2;
    }

    @Override
    protected Adapter adapter() {
        return Adapter.MQTT;
    }

    @SuppressWarnings("resource")
    @Override
    protected Sender sender(Device device) throws Exception {
        return cleanup(device.createMqttAdapterClient())::sendQos1;
    }

}
