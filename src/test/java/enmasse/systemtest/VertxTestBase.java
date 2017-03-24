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

import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.net.PemTrustOptions;
import io.vertx.proton.ProtonClient;
import io.vertx.proton.ProtonClientOptions;
import io.vertx.proton.ProtonConnection;
import io.vertx.proton.ProtonSender;
import org.junit.After;
import org.junit.Before;

import java.net.UnknownHostException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public abstract class VertxTestBase {
    protected Vertx vertx;
    protected AddressApiClient addressApiClient;
    protected Environment environment = new Environment();
    protected OpenShift openShift;

    protected abstract String getInstanceName();

    @Before
    public void setup() throws Exception {
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
    }

    private void cleanup() throws Exception {
        deploy();
    }

    protected void deploy(Destination ... destinations) throws Exception {
        TimeoutBudget budget = new TimeoutBudget(5, TimeUnit.MINUTES);
        TestUtils.deploy(addressApiClient, openShift, budget, getInstanceName().toLowerCase(), destinations);
        for (Destination destination : destinations) {
            waitUntilReady(destination.getAddress(), budget);
        }
    }

    protected void scale(Destination destination, int numReplicas) throws Exception {
        TimeoutBudget budget = new TimeoutBudget(5, TimeUnit.MINUTES);
        TestUtils.setReplicas(openShift, destination, numReplicas, budget);
        if (destination.isStoreAndForward() && !destination.isMulticast()) {
            TestUtils.waitForAddress(openShift, destination.getAddress(), budget);
        }
        waitUntilReady(destination.getAddress(), budget);
    }

    protected EnMasseClient createQueueClient() throws UnknownHostException {
        return createClient(new QueueTerminusFactory());
    }

    protected EnMasseClient createTopicClient() throws UnknownHostException {
        return createClient(new TopicTerminusFactory());
    }

    protected EnMasseClient createDurableTopicClient() throws UnknownHostException {
        return createClient(new DurableTopicTerminusFactory());
    }

    protected EnMasseClient createClient(TerminusFactory terminusFactory) throws UnknownHostException {
        ProtonClientOptions options = new ProtonClientOptions();
        if (environment.useTLS()) {
            options.setSsl(true);
            options.setHostnameVerificationAlgorithm("");
            options.setPemTrustOptions(new PemTrustOptions().addCertValue(Buffer.buffer(environment.messagingCert())));
          //  options.setSniServerName(openShift.getRouteHost());
            options.setTrustAll(true);
            return createClient(terminusFactory, options, openShift.getSecureEndpoint());
        } else {
            return createClient(terminusFactory, options, openShift.getInsecureEndpoint());
        }
    }

    protected EnMasseClient createClient(TerminusFactory terminusFactory, ProtonClientOptions options, Endpoint endpoint) {
        return new EnMasseClient(vertx, endpoint, terminusFactory, options);
    }

    public void waitUntilReady(String address, TimeoutBudget budget) throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);
        connectToEndpoint(address, latch);
        if (!latch.await(budget.timeLeft(), TimeUnit.MILLISECONDS)) {
            throw new IllegalStateException("Timed out while waiting for addresses to get configured");
        }
    }

    private void connectToEndpoint(String address, CountDownLatch latch) {
        ProtonClient protonClient = ProtonClient.create(vertx);
        Endpoint endpoint = openShift.getInsecureEndpoint();
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
