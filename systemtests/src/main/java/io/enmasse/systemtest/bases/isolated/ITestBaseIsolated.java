/*
 * Copyright 2019, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.systemtest.bases.isolated;

import static io.enmasse.systemtest.TestTag.ISOLATED;

import java.util.List;

import org.junit.jupiter.api.Tag;

import io.enmasse.address.model.AddressSpace;
import io.enmasse.systemtest.amqp.AmqpClientFactory;
import io.enmasse.systemtest.bases.ITestBase;
import io.enmasse.systemtest.manager.IsolatedResourcesManager;
import io.enmasse.systemtest.manager.ResourceManager;

@Tag(ISOLATED)
public interface ITestBaseIsolated extends ITestBase {
    IsolatedResourcesManager isolatedResourcesManager = IsolatedResourcesManager.getInstance();
    List<AddressSpace> currentAddressSpaces = isolatedResourcesManager.getCurrentAddressSpaces();

    default AmqpClientFactory getAmqpClientFactory() {
        return isolatedResourcesManager.getAmqpClientFactory();
    }

    @Override
    default ResourceManager getResourceManager() {
        return isolatedResourcesManager;
    }
}
