/*
 * Copyright 2016-2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package io.enmasse.config.service.amqp;

import io.enmasse.config.service.model.ObserverKey;
import io.enmasse.config.service.model.ResourceDatabase;
import io.enmasse.config.service.model.Subscriber;
import io.vertx.core.Vertx;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import io.vertx.proton.ProtonMessageHandler;
import org.apache.qpid.proton.amqp.messaging.AmqpValue;
import org.apache.qpid.proton.message.Message;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;

import java.util.Collections;
import java.util.Map;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.*;

@RunWith(VertxUnitRunner.class)
public class AMQPServerTest {
    private Vertx vertx;
    private AMQPServer server;
    private ResourceDatabase database;
    private TestClient client;

    @Before
    public void setup(TestContext context) throws InterruptedException {
        vertx = Vertx.vertx();
        database = mock(ResourceDatabase.class);
        server = new AMQPServer("localhost", 0, Collections.singletonMap("foo", database));
        vertx.deployVerticle(server, context.asyncAssertSuccess());
        int port = waitForPort(server);
        System.out.println("Server running on port " + server.port());
        client = new TestClient(vertx, "localhost", port);
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
    public void teardown() throws InterruptedException {
        client.close();
        server.stop();
        vertx.close();
    }

    @Test
    public void testSubscribe(TestContext context) throws Exception {
        ProtonMessageHandler msgHandler = mock(ProtonMessageHandler.class);
        client.subscribe("foo", null, msgHandler);

        ArgumentCaptor<ObserverKey> keyCapture = ArgumentCaptor.forClass(ObserverKey.class);
        ArgumentCaptor<Subscriber> subCapture = ArgumentCaptor.forClass(Subscriber.class);
        verify(database, timeout(10000)).subscribe(keyCapture.capture(), subCapture.capture());

        ObserverKey key = keyCapture.getValue();
        Map<String, String> filter = key.getLabelFilter();
        assertThat(filter.size(), is(1));
        assertTrue(filter.containsKey("my"));
        assertThat(filter.get("my"), is("label"));

        Map<String, String> afilter = key.getAnnotationFilter();
        assertThat(afilter.size(), is(1));
        assertTrue(afilter.containsKey("my"));
        assertThat(afilter.get("my"), is("annotation"));

        Subscriber sub = subCapture.getValue();
        Message testMessage = Message.Factory.create();
        testMessage.setBody(new AmqpValue("test1"));
        sub.resourcesUpdated(testMessage);

        ArgumentCaptor<Message> msgCapture = ArgumentCaptor.forClass(Message.class);
        verify(msgHandler, timeout(10000)).handle(any(), msgCapture.capture());
        String value = (String) ((AmqpValue)msgCapture.getValue().getBody()).getValue();
        assertThat(value, is("test1"));
    }

    @Test
    public void testSubscribeWithBadKey(TestContext context) throws InterruptedException {
        ProtonMessageHandler msgHandler = mock(ProtonMessageHandler.class);
        Async closed = context.async();
        client.subscribe("nosuchaddress", closed, msgHandler);
   }
}

