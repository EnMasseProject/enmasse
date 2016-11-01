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
package enmasse.smoketest;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.openshift.restclient.ResourceKind;
import com.openshift.restclient.model.IPod;
import enmasse.amqp.SyncRequestClient;
import io.vertx.core.Vertx;
import io.vertx.proton.ProtonClient;
import io.vertx.proton.ProtonConnection;
import io.vertx.proton.ProtonSender;
import org.apache.qpid.proton.amqp.messaging.AmqpValue;
import org.apache.qpid.proton.message.Message;
import org.junit.After;
import org.junit.Before;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static enmasse.smoketest.Environment.endpoint;
import static enmasse.smoketest.Environment.namespace;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class VertxTestBase {
    private static final ObjectMapper mapper = new ObjectMapper();
    protected Vertx vertx;
    protected ProtonClient protonClient;

    @Before
    public void setup() {
        vertx = Vertx.vertx();
        protonClient = ProtonClient.create(vertx);
    }

    @After
    public void teardown() throws InterruptedException {
        vertx.close();
    }

    protected EnMasseClient createQueueClient() {
        return createClient(new QueueTerminusFactory());
    }

    protected EnMasseClient createTopicClient() {
        return createClient(new TopicTerminusFactory());
    }

    protected EnMasseClient createDurableTopicClient() {
        return createClient(new DurableTopicTerminusFactory());
    }

    protected EnMasseClient createClient(TerminusFactory terminusFactory) {
        return new EnMasseClient(protonClient, endpoint, terminusFactory);
    }

    protected void waitForAddress(String address, long timeout , TimeUnit timeUnit) throws Exception {
        ArrayNode root = mapper.createArrayNode();
        ObjectNode data = root.addObject();
        data.put("name", address);
        data.put("store-and-forward", true);
        data.put("multicast", false);
        String json = mapper.writeValueAsString(root);
        Message message = Message.Factory.create();
        message.setAddress("health-check");
        message.setSubject("health-check");
        message.setBody(new AmqpValue(json));

        int numConfigured = 0;
        TimeoutBudget budget = new TimeoutBudget(timeout, timeUnit);
        List<IPod> agents = Environment.client.list(ResourceKind.POD, namespace, Collections.singletonMap("name", "ragent"));

        while (budget.timeLeft() >= 0 && numConfigured < agents.size()) {
            numConfigured = 0;
            for (IPod pod : agents) {
                SyncRequestClient client = new SyncRequestClient(pod.getIP(), pod.getContainerPorts().iterator().next().getContainerPort());
                Message response = client.request(message, budget.timeLeft(), TimeUnit.MILLISECONDS);
                AmqpValue value = (AmqpValue) response.getBody();
                if ((Boolean) value.getValue() == true) {
                    numConfigured++;
                }
            }
            Thread.sleep(1000);
        }
        assertEquals("Timed out while waiting for EnMasse to be configured", numConfigured, agents.size());
    }

    protected void waitUntilReady(String address, long timeout, TimeUnit timeUnit) throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);
        connectToEndpoint(address, latch);
        assertTrue(latch.await(timeout, timeUnit));
    }

    private void connectToEndpoint(String address, CountDownLatch latch) {
        protonClient.connect(endpoint.getHost(), endpoint.getPort(), event -> {
            if (event.succeeded()) {
                ProtonConnection connection = event.result();
                connection.openHandler(openResult -> {
                    if (openResult.succeeded()) {
                        ProtonSender sender = connection.createSender(address);
                        sender.openHandler(remoteOpenEvent -> {
                            sender.close();
                            connection.close();
                            if (remoteOpenEvent.succeeded()) {
                                latch.countDown();
                            } else {
                                scheduleReconnect(address, latch);
                            }
                        });
                        sender.open();
                    } else {
                        connection.close();
                        scheduleReconnect(address, latch);
                    }
                });
                connection.open();
            } else {
                scheduleReconnect(address, latch);
            }
        });
    }

    private void scheduleReconnect(String address, CountDownLatch latch) {
        vertx.setTimer(2000, timerId -> connectToEndpoint(address, latch));
    }
}
