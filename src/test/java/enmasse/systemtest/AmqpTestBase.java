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
import enmasse.systemtest.amqp.TerminusFactory;
import io.vertx.proton.ProtonClientOptions;
import io.vertx.proton.ProtonQoS;
import org.junit.After;
import org.junit.Before;

import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

public abstract class AmqpTestBase {
    protected AddressApiClient addressApiClient;
    protected Environment environment = new Environment();
    protected OpenShift openShift;
    private final List<AmqpClient> clients = new ArrayList<>();

    protected abstract String getInstanceName();

    @Before
    public void setup() throws Exception {
        clients.clear();
        openShift = new OpenShift(environment, environment.isMultitenant() ? getInstanceName().toLowerCase() : environment.namespace());
        addressApiClient = new AddressApiClient(openShift.getRestEndpoint(), environment.isMultitenant());
        addressApiClient.deployInstance(getInstanceName().toLowerCase());
    }

    @After
    public void teardown() throws Exception {
        cleanup();
        addressApiClient.close();
        clients.clear();
    }

    private void cleanup() throws Exception {
        deploy();
        for (AmqpClient client : clients) {
            client.close();
        }
    }

    protected void deploy(Destination ... destinations) throws Exception {
        TimeoutBudget budget = new TimeoutBudget(5, TimeUnit.MINUTES);
        TestUtils.deploy(addressApiClient, openShift, budget, getInstanceName().toLowerCase(), destinations);
    }

    protected void scale(Destination destination, int numReplicas) throws Exception {
        TimeoutBudget budget = new TimeoutBudget(5, TimeUnit.MINUTES);
        TestUtils.setReplicas(openShift, destination, numReplicas, budget);
        if (destination.isStoreAndForward() && !destination.isMulticast()) {
            TestUtils.waitForAddress(openShift, destination.getAddress(), budget);
        }
    }

    protected AmqpClient createQueueClient() throws UnknownHostException {
        return createClient(new QueueTerminusFactory(), ProtonQoS.AT_LEAST_ONCE);
    }

    protected AmqpClient createTopicClient() throws UnknownHostException {
        return createClient(new TopicTerminusFactory(), ProtonQoS.AT_LEAST_ONCE);
    }

    protected AmqpClient createDurableTopicClient() throws UnknownHostException {
        return createClient(new DurableTopicTerminusFactory(), ProtonQoS.AT_LEAST_ONCE);
    }

    protected AmqpClient createBroadcastClient() throws UnknownHostException {
        return createClient(new QueueTerminusFactory(), ProtonQoS.AT_MOST_ONCE);
    }

    protected AmqpClient createClient(TerminusFactory terminusFactory, ProtonQoS qos) throws UnknownHostException {
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
        AmqpClient client = new AmqpClient(endpoint, terminusFactory, protonOptions, qos);
        clients.add(client);
        return client;
    }
}
