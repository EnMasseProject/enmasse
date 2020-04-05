/*
 * Copyright 2019, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.systemtest.annotations;

import io.enmasse.systemtest.manager.ResourceManager;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.BeforeTestExecutionCallback;
import org.junit.jupiter.api.extension.ExtensionContext;

import static org.junit.platform.commons.support.AnnotationSupport.findAnnotation;

public class DefaultIoTExtension implements BeforeTestExecutionCallback, BeforeAllCallback {

    @Override
    public void beforeAll(ExtensionContext extensionContext) throws Exception {
        ResourceManager.getInstance().createDefaultIoT();
    }

    @Override
    public void beforeTestExecution(ExtensionContext extensionContext) throws Exception {
        ResourceManager.getInstance().createDefaultIoT();
    }
}
