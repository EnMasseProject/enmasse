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

import io.enmasse.systemtest.amqp.AmqpClientFactory;
import io.enmasse.systemtest.mqtt.MqttClientFactory;
import io.vertx.core.http.HttpMethod;
import org.junit.After;
import org.junit.Before;

import java.io.File;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

/**
 * Base class for all tests
 */
public abstract class TestBase {

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
        addressApiClient = new AddressApiClient(openShift.getRestEndpoint());
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
        }
        amqpClientFactory = new AmqpClientFactory(openShift, environment, defaultAddressSpace, username, password);
        mqttClientFactory = new MqttClientFactory(openShift, environment, defaultAddressSpace, username, password);
    }

    protected AddressSpace createAddressSpace(AddressSpace addressSpace, String authService, String addressSpaceType) throws Exception {
        if (!TestUtils.existAddressSpace(addressApiClient, addressSpace.getName())) {
            Logging.log.info("Address space " + addressSpace + "doesn't exist and will be created.");
            addressApiClient.createAddressSpace(addressSpace, authService, addressSpaceType);
            logCollector.startCollecting(addressSpace.getNamespace());
            TestUtils.waitForAddressSpaceReady(addressApiClient, addressSpace.getName());
            if (addressSpace.equals(defaultAddressSpace)) {
                Thread.sleep(120_000);
            }
        }
        return addressSpace;
    }

    protected AddressSpace createAddressSpace(AddressSpace addressSpace, String authService) throws Exception {
        createAddressSpace(addressSpace, authService, "standard");
        return addressSpace;
    }

    protected void deleteAddressSpace(AddressSpace addressSpace) throws Exception {
        addressApiClient.deleteAddressSpace(addressSpace);
        TestUtils.waitForAddressSpaceDeleted(openShift, addressSpace);
        logCollector.stopCollecting(addressSpace.getNamespace());
    }

    protected KeycloakClient getKeycloakClient() throws InterruptedException {
        if (keycloakApiClient == null) {
            KeycloakCredentials creds = environment.keycloakCredentials();
            if (creds == null) {
                creds = openShift.getKeycloakCredentials();
            }
            keycloakApiClient = new KeycloakClient(openShift.getKeycloakEndpoint(), creds);
        }
        return keycloakApiClient;
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

    /**
     * use DELETE html method for delete specific addresses
     *
     * @param destinations
     * @throws Exception
     */
    protected void deleteAddresses(Destination... destinations) throws Exception {
        TestUtils.delete(addressApiClient, defaultAddressSpace, destinations);
    }

    /**
     * use POST html method to append addresses to already existing addresses
     *
     * @param destinations
     * @throws Exception
     */
    protected void appendAddresses(Destination... destinations) throws Exception {
        TimeoutBudget budget = new TimeoutBudget(5, TimeUnit.MINUTES);
        TestUtils.deploy(addressApiClient, openShift, budget, defaultAddressSpace, HttpMethod.POST, destinations);
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
        return TestUtils.getAddresses(addressApiClient, defaultAddressSpace, addressName);
    }

    protected void scale(Destination destination, int numReplicas) throws Exception {
        TimeoutBudget budget = new TimeoutBudget(5, TimeUnit.MINUTES);
        TestUtils.setReplicas(openShift, defaultAddressSpace, destination, numReplicas, budget);
    }
}
