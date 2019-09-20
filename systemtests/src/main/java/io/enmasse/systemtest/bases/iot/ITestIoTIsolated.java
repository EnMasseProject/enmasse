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
import io.enmasse.systemtest.manager.IsolatedIoTManager;
import io.enmasse.systemtest.manager.ResourceManager;
import io.enmasse.systemtest.mqtt.MqttClientFactory;

public interface ITestIoTIsolated extends ITestIoTBase, ITestBase {

    IsolatedIoTManager isolatedIoTManager = IsolatedIoTManager.getInstance();

    default AmqpClientFactory getAmqpClientFactory() {
        return isolatedIoTManager.getAmqpClientFactory();
    }

    default MqttClientFactory getMqttClientFactory() {
        return isolatedIoTManager.getMqttClientFactory();
    }

    @Override
    default ResourceManager getResourceManager() {
        return isolatedIoTManager;
    }

}
