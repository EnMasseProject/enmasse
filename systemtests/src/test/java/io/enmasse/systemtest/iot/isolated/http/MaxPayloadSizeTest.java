/*
 * Copyright 2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package io.enmasse.systemtest.iot.isolated.http;

import io.enmasse.systemtest.iot.IoTTestSession.Adapter;
import io.enmasse.systemtest.iot.IoTTestSession.Device;
import io.enmasse.systemtest.iot.MessageSendTester.Sender;
import io.enmasse.systemtest.iot.isolated.AbstractMaxPayloadSizeTest;
import org.junit.jupiter.api.Tag;

import static io.enmasse.systemtest.framework.TestTag.IOT;
import static io.enmasse.systemtest.iot.HttpAdapterClient.ResponseException.statusCode;

@Tag(IOT)
public class MaxPayloadSizeTest extends AbstractMaxPayloadSizeTest {

    @Override
    protected Adapter adapter() {
        return Adapter.HTTP;
    }

    @SuppressWarnings("resource")
    @Override
    protected Sender sender(Device device) throws Exception {
        return cleanup(device
                .createHttpAdapterClient()
                .suppressExceptions(statusCode(413)))::send;
    }

}
