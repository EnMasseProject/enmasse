/*
 * Copyright 2019, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.systemtest.framework.annotations;

import io.enmasse.api.model.MessagingProject;
import io.enmasse.systemtest.messaginginfra.ResourceManager;
import io.enmasse.systemtest.messaginginfra.resources.MessagingProjectResourceType;
import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.BeforeTestExecutionCallback;
import org.junit.jupiter.api.extension.ExtensionContext;

public class DefaultMessagingProjectExtension implements BeforeTestExecutionCallback, BeforeAllCallback, AfterAllCallback {
    private boolean isFullClass = false;

    @Override
    public void beforeTestExecution(ExtensionContext extensionContext) {
        if (!isFullClass) {
            createDefaultTenant(extensionContext);
        }
    }

    @Override
    public void beforeAll(ExtensionContext extensionContext) {
        createDefaultTenant(extensionContext);
        isFullClass = true;
    }

    private void createDefaultTenant(ExtensionContext extensionContext) {
        MessagingProject project = MessagingProjectResourceType.getDefault();
        ResourceManager.getInstance().createResource(extensionContext, project);
        ResourceManager.getInstance().setDefaultMessagingProject(project);
    }

    @Override
    public void afterAll(ExtensionContext extensionContext) throws Exception {
        isFullClass = false;
    }
}
