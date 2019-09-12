/*
 * Copyright 2019, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.systemtest.bases.isolated;

import io.enmasse.address.model.AddressSpace;
import io.enmasse.systemtest.amqp.AmqpClientFactory;
import io.enmasse.systemtest.bases.ITestBase;
import io.enmasse.systemtest.manager.CommonResourcesManager;
import io.enmasse.systemtest.manager.ResourceManager;
import io.enmasse.systemtest.mqtt.MqttClientFactory;

import java.util.List;

public interface ITestBaseIsolated extends ITestBase {
     CommonResourcesManager commonResourcesManager = CommonResourcesManager.getInstance();
    List<AddressSpace> currentAddressSpaces = commonResourcesManager.getCurrentAddressSpaces();

    default AmqpClientFactory getAmqpClientFactory() {
        return commonResourcesManager.getAmqpClientFactory();
    }

    default MqttClientFactory getMqttClientFactory() {
        return commonResourcesManager.getMqttClientFactory();
    }

    @Override
    default ResourceManager getResourceManager() {
        return commonResourcesManager;
    }
}
