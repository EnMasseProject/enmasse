/*
 * Copyright 2019, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.systemtest.bases.iot;

import io.enmasse.address.model.AddressSpace;
import io.enmasse.iot.model.v1.IoTConfig;
import io.enmasse.iot.model.v1.IoTProject;
import io.enmasse.systemtest.amqp.AmqpClientFactory;
import io.enmasse.systemtest.bases.ITestBase;
import io.enmasse.systemtest.manager.ResourceManager;
import io.enmasse.systemtest.mqtt.MqttClientFactory;

public interface ITestIoTIsolated extends ITestIoTBase, ITestBase {

    default AddressSpace getSharedAddressSpace() {
        return sharedIoTResourceManager.getSharedAddressSpace();
    }

    default AmqpClientFactory getAmqpClientFactory() {
        return sharedIoTResourceManager.getAmqpClientFactory();
    }

    default MqttClientFactory getMqttClientFactory() {
        return sharedIoTResourceManager.getMqttClientFactory();
    }

    default IoTProject getSharedIoTProject() {
        return sharedIoTResourceManager.getSharedIoTProject();
    }

    default IoTConfig getSharedIoTConfig() {
        return sharedIoTResourceManager.getSharedIoTConfig();
    }

    @Override
    default ResourceManager getResourceManager() {
        return sharedIoTResourceManager;
    }

}
