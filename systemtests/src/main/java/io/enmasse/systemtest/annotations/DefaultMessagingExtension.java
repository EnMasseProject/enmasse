/*
 * Copyright 2019, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.systemtest.annotations;

import io.enmasse.systemtest.manager.ResourceManager;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.BeforeTestExecutionCallback;
import org.junit.jupiter.api.extension.ExtensionContext;

import java.util.Optional;

import static org.junit.platform.commons.support.AnnotationSupport.findAnnotation;

public class DefaultMessagingExtension implements BeforeTestExecutionCallback, BeforeAllCallback {

    @Override
    public void beforeAll(ExtensionContext extensionContext) throws Exception {
        Optional<DefaultMessaging> annotation = findAnnotation(extensionContext.getElement(), DefaultMessaging.class);
        if (annotation.isPresent()) {
            ResourceManager.getInstance().createDefaultMessaging(annotation.get().type(), annotation.get().plan());
        }
    }

    @Override
    public void beforeTestExecution(ExtensionContext extensionContext) throws Exception {
        Optional<DefaultMessaging> annotation = findAnnotation(extensionContext.getElement(), DefaultMessaging.class);
        if (annotation.isPresent()) {
            ResourceManager.getInstance().createDefaultMessaging(annotation.get().type(), annotation.get().plan());
        }
    }
}
