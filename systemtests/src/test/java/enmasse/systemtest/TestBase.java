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

    private static final String ADDRESS_SPACE = "testspace";
    protected static final Environment environment = new Environment();
    protected static final OpenShift openShift = new OpenShift(environment, environment.isMultitenant() ? ADDRESS_SPACE : environment.namespace());
    private static final GlobalLogCollector logCollector = new GlobalLogCollector(openShift, new File(environment.testLogDir()));

    protected AddressApiClient addressApiClient;

    @Before
    public void setup() throws Exception {
        addressApiClient = new AddressApiClient(openShift.getRestEndpoint(), environment.isMultitenant());
        if (environment.isMultitenant()) {
            Logging.log.info("Test is running in multitenant mode");
            if (!TestUtils.existAddressSpace(addressApiClient, ADDRESS_SPACE)) {
                Logging.log.info("Address space " + ADDRESS_SPACE + "doesn't exist and will be created.");
                addressApiClient.createAddressSpace(ADDRESS_SPACE);
                logCollector.collectLogs(ADDRESS_SPACE);
                TestUtils.waitForAddressSpaceReady(addressApiClient, ADDRESS_SPACE);
            }
        }
    }

    @After
    public void teardown() throws Exception {
        setAddresses();
        addressApiClient.close();
    }


    /**
     * use DELETE html method for delete specific addresses
     *
     * @param destinations
     * @throws Exception
     */
    protected void deleteAddresses(Destination... destinations) throws Exception {
        TestUtils.delete(addressApiClient, ADDRESS_SPACE, destinations);
    }

    /**
     * use POST html method to append addresses to already existing addresses
     *
     * @param destinations
     * @throws Exception
     */
    protected void appendAddresses(Destination... destinations) throws Exception {
        TimeoutBudget budget = new TimeoutBudget(5, TimeUnit.MINUTES);
        TestUtils.deploy(addressApiClient, openShift, budget, ADDRESS_SPACE, HttpMethod.POST, destinations);
    }

    /**
     * use PUT html method to replace all addresses
     *
     * @param destinations
     * @throws Exception
     */
    protected void setAddresses(Destination... destinations) throws Exception {
        TimeoutBudget budget = new TimeoutBudget(5, TimeUnit.MINUTES);
        TestUtils.deploy(addressApiClient, openShift, budget, ADDRESS_SPACE, HttpMethod.PUT, destinations);
    }

    /**
     * give you a list of all deployed addresses (or single deployed address)
     *
     * @param addressName name of single address
     * @return list of addresses
     * @throws Exception
     */
    protected Future<List<String>> getAddresses(Optional<String> addressName) throws Exception {
        return TestUtils.getAddresses(addressApiClient, ADDRESS_SPACE, addressName);
    }

    protected void scale(Destination destination, int numReplicas) throws Exception {
        TimeoutBudget budget = new TimeoutBudget(5, TimeUnit.MINUTES);
        TestUtils.setReplicas(openShift, destination, numReplicas, budget);
        if (Destination.isQueue(destination)) {
            TestUtils.waitForAddress(openShift, destination.getAddress(), budget);
        }
    }
}
