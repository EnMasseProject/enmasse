/*
 * Copyright 2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.systemtest.bases.isolated;

import io.enmasse.systemtest.amqp.AmqpClientFactory;
import io.enmasse.systemtest.bases.ITestBase;
import io.enmasse.systemtest.manager.IsolatedResourcesManager;
import io.enmasse.systemtest.manager.ResourceManager;


public interface ITestIsolatedSharedInfra extends ITestBase {

    IsolatedResourcesManager isolatedResourcesManager = IsolatedResourcesManager.getInstance();

    default AmqpClientFactory getAmqpClientFactory() {
        return isolatedResourcesManager.getAmqpClientFactory();
    }

    @Override
    default ResourceManager getResourceManager() {
        return isolatedResourcesManager;
    }
}
