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

import java.util.List;
import java.util.Optional;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public abstract class TestBaseWithDefault extends TestBase {
    private static final String defaultAddressTemplate = "-default-";
    protected AddressSpace defaultAddressSpace = new AddressSpace(getAddressSpaceType().name().toLowerCase() + defaultAddressTemplate + "0", getAddressSpaceType());

    protected abstract AddressSpaceType getAddressSpaceType();

    @Rule
    public TestWatcher watcher = new TestWatcher() {
        @Override
        protected void failed(Throwable e, Description description) {
            Logging.log.info("test failed:" + description);
            if (getSharedAddressSpace() != null) {
                Logging.log.info("default address space '{}' will be removed", defaultAddressSpace);
                try {
                    deleteAddressSpace(defaultAddressSpace);
                    initializeSharedAddressSpace();
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
        }
    };

    @Override
    public void testRunFinished(Result result) throws Exception {
        try {
            if (getSharedAddressSpace() != null) {
                deleteAddressSpace(getSharedAddressSpace());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    public AddressSpace getSharedAddressSpace() {
        return defaultAddressSpace;
    }

    @Before
    public void setupDefault() throws Exception {
        Logging.log.info("Test is running in multitenant mode");
        createAddressSpace(getSharedAddressSpace(), environment.defaultAuthService());
        // TODO: Wait another minute so that all services are connected
        Logging.log.info("Waiting for 2 minutes before starting tests");

        if ("standard".equals(environment.defaultAuthService())) {
            this.username = "systemtest";
            this.password = "systemtest";
            getKeycloakClient().createUser(getSharedAddressSpace().getName(), username, password, 1, TimeUnit.MINUTES);
        }
    }

    @After
    public void teardownDefault() throws Exception {
        setAddresses(defaultAddressSpace);
    }


    /**
     * initialize new defaultBrokeredAddressSpace with new name due to collecting logs
     */
    private void initializeSharedAddressSpace() {
        String regExp = "^(.*)-([0-9]+)";
        Pattern p = Pattern.compile(regExp);
        Matcher m = p.matcher(defaultAddressSpace.getName());
        if (m.find()) {
            int suffix = Integer.valueOf(m.group(2)) + 1; //number of created default brokered address space + 1
            defaultAddressSpace = new AddressSpace(getAddressSpaceType().name().toLowerCase() + defaultAddressTemplate + suffix, getAddressSpaceType());
        } else {
            throw new IllegalStateException("Wrong name of default brokered address space! Didn't match reg exp: " + regExp);
        }
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

