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

package io.enmasse.systemtest;

import io.enmasse.systemtest.amqp.AmqpClient;
import io.enmasse.systemtest.amqp.AmqpClientFactory;
import io.enmasse.systemtest.mqtt.MqttClient;
import io.enmasse.systemtest.mqtt.MqttClientFactory;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.json.JsonObject;
import org.apache.qpid.proton.message.Message;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.junit.After;
import org.junit.Before;

import java.io.File;
import java.io.UnsupportedEncodingException;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * Base class for all tests
 */
public abstract class TestBase extends SystemTestRunListener {

    protected static final Environment environment = new Environment();
    protected static final AddressSpace defaultAddressSpace = environment.isMultitenant() ? new AddressSpace("testspace", "testspace")
            : new AddressSpace("default", environment.namespace());

    protected static final OpenShift openShift = new OpenShift(environment, environment.namespace(), defaultAddressSpace.getNamespace());
    private static final GlobalLogCollector logCollector = new GlobalLogCollector(openShift,
            new File(environment.testLogDir()));
    private KeycloakClient keycloakApiClient;
    protected AddressApiClient addressApiClient;
    protected String username;
    protected String password;
    protected AmqpClientFactory amqpClientFactory;
    protected MqttClientFactory mqttClientFactory;

    protected boolean createDefaultAddressSpace() {
        return true;
    }

    @Before
    public void setup() throws Exception {
        addressApiClient = new AddressApiClient(openShift);
        if (createDefaultAddressSpace()) {
            if (environment.isMultitenant()) {
                Logging.log.info("Test is running in multitenant mode");
                createAddressSpace(defaultAddressSpace, environment.defaultAuthService());
                // TODO: Wait another minute so that all services are connected
                Logging.log.info("Waiting for 2 minutes before starting tests");
            }

            if ("standard".equals(environment.defaultAuthService())) {
                this.username = "systemtest";
                this.password = "systemtest";
                getKeycloakClient().createUser(defaultAddressSpace.getName(), username, password, 1, TimeUnit.MINUTES);
            }
            amqpClientFactory = new AmqpClientFactory(openShift, environment, defaultAddressSpace, username, password);
            mqttClientFactory = new MqttClientFactory(openShift, environment, defaultAddressSpace, username, password);
        }
    }

    @After
    public void teardown() throws Exception {
        if (mqttClientFactory != null) {
            mqttClientFactory.close();
        }
        if (amqpClientFactory != null) {
            amqpClientFactory.close();
        }
        if (addressApiClient != null) {
            if (createDefaultAddressSpace()) {
                setAddresses();
            }
            addressApiClient.close();
        }
    }

    protected AddressSpace createAddressSpace(AddressSpace addressSpace, String authService) throws Exception {
        if (!TestUtils.existAddressSpace(addressApiClient, addressSpace.getName())) {
            Logging.log.info("Address space '" + addressSpace + "' doesn't exist and will be created.");
            addressApiClient.createAddressSpace(addressSpace, authService);
            logCollector.startCollecting(addressSpace.getNamespace());
            TestUtils.waitForAddressSpaceReady(addressApiClient, addressSpace.getName());
            if (addressSpace.equals(defaultAddressSpace)) {
                Thread.sleep(120_000);
            }
        } else {
            Logging.log.info("Address space '" + addressSpace + "' already exists.");
        }
        return addressSpace;
    }

    protected void deleteAddressSpace(AddressSpace addressSpace) throws Exception {
        logCollector.collectEvents(addressSpace.getNamespace());
        addressApiClient.deleteAddressSpace(addressSpace);
        TestUtils.waitForAddressSpaceDeleted(openShift, addressSpace);
        logCollector.stopCollecting(addressSpace.getNamespace());
    }

    //!TODO: protected void appendAddressSpace(...)

    protected JsonObject getAddressSpace(String name) throws Exception {
        return addressApiClient.getAddressSpace(name);
    }

    protected KeycloakClient getKeycloakClient() throws Exception {
        if (keycloakApiClient == null) {
            KeycloakCredentials creds = environment.keycloakCredentials();
            if (creds == null) {
                creds = openShift.getKeycloakCredentials();
            }
            keycloakApiClient = new KeycloakClient(openShift.getKeycloakEndpoint(), creds, openShift.getKeycloakCA());
        }
        return keycloakApiClient;
    }

    /**
     * use DELETE html method for delete specific addresses
     *
     * @param destinations
     * @throws Exception
     */
    protected void deleteAddresses(Destination... destinations) throws Exception {
        deleteAddresses(defaultAddressSpace, destinations);
    }

    protected void deleteAddresses(AddressSpace addressSpace, Destination... destinations) throws Exception {
        TestUtils.delete(addressApiClient, addressSpace, destinations);
    }

    /**
     * use POST html method to append addresses to already existing addresses
     *
     * @param destinations
     * @throws Exception
     */
    protected void appendAddresses(Destination... destinations) throws Exception {
        appendAddresses(defaultAddressSpace, destinations);
    }

    protected void appendAddresses(AddressSpace addressSpace, Destination... destinations) throws Exception {
        TimeoutBudget budget = new TimeoutBudget(5, TimeUnit.MINUTES);
        TestUtils.deploy(addressApiClient, openShift, budget, addressSpace, HttpMethod.POST, destinations);
    }

    /**
     * use PUT html method to replace all addresses
     *
     * @param destinations
     * @throws Exception
     */
    protected void setAddresses(Destination... destinations) throws Exception {
        setAddresses(defaultAddressSpace, destinations);
    }

    protected void setAddresses(AddressSpace addressSpace, Destination... destinations) throws Exception {
        TimeoutBudget budget = new TimeoutBudget(5, TimeUnit.MINUTES);
        TestUtils.deploy(addressApiClient, openShift, budget, addressSpace, HttpMethod.PUT, destinations);
    }

    /**
     * give you a list of all deployed addresses (or single deployed address)
     *
     * @param addressName name of single address
     * @return list of addresses
     * @throws Exception
     */
    protected Future<List<String>> getAddresses(Optional<String> addressName) throws Exception {
        return getAddresses(defaultAddressSpace, addressName);
    }

    protected Future<List<String>> getAddresses(AddressSpace addressSpace, Optional<String> addressName) throws Exception {
        return TestUtils.getAddresses(addressApiClient, addressSpace, addressName);
    }

    protected void scale(Destination destination, int numReplicas) throws Exception {
        scale(defaultAddressSpace, destination, numReplicas);
    }

    protected void scale(AddressSpace addressSpace, Destination destination, int numReplicas) throws Exception {
        TimeoutBudget budget = new TimeoutBudget(5, TimeUnit.MINUTES);
        TestUtils.setReplicas(openShift, addressSpace, destination, numReplicas, budget);
    }

    protected void scaleKeycloak(int numReplicas) throws Exception {
        scaleInGlobal("keycloak", numReplicas);
    }

    /**
     * scale up/down deployment to count of replicas, includes waiting for expected replicas
     *
     * @param deployment name of deployment
     * @param numReplicas count of replicas
     * @throws InterruptedException
     */
    private void scaleInGlobal(String deployment, int numReplicas) throws InterruptedException {
        if (numReplicas >= 0) {
            TimeoutBudget budget = new TimeoutBudget(5, TimeUnit.MINUTES);
            TestUtils.setReplicas(openShift, environment.namespace(), deployment, numReplicas, budget);
        } else {
            throw new IllegalArgumentException("'numReplicas' must be greater than 0");
        }

    }

    protected AmqpClientFactory createAmqpClientFactory(AddressSpace addressSpace) {
        return new AmqpClientFactory(new OpenShift(environment, environment.namespace(), addressSpace.getNamespace()),
                environment, addressSpace, username, password);
    }

    protected MqttClientFactory createMqttClientFactory(AddressSpace addressSpace) {
        return new MqttClientFactory(new OpenShift(environment, environment.namespace(), addressSpace.getNamespace()),
                environment, addressSpace, username, password);
    }

    protected boolean isBrokered(AddressSpace addressSpace) throws Exception{
        return addressSpace.getType().equals(AddressSpaceType.BROKERED);
    }

    protected void assertCanConnect(AddressSpace addressSpace, String username, String password, List<Destination> destinations) throws Exception {
        assertTrue(canConnectWithAmqp(addressSpace, username, password , destinations));
        // TODO: Enable this when mqtt is stable enough
        // assertTrue(canConnectWithMqtt(addressSpace, username, password));
    }

    protected void assertCannotConnect(AddressSpace addressSpace, String username, String password, List<Destination> destinations) throws Exception {
        try {
            assertFalse(canConnectWithAmqp(addressSpace, username, password, destinations));
            fail("Expected connection to timeout");
        } catch (ConnectTimeoutException e) {
        }

        // TODO: Enable this when mqtt is stable enough
        // assertFalse(canConnectWithMqtt(addressSpace, username, password));
    }


    private boolean canConnectWithAmqp(AddressSpace addressSpace, String username, String password, List<Destination> destinations) throws Exception {
        for (Destination destination : destinations){
            switch (destination.getType()){
                case "queue":
                    assertTrue(canConnectWithAmqpToQueue(addressSpace, username, password, destination.getAddress()));
                    break;
                case "topic":
                    assertTrue(canConnectWithAmqpToTopic(addressSpace, username, password, destination.getAddress()));
                    break;
                case "multicast":
                    if (!isBrokered(addressSpace))
                        assertTrue(canConnectWithAmqpToMulticast(addressSpace, username, password, destination.getAddress()));
                    break;
                case "anycast":
                    if(!isBrokered(addressSpace))
                        assertTrue(canConnectWithAmqpToAnycast(addressSpace, username, password, destination.getAddress()));
                    break;
            }
        }
        return true;
    }

    private boolean canConnectWithMqtt(String name, String username, String password) throws Exception {
        AddressSpace addressSpace = new AddressSpace(name);
        MqttClient client = mqttClientFactory.createClient(addressSpace);
        MqttConnectOptions options = client.getMqttConnectOptions();
        options.setUserName(username);
        options.setPassword(password.toCharArray());

        Future<List<String>> received = client.recvMessages("t1", 1);
        Future<Integer> sent = client.sendMessages("t1", Arrays.asList("msgt1"));

        return (sent.get(1, TimeUnit.MINUTES) == received.get(1, TimeUnit.MINUTES).size());
    }

    protected boolean canConnectWithAmqpToQueue(AddressSpace addressSpace, String username, String password, String queueAddress) throws InterruptedException, IOException, TimeoutException, ExecutionException {
        AmqpClientFactory clientFactory = createAmqpClientFactory(addressSpace);
        AmqpClient client = clientFactory.createQueueClient(addressSpace);
        client.getConnectOptions().setUsername(username).setPassword(password);

        Future<Integer> sent = client.sendMessages(queueAddress, Arrays.asList("msg1"), 10, TimeUnit.SECONDS);
        Future<List<Message>> received = client.recvMessages(queueAddress, 1, 10, TimeUnit.SECONDS);

        return (sent.get(1, TimeUnit.MINUTES) == received.get(1, TimeUnit.MINUTES).size());
    }

    protected boolean canConnectWithAmqpToAnycast(AddressSpace addressSpace, String username, String password, String anycastAddress) throws InterruptedException, IOException, TimeoutException, ExecutionException {
        AmqpClientFactory clientFactory = createAmqpClientFactory(addressSpace);
        AmqpClient client = clientFactory.createQueueClient(addressSpace);
        client.getConnectOptions().setUsername(username).setPassword(password);

        Future<List<Message>> received = client.recvMessages(anycastAddress, 1, 10, TimeUnit.SECONDS);
        Future<Integer> sent = client.sendMessages(anycastAddress, Arrays.asList("msg1"), 10, TimeUnit.SECONDS);

        return (sent.get(1, TimeUnit.MINUTES) == received.get(1, TimeUnit.MINUTES).size());
    }

    protected boolean canConnectWithAmqpToMulticast(AddressSpace addressSpace, String username, String password, String multicastAddress) throws InterruptedException, IOException, TimeoutException, ExecutionException {
        AmqpClientFactory clientFactory = createAmqpClientFactory(addressSpace);
        AmqpClient client = clientFactory.createBroadcastClient(addressSpace);
        client.getConnectOptions().setUsername(username).setPassword(password);

        Future<List<Message>> received = client.recvMessages(multicastAddress, 1, 10, TimeUnit.SECONDS);
        Future<Integer> sent = client.sendMessages(multicastAddress, Arrays.asList("msg1"), 10, TimeUnit.SECONDS);

        return (sent.get(1, TimeUnit.MINUTES) == received.get(1, TimeUnit.MINUTES).size());
    }

    protected boolean canConnectWithAmqpToTopic(AddressSpace addressSpace, String username, String password, String topicAddress) throws InterruptedException, IOException, TimeoutException, ExecutionException {
        AmqpClientFactory clientFactory = createAmqpClientFactory(addressSpace);
        AmqpClient client = clientFactory.createTopicClient(addressSpace);
        client.getConnectOptions().setUsername(username).setPassword(password);

        Future<List<Message>> received = client.recvMessages(topicAddress, 1, 10, TimeUnit.SECONDS);
        Future<Integer> sent = client.sendMessages(topicAddress, Arrays.asList("msg1"), 10, TimeUnit.SECONDS);

        return (sent.get(1, TimeUnit.MINUTES) == received.get(1, TimeUnit.MINUTES).size());
    }
}
