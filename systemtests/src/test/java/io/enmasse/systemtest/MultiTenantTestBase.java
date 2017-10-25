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

import java.util.ArrayList;
import java.util.List;

public class MultiTenantTestBase extends TestBase {
    private List<AddressSpace> addressSpaces = new ArrayList<>();

    @Before
    public void setupSpaceList() throws Exception {
        addressSpaces = new ArrayList<>();
    }

    @After
    public void teardownSpaces() throws Exception {
        for (AddressSpace addressSpace : addressSpaces) {
            deleteAddressSpace(addressSpace);
        }
        addressSpaces.clear();
    }

    @Override
    protected boolean createDefaultAddressSpace() {
        return false;
    }

    @Override
    protected AddressSpace createAddressSpace(AddressSpace addressSpace, String authService, String addrSpaceType) throws Exception {
        super.createAddressSpace(addressSpace, authService, addrSpaceType);
        addressSpaces.add(addressSpace);
        return addressSpace;
    }

    @Override
    protected AddressSpace createAddressSpace(AddressSpace addressSpace, String authService) throws Exception {
        return createAddressSpace(addressSpace, authService, STANDARD_ADDRESS_SPACE_TYPE);
    }

    protected AmqpClientFactory createAmqpClientFactory(AddressSpace addressSpace) {
        return new AmqpClientFactory(new OpenShift(environment, environment.namespace(), addressSpace.getNamespace()),
                environment, addressSpace, username, password);
    }

    protected MqttClientFactory createMqttClientFactory(AddressSpace addressSpace) {
        return new MqttClientFactory(new OpenShift(environment, environment.namespace(), addressSpace.getNamespace()),
                environment, addressSpace, username, password);
    }
}
