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

import io.vertx.core.Vertx;
import io.vertx.proton.ProtonClient;
import org.junit.Test;

public class TopicMigratorTest {
    private Endpoint from = new Endpoint("127.0.0.1", 12345);
    private Endpoint to = new Endpoint("127.0.0.1", 12346);
    private TestBroker fromServer;
    private TestBroker toServer;
    private Vertx vertx = Vertx.vertx();

    @Test
    public void testMigrator() throws Exception {
        fromServer = new TestBroker(from.hostName(), from.port(), "mytopic");
        toServer = new TestBroker(to.hostName(), to.port(), "mytopic");
        fromServer.start();
        toServer.start();

        Thread.sleep(5000);
        System.out.println("Started brokers");
        BrokerManager fromMgr = new BrokerManager(from);
        TestSubscriber subscriber = new TestSubscriber(ProtonClient.create(vertx));
        System.out.println("Attempting to subscribe");
        subscriber.subscribe(from, "jms.topic.mytopic", to);

        Thread.sleep(5000);
        System.out.println("Starting migrator");
        TopicMigrator migrator = new TopicMigrator(fromMgr, from, vertx);
        migrator.migrateTo(to, "jms.topic.mytopic");

        Thread.sleep(10000);
    }
}
