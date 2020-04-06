/*
 * Copyright 2019, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.systemtest.annotations;

import io.enmasse.systemtest.info.TestInfo;
import io.enmasse.systemtest.manager.ResourceManager;
import io.enmasse.systemtest.platform.apps.SystemtestsKubernetesApps;
import io.enmasse.systemtest.utils.TestUtils;
import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.AfterTestExecutionCallback;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.BeforeTestExecutionCallback;
import org.junit.jupiter.api.extension.ExtensionContext;

import java.nio.file.Path;
import java.util.Optional;

import static org.junit.platform.commons.support.AnnotationSupport.findAnnotation;

public class DefaultIoTExtension implements BeforeTestExecutionCallback, BeforeAllCallback, AfterTestExecutionCallback, AfterAllCallback {

    @Override
    public void beforeAll(ExtensionContext extensionContext) throws Exception {
        Optional<DefaultIoT> annotation = findAnnotation(extensionContext.getElement(), DefaultIoT.class);
        if (annotation.isPresent()) {
            ResourceManager.getInstance().createDefaultIoT();
        }

    }

    @Override
    public void beforeTestExecution(ExtensionContext extensionContext) throws Exception {
        Optional<DefaultIoT> annotation = findAnnotation(extensionContext.getElement(), DefaultIoT.class);
        if (annotation.isPresent()) {
            ResourceManager.getInstance().createDefaultIoT();
        }
    }

    @Override
    public void afterAll(ExtensionContext extensionContext) throws Exception {
        Optional<DefaultMessaging> annotation = findAnnotation(extensionContext.getElement(), DefaultMessaging.class);
        if (annotation.isPresent()) {
            if (extensionContext.getExecutionException().isPresent()) {
                Path path = TestUtils.getFailedTestLogsPath(extensionContext);
                SystemtestsKubernetesApps.collectInfinispanServerLogs(path);
            }
            SystemtestsKubernetesApps.deleteInfinispanServer();
            SystemtestsKubernetesApps.deletePostgresqlServer();
            SystemtestsKubernetesApps.deleteH2Server();
        }
    }

    @Override
    public void afterTestExecution(ExtensionContext extensionContext) throws Exception {
        Optional<DefaultMessaging> annotation = findAnnotation(extensionContext.getElement(), DefaultMessaging.class);
        if (annotation.isPresent()) {
            if (extensionContext.getExecutionException().isPresent()) {
                Path path = TestUtils.getFailedTestLogsPath(extensionContext);
                SystemtestsKubernetesApps.collectInfinispanServerLogs(path);
            }
            SystemtestsKubernetesApps.deleteInfinispanServer();
            SystemtestsKubernetesApps.deletePostgresqlServer();
            SystemtestsKubernetesApps.deleteH2Server();
        }
    }
}
