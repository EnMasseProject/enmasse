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
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.rules.TestWatcher;
import org.junit.runner.Description;
import org.junit.runner.Result;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MultiTenantTestBase extends TestBase {

    private static final String defaultBrokeredAddressSpaceName = "brokered-default-";
    protected AddressSpace defaultBrokeredAddressSpace = new AddressSpace(defaultBrokeredAddressSpaceName + "0", AddressSpaceType.BROKERED);
    private List<AddressSpace> addressSpaces = new ArrayList<>();

    @Rule
    public TestWatcher watcher = new TestWatcher() {
        @Override
        protected void failed(Throwable e, Description description) {
            Logging.log.info("test failed:" + description);
            if (createDefaultBrokeredAddressSpace()) {
                Logging.log.info("default brokered address space '{}' will be removed", defaultBrokeredAddressSpace);
                try {
                    deleteAddressSpace(defaultBrokeredAddressSpace);
                    initializeDefaultBrokeredAddressSpace();
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
        }
    };

    @Before
    public void setupSpaceList() throws Exception {
        addressSpaces = new ArrayList<>();
        if (createDefaultBrokeredAddressSpace()) {
            if (environment.isMultitenant()) {
                Logging.log.info("Test is running in multitenant mode");
                super.createAddressSpace(defaultBrokeredAddressSpace, "none");
                // TODO: Wait another minute so that all services are connected
                Logging.log.info("Waiting for 2 minutes before starting tests");
            }
            amqpClientFactory = new AmqpClientFactory(openShift, environment, defaultBrokeredAddressSpace, username, password);
            mqttClientFactory = new MqttClientFactory(openShift, environment, defaultBrokeredAddressSpace, username, password);
        }

    }

    @After
    public void teardownSpaces() throws Exception {
        if (addressApiClient != null) {
            if (createDefaultBrokeredAddressSpace()) {
                setAddresses(defaultBrokeredAddressSpace);
            }
            for (AddressSpace addressSpace : addressSpaces) {
                deleteAddressSpace(addressSpace);
            }
            addressSpaces.clear();
        }
    }

    @Override
    protected boolean createDefaultAddressSpace() {
        return false;
    }

    protected boolean createDefaultBrokeredAddressSpace() {
        return true;
    }

    @Override
    protected AddressSpace createAddressSpace(AddressSpace addressSpace, String authService) throws Exception {
        super.createAddressSpace(addressSpace, authService);
        addressSpaces.add(addressSpace);
        return addressSpace;
    }

    @Override
    public void testRunFinished(Result result) throws Exception {
        try {
            deleteDefaultAddressSpaces();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * initialize new defaultBrokeredAddressSpace with new name due to collecting logs
     */
    private void initializeDefaultBrokeredAddressSpace() {
        String regExp = "^(.*)-([0-9]+)";
        Pattern p = Pattern.compile(regExp);
        Matcher m = p.matcher(defaultBrokeredAddressSpace.getName());
        if (m.find()) {
            int sufix = Integer.valueOf(m.group(2)) + 1; //number of created default brokered address space + 1
            defaultBrokeredAddressSpace = new AddressSpace(defaultBrokeredAddressSpaceName + sufix, AddressSpaceType.BROKERED);
        }
        throw new IllegalStateException("Wrong name of default brokered address space! Didn't match reg exp: " + regExp);
    }

    protected Endpoint getRouteEndpoint(AddressSpace addressSpace) {
        return openShift.getRouteEndpoint(addressSpace.getName(), "messaging");
    }

    protected void deleteDefaultAddressSpaces() throws Exception {
        if (addressApiClient != null) {
            if (createDefaultBrokeredAddressSpace()) {
                deleteAddressSpace(defaultBrokeredAddressSpace);
            }
            if (createDefaultAddressSpace()) {
                deleteAddressSpace(defaultAddressSpace);
            }
        }
    }
}
