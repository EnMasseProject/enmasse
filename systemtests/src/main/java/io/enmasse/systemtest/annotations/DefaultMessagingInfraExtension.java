/*
 * Copyright 2019, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.systemtest.annotations;

import io.enmasse.api.model.MessagingInfra;
import io.enmasse.systemtest.messaginginfra.ResourceManager;
import io.enmasse.systemtest.messaginginfra.resources.MessagingInfraResourceType;
import org.junit.jupiter.api.extension.BeforeTestExecutionCallback;
import org.junit.jupiter.api.extension.ExtensionContext;

public class DefaultMessagingInfraExtension implements BeforeTestExecutionCallback {
    @Override
    public void beforeTestExecution(ExtensionContext extensionContext) {
        MessagingInfra infra = MessagingInfraResourceType.getDefault();
        ResourceManager.getInstance().createResource(infra);
        ResourceManager.getInstance().setDefaultInfra(infra);
    }
}
