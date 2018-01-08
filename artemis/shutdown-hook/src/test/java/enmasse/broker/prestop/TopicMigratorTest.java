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

package enmasse.broker.prestop;

import enmasse.discovery.Endpoint;
import enmasse.discovery.Host;
import io.vertx.core.Vertx;
import io.vertx.proton.ProtonClientOptions;
import org.apache.qpid.proton.amqp.messaging.AmqpValue;
import org.apache.qpid.proton.message.Message;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.concurrent.TimeUnit;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

public class TopicMigratorTest {
    private Host from = TestUtil.createHost("127.0.0.1", 43210);
    private Host to = TestUtil.createHost("127.0.0.1", 43211);
    private TestBroker fromServer;
    private TestBroker toServer;
    private TestSubscriber subscriber;
    private TestPublisher publisher;

    @Before
    public void setup() throws Exception {
        subscriber = new TestSubscriber();
        publisher = new TestPublisher();
        fromServer = new TestBroker(from.amqpEndpoint(), Arrays.asList("mytopic"), true);
        toServer = new TestBroker(to.amqpEndpoint(), Arrays.asList("mytopic"), true);
        fromServer.start();
        toServer.start();
        Thread.sleep(10000);
    }

    @After
    public void teardown() throws Exception {
        subscriber.close();
        publisher.close();
        fromServer.stop();
        toServer.stop();
    }

    @Test
    @Ignore
    public void testMigrator() throws Exception {
        System.out.println("Attempting to subscribe");
        subscriber.subscribe(from.amqpEndpoint(), "mytopic");
        subscriber.unsubscribe();

        System.out.println("Publishing message");
        publisher.publish(from.amqpEndpoint(), "mytopic", "hello, world");

        TopicMigrator migrator = new TopicMigrator(Vertx.vertx(), from, new Endpoint("messaging.example.com", 5672), new ArtemisBrokerFactory(20_000), new ProtonClientOptions());

        System.out.println("Starting migrator");
        migrator.migrate(Collections.singleton(to));
        fromServer.assertShutdown(1, TimeUnit.MINUTES);

        subscriber.subscribe(to.amqpEndpoint(), "mytopic");
        Message message = subscriber.receiveMessage(1, TimeUnit.MINUTES);
        assertThat(((AmqpValue)message.getBody()).getValue(), is("hello, world"));
    }
}
