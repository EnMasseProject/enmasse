/*
 * Copyright 2019, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.systemtest.annotations;

import io.enmasse.api.model.MessagingTenant;
import io.enmasse.systemtest.messaginginfra.ResourceManager;
import io.enmasse.systemtest.messaginginfra.resources.MessagingTenantResourceType;
import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.BeforeTestExecutionCallback;
import org.junit.jupiter.api.extension.ExtensionContext;

public class DefaultMessagingTenantExtension implements BeforeTestExecutionCallback, BeforeAllCallback, AfterAllCallback {
    private boolean isFullClass = false;

    @Override
    public void beforeTestExecution(ExtensionContext extensionContext) {
        if (!isFullClass) {
            createDefaultTenant();
        }
    }

    @Override
    public void beforeAll(ExtensionContext extensionContext) {
        createDefaultTenant();
        isFullClass = true;
    }

    private void createDefaultTenant() {
        MessagingTenant tenant = MessagingTenantResourceType.getDefault();
        ResourceManager.getInstance().createResource(tenant);
        ResourceManager.getInstance().setDefaultMessagingTenant(tenant);
    }

    @Override
    public void afterAll(ExtensionContext extensionContext) throws Exception {
        isFullClass = false;
    }
}
