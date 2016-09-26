/*
 * Copyright 2016 Red Hat Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package enmasse.config.bridge.amqp;

import enmasse.config.bridge.amqp.subscription.AddressConfigCodec;
import enmasse.config.bridge.model.ConfigDatabase;
import enmasse.config.bridge.model.ConfigSubscriber;
import io.vertx.proton.ProtonMessageHandler;
import org.apache.qpid.proton.amqp.messaging.AmqpValue;
import org.apache.qpid.proton.message.Message;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import java.util.Collections;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.*;

public class AMQPServerTest {
    private AMQPServer server;
    private ConfigDatabase database;
    private TestClient client;

    @Before
    public void setup() throws InterruptedException {
        database = mock(ConfigDatabase.class);
        when(database.subscribe(any(), any())).thenReturn(true);
        server = new AMQPServer("localhost", 0, database);
        server.run();
        int port = waitForPort(server);
        System.out.println("Server running on port " + server.port());
        client = new TestClient("localhost", port);
    }

    private int waitForPort(AMQPServer server) throws InterruptedException {
        int port = server.port();
        while (port == 0) {
            Thread.sleep(100);
            port = server.port();
        }
        return port;
    }

    @After
    public void teardown() {
        client.close();
        server.close();
    }

    @Test
    public void testSubscribe() {
        ProtonMessageHandler msgHandler = mock(ProtonMessageHandler.class);
        client.subscribe("foo", msgHandler);

        ArgumentCaptor<ConfigSubscriber> subCapture = ArgumentCaptor.forClass(ConfigSubscriber.class);
        verify(database, timeout(10000)).subscribe(anyString(), subCapture.capture());

        ConfigSubscriber sub = subCapture.getValue();
        sub.configUpdated(Collections.singletonList(AddressConfigCodec.encodeConfig("myqueue", true, false)));

        ArgumentCaptor<Message> msgCapture = ArgumentCaptor.forClass(Message.class);
        verify(msgHandler, timeout(10000)).handle(any(), msgCapture.capture());
        String value = (String) ((AmqpValue)msgCapture.getValue().getBody()).getValue();
        assertThat(value, is("{\"myqueue\":{\"store_and_forward\":true,\"multicast\":false}}"));
    }
}

