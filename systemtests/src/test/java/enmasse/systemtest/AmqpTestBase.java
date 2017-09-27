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
package enmasse.systemtest;

import enmasse.systemtest.amqp.AmqpClient;
import enmasse.systemtest.amqp.AmqpConnectOptions;
import enmasse.systemtest.amqp.DurableTopicTerminusFactory;
import enmasse.systemtest.amqp.QueueTerminusFactory;
import enmasse.systemtest.amqp.TerminusFactory;
import enmasse.systemtest.amqp.TopicTerminusFactory;
import io.vertx.proton.ProtonClientOptions;
import io.vertx.proton.ProtonQoS;
import org.junit.After;
import org.junit.Before;

import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;

public abstract class AmqpTestBase extends TestBase {

    private final List<AmqpClient> clients = new ArrayList<>();

    @Before
    public void setupAmqpTest() throws Exception {
        clients.clear();
    }

    @After
    public void teardownAmqpTest() throws Exception {
        for (AmqpClient client : clients) {
            client.close();
        }
        clients.clear();
    }

    protected AmqpClient createQueueClient() throws UnknownHostException, InterruptedException {
        return createClient(new QueueTerminusFactory(), ProtonQoS.AT_LEAST_ONCE);
    }

    protected AmqpClient createTopicClient() throws UnknownHostException, InterruptedException {
        return createClient(new TopicTerminusFactory(), ProtonQoS.AT_LEAST_ONCE);
    }

    protected AmqpClient createDurableTopicClient() throws UnknownHostException, InterruptedException {
        return createClient(new DurableTopicTerminusFactory(), ProtonQoS.AT_LEAST_ONCE);
    }

    protected AmqpClient createBroadcastClient() throws UnknownHostException, InterruptedException {
        return createClient(new QueueTerminusFactory(), ProtonQoS.AT_MOST_ONCE);
    }

    protected AmqpClient createClient(TerminusFactory terminusFactory, ProtonQoS qos) throws UnknownHostException, InterruptedException {
        if (environment.useTLS()) {
            Endpoint messagingEndpoint = openShift.getRouteEndpoint("messaging");
            Endpoint clientEndpoint;
            ProtonClientOptions clientOptions = new ProtonClientOptions();
            clientOptions.setSsl(true);
            clientOptions.setTrustAll(true);
            clientOptions.setHostnameVerificationAlgorithm("");

            if (TestUtils.resolvable(messagingEndpoint)) {
                clientEndpoint = messagingEndpoint;
            } else {
                clientEndpoint = new Endpoint("localhost", 443);
                clientOptions.setSniServerName(messagingEndpoint.getHost());
            }
            Logging.log.info("External endpoint: " + clientEndpoint + ", internal: " + messagingEndpoint);

            return createClient(terminusFactory, clientEndpoint, clientOptions, qos);
        } else {
            return createClient(terminusFactory, openShift.getInsecureEndpoint(), qos);
        }
    }

    protected AmqpClient createClient(TerminusFactory terminusFactory, Endpoint endpoint, ProtonQoS qos) {
        return createClient(terminusFactory, endpoint, new ProtonClientOptions(), qos);
    }

    protected AmqpClient createClient(TerminusFactory terminusFactory, Endpoint endpoint, ProtonClientOptions protonOptions, ProtonQoS qos) {
        AmqpConnectOptions connectOptions = new AmqpConnectOptions()
                .setTerminusFactory(terminusFactory)
                .setEndpoint(endpoint)
                .setProtonClientOptions(protonOptions)
                .setQoS(qos)
                .setUsername(username)
                .setPassword(password);
        return createClient(connectOptions);
    }

    protected AmqpClient createClient(AmqpConnectOptions connectOptions) {
        AmqpClient client = new AmqpClient(connectOptions);
        clients.add(client);
        return client;
    }
}
