/*
 * Copyright 2019, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.systemtest.bases.shared;

import io.enmasse.address.model.AddressSpace;
import io.enmasse.systemtest.amqp.AmqpClientFactory;
import io.enmasse.systemtest.bases.ITestBase;
import io.enmasse.systemtest.manager.ResourceManager;
import io.enmasse.systemtest.manager.SharedResourceManager;
import io.enmasse.systemtest.mqtt.MqttClientFactory;

public interface ITestBaseShared extends ITestBase {
    SharedResourceManager sharedResourceManager = SharedResourceManager.getInstance();

    default AddressSpace getSharedAddressSpace() {
        return sharedResourceManager.getSharedAddressSpace();
    }

    default AmqpClientFactory getAmqpClientFactory() {
        return sharedResourceManager.getAmqpClientFactory();
    }

    default MqttClientFactory getMqttClientFactory() {
        return sharedResourceManager.getMqttClientFactory();
    }

    @Override
    default ResourceManager getResourceManager() {
        return sharedResourceManager;
    }
}
