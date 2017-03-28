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
import enmasse.systemtest.amqp.SslOptions;
import enmasse.systemtest.amqp.TerminusFactory;
import io.vertx.core.Vertx;
import org.junit.After;
import org.junit.Before;

import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

public abstract class AmqpTestBase {
    protected Vertx vertx;
    protected AddressApiClient addressApiClient;
    protected Environment environment = new Environment();
    protected OpenShift openShift;
    private final List<AmqpClient> clients = new ArrayList<>();

    protected abstract String getInstanceName();

    @Before
    public void setup() throws Exception {
        clients.clear();
        vertx = Vertx.vertx();
        openShift = new OpenShift(environment, environment.isMultitenant() ? getInstanceName().toLowerCase() : environment.namespace());
        addressApiClient = new AddressApiClient(vertx, openShift.getRestEndpoint(), environment.isMultitenant());
        addressApiClient.deployInstance(getInstanceName().toLowerCase());
    }

    @After
    public void teardown() throws Exception {
        cleanup();
        addressApiClient.close();
        vertx.close();
        for (AmqpClient client : clients) {
            client.close();
        }
        clients.clear();
    }

    private void cleanup() throws Exception {
        deploy();
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
        return createClient(new QueueTerminusFactory());
    }

    protected AmqpClient createTopicClient() throws UnknownHostException {
        return createClient(new TopicTerminusFactory());
    }

    protected AmqpClient createDurableTopicClient() throws UnknownHostException {
        return createClient(new DurableTopicTerminusFactory());
    }

    protected AmqpClient createClient(TerminusFactory terminusFactory) throws UnknownHostException {
        /*
        if (environment.useTLS()) {
            options.setSsl(true);
            options.setHostnameVerificationAlgorithm("");
            options.setPemTrustOptions(new PemTrustOptions().addCertValue(Buffer.buffer(environment.messagingCert())));
          //  options.setSniServerName(openShift.getRouteHost());
            options.setTrustAll(true);
            return createClient(terminusFactory, options, openShift.getSecureEndpoint());
        } else { */
            return createClient(terminusFactory, openShift.getInsecureEndpoint(), Optional.empty());
       // }
    }

    protected AmqpClient createClient(TerminusFactory terminusFactory, Endpoint endpoint) {
        return createClient(terminusFactory, endpoint, Optional.empty());
    }

    protected AmqpClient createClient(TerminusFactory terminusFactory, Endpoint endpoint, Optional<SslOptions> sslOptions) {
        AmqpClient client = new AmqpClient(endpoint, terminusFactory, sslOptions);
        clients.add(client);
        return client;
    }
}
