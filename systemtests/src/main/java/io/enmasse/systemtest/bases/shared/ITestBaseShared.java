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
    SharedResourceManager SHARED_RESOURCE_MANAGER = SharedResourceManager.getInstance();

    default AddressSpace getSharedAddressSpace() {
        return SHARED_RESOURCE_MANAGER.getSharedAddressSpace();
    }

    default AmqpClientFactory getAmqpClientFactory() {
        return SHARED_RESOURCE_MANAGER.getAmqpClientFactory();
    }

    default MqttClientFactory getMqttClientFactory() {
        return SHARED_RESOURCE_MANAGER.getMqttClientFactory();
    }

    @Override
    default ResourceManager getResourceManager() {
        return SHARED_RESOURCE_MANAGER;
    }
}
