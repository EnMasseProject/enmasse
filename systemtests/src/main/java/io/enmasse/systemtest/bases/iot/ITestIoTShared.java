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
import io.enmasse.systemtest.iot.DeviceRegistryClient;
import io.enmasse.systemtest.manager.ResourceManager;
import io.enmasse.systemtest.manager.SharedIoTManager;
import io.enmasse.systemtest.model.addressspace.AddressSpacePlans;
import io.enmasse.systemtest.model.addressspace.AddressSpaceType;
import io.enmasse.systemtest.mqtt.MqttClientFactory;
import org.junit.jupiter.api.Tag;

@Tag(TestTag.SHARED_IOT)
public interface ITestIoTShared extends ITestBase, ITestIoTBase {

    SharedIoTManager sharedIoTResourceManager = SharedIoTManager.getInstance();

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

    @Override
    default String getDefaultAddressSpacePlan() {
        return AddressSpacePlans.STANDARD_SMALL;
    }

    @Override
    default String getDefaultAddrSpaceIdentifier() {
        return "shared-iot";
    }

    @Override
    default AddressSpaceType getAddressSpaceType() {
        return AddressSpaceType.STANDARD;
    }
}
