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

import java.util.ArrayList;
import java.util.List;

public class MultiTenantTestBase extends TestBase {

    protected AddressSpace defaultBrokeredAddressSpace = new AddressSpace("brokered-default", AddressSpaceType.BROKERED);
    private List<AddressSpace> addressSpaces = new ArrayList<>();

    @Rule
    public TestWatcher watcher = new TestWatcher() {
        @Override
        protected void failed(Throwable e, Description description) {
            Logging.log.info("test failed:" + description);
            if (createDefaultBrokeredAddressSpace()) {
                Logging.log.info("default brokered address space '{}' will be removed", defaultBrokeredAddressSpace);
                try {
                    deleteAddresses(defaultBrokeredAddressSpace);
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

    protected Endpoint getRouteEndpoint(AddressSpace addressSpace) {
        return openShift.getRouteEndpoint(addressSpace.getName(), "messaging");
    }
}
