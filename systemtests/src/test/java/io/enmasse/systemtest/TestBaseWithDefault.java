/*
 * Copyright 2017 Red Hat Inc.
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

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.rules.TestWatcher;
import org.junit.runner.Description;
import org.junit.runner.Result;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

public abstract class TestBaseWithDefault extends TestBase {
    private static final String defaultAddressTemplate = "-default-";
    private static Map<AddressSpaceType, Integer> spaceCountMap = new HashMap<>();
    protected AddressSpace defaultAddressSpace;

    protected abstract AddressSpaceType getAddressSpaceType();

    @Rule
    public TestWatcher watcher = new TestWatcher() {
        @Override
        protected void failed(Throwable e, Description description) {
        Logging.log.info("test failed:" + description);
        Logging.log.info("default address space '{}' will be removed", defaultAddressSpace);
        try {
            deleteAddressSpace(defaultAddressSpace);
            spaceCountMap.put(defaultAddressSpace.getType(), spaceCountMap.get(defaultAddressSpace.getType()) + 1);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        }
    };

    @Override
    public void testRunFinished(Result result) throws Exception {
        Logging.log.info("default address space '{}' will be removed", defaultAddressSpace);
        try {
            deleteAddressSpace(defaultAddressSpace);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public AddressSpace getSharedAddressSpace() {
        return defaultAddressSpace;
    }

    @Before
    public void setupDefault() throws Exception {
        spaceCountMap.putIfAbsent(getAddressSpaceType(), 0);
        defaultAddressSpace = new AddressSpace(getAddressSpaceType().name().toLowerCase() + defaultAddressTemplate + spaceCountMap.get(getAddressSpaceType()), getAddressSpaceType());
        Logging.log.info("Test is running in multitenant mode");
        createAddressSpace(defaultAddressSpace, environment.defaultAuthService());
        // TODO: Wait another minute so that all services are connected
        Logging.log.info("Waiting for 2 minutes before starting tests");

        if ("standard".equals(environment.defaultAuthService())) {
            this.username = "systemtest";
            this.password = "systemtest";
            getKeycloakClient().createUser(defaultAddressSpace.getName(), username, password, 1, TimeUnit.MINUTES);
        }
    }

    @After
    public void teardownDefault() throws Exception {
        setAddresses(defaultAddressSpace);
    }

    protected void scale(Destination destination, int numReplicas) throws Exception {
        scale(defaultAddressSpace, destination, numReplicas);
    }

    protected Future<List<String>> getAddresses(Optional<String> addressName) throws Exception {
        return getAddresses(defaultAddressSpace, addressName);
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

    /**
     * use POST html method to append addresses to already existing addresses
     *
     * @param destinations
     * @throws Exception
     */
    protected void appendAddresses(Destination... destinations) throws Exception {
        appendAddresses(defaultAddressSpace, destinations);
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
}

