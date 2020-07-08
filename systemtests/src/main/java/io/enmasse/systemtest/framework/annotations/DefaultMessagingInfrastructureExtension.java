/*
 * Copyright 2019, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.systemtest.framework.annotations;

import io.enmasse.api.model.MessagingInfrastructure;
import io.enmasse.systemtest.messaginginfra.ResourceManager;
import io.enmasse.systemtest.messaginginfra.resources.MessagingInfrastructureResourceType;
import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.BeforeTestExecutionCallback;
import org.junit.jupiter.api.extension.ExtensionContext;

public class DefaultMessagingInfrastructureExtension implements BeforeTestExecutionCallback, BeforeAllCallback, AfterAllCallback {
    private boolean isFullClass = false;

    @Override
    public void beforeTestExecution(ExtensionContext extensionContext) {
        if (!isFullClass) {
            createDefaultInfra(extensionContext);
        }
    }

    @Override
    public void beforeAll(ExtensionContext extensionContext) {
        createDefaultInfra(extensionContext);
        isFullClass = true;
    }

    private void createDefaultInfra(ExtensionContext extensionContext) {
        MessagingInfrastructure infra = MessagingInfrastructureResourceType.getDefault();
        ResourceManager.getInstance().createResource(extensionContext, infra);
        ResourceManager.getInstance().setDefaultInfra(infra);
    }

    @Override
    public void afterAll(ExtensionContext extensionContext) throws Exception {
        isFullClass = false;
    }
}
