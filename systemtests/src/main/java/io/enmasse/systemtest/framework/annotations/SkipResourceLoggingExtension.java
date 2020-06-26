/*
 * Copyright 2019, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.systemtest.framework.annotations;

import io.enmasse.systemtest.messaginginfra.ResourceManager;
import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.ExtensionContext;

public class SkipResourceLoggingExtension implements BeforeAllCallback, AfterAllCallback {

    @Override
    public void beforeAll(ExtensionContext extensionContext) {
        ResourceManager.getInstance().disableVerboseLogging();
    }

    @Override
    public void afterAll(ExtensionContext extensionContext) {
        ResourceManager.getInstance().enableVerboseLogging();
    }
}
