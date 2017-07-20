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

import org.junit.After;
import org.junit.Before;

import java.io.File;
import java.util.concurrent.TimeUnit;

/**
 * Base class for all tests
 */
public abstract class TestBase {

    private LogCollector logCollector;
    protected AddressApiClient addressApiClient;
    protected Environment environment = new Environment();
    protected OpenShift openShift;


    protected abstract String getInstanceName();

    @Before
    public void setup() throws Exception {
        openShift = new OpenShift(environment, environment.isMultitenant() ? getInstanceName().toLowerCase() : environment.namespace());
        File testLogs = new File("/tmp/testlogs");
        testLogs.mkdirs();
        logCollector = new LogCollector(openShift, testLogs);
        addressApiClient = new AddressApiClient(openShift.getRestEndpoint(), environment.isMultitenant());
        addressApiClient.deployInstance(getInstanceName().toLowerCase());
    }

    @After
    public void teardown() throws Exception {
        deploy();
        addressApiClient.close();
        logCollector.close();
    }

    protected void deploy(Destination ... destinations) throws Exception {
        TimeoutBudget budget = new TimeoutBudget(5, TimeUnit.MINUTES);
        TestUtils.deploy(addressApiClient, openShift, budget, getInstanceName().toLowerCase(), destinations);
    }

    protected void scale(Destination destination, int numReplicas) throws Exception {
        TimeoutBudget budget = new TimeoutBudget(5, TimeUnit.MINUTES);
        TestUtils.setReplicas(openShift, destination, numReplicas, budget);
        if (Destination.isQueue(destination)) {
            TestUtils.waitForAddress(openShift, destination.getAddress(), budget);
        }
    }
}
