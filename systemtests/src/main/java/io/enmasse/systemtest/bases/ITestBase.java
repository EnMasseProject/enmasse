/*
 * Copyright 2019, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.systemtest.bases;

import io.enmasse.systemtest.amqp.AmqpClientFactory;
import io.enmasse.systemtest.clients.ClientUtils;
import io.enmasse.systemtest.manager.ResourceManager;
import io.enmasse.systemtest.model.address.AddressType;
import io.enmasse.systemtest.model.addressspace.AddressSpaceType;
import io.enmasse.systemtest.mqtt.MqttClientFactory;

public interface ITestBase {
    ClientUtils clientUtils = new ClientUtils();

    default ClientUtils getClientUtils() {
        return clientUtils;
    }

    default AddressSpaceType getAddressSpaceType() {
        return null;
    }

    default String getDefaultPlan(AddressType addressType) {
        return null;
    }

    default String getDefaultAddressSpacePlan() {
        return null;
    }

    default String getDefaultAddrSpaceIdentifier() {
        return "default";
    }

    default ResourceManager getResourceManager() {return null;}
}
