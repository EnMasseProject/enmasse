/*
 * Copyright 2019, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.systemtest.bases.iot;

import io.enmasse.address.model.AddressSpace;
import io.enmasse.iot.model.v1.IoTConfig;
import io.enmasse.iot.model.v1.IoTProject;
import io.enmasse.systemtest.TestTag;
import io.enmasse.systemtest.amqp.AmqpClientFactory;
import io.enmasse.systemtest.bases.ITestBase;
import io.enmasse.systemtest.manager.IsolatedIoTManager;
import io.enmasse.systemtest.manager.ResourceManager;
import io.enmasse.systemtest.model.addressspace.AddressSpacePlans;
import io.enmasse.systemtest.model.addressspace.AddressSpaceType;
import io.enmasse.systemtest.mqtt.MqttClientFactory;
import org.junit.jupiter.api.Tag;

@Tag(TestTag.ISOLATED_IOT)
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

    @Override
    default String getDefaultAddressSpacePlan() {
        return AddressSpacePlans.STANDARD_SMALL;
    }

    @Override
    default String getDefaultAddrSpaceIdentifier() {
        return "standard";
    }

    @Override
    default AddressSpaceType getAddressSpaceType() {
        return AddressSpaceType.STANDARD;
    }
}
