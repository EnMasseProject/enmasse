/*
 * Copyright 2019, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.systemtest.bases.isolated;

import io.enmasse.address.model.AddressSpace;
import io.enmasse.systemtest.amqp.AmqpClientFactory;
import io.enmasse.systemtest.bases.ITestBase;
import io.enmasse.systemtest.manager.IsolatedResourcesManager;
import io.enmasse.systemtest.manager.ResourceManager;
import io.enmasse.systemtest.mqtt.MqttClientFactory;
import org.junit.jupiter.api.Tag;

import java.util.List;

import static io.enmasse.systemtest.TestTag.ISOLATED;

@Tag(ISOLATED)
public interface ITestBaseIsolated extends ITestBase {
    IsolatedResourcesManager ISOLATED_RESOURCES_MANAGER = IsolatedResourcesManager.getInstance();
    List<AddressSpace> currentAddressSpaces = ISOLATED_RESOURCES_MANAGER.getCurrentAddressSpaces();

    default AmqpClientFactory getAmqpClientFactory() {
        return ISOLATED_RESOURCES_MANAGER.getAmqpClientFactory();
    }

    default MqttClientFactory getMqttClientFactory() {
        return ISOLATED_RESOURCES_MANAGER.getMqttClientFactory();
    }

    @Override
    default ResourceManager getResourceManager() {
        return ISOLATED_RESOURCES_MANAGER;
    }
}
