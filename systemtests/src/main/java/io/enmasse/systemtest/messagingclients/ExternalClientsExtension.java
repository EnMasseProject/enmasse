/*
 * Copyright 2019, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.systemtest.messagingclients;

import io.enmasse.systemtest.Environment;
import io.enmasse.systemtest.GlobalLogCollector;
import io.enmasse.systemtest.Kubernetes;
import io.enmasse.systemtest.SystemtestsKubernetesApps;
import io.enmasse.systemtest.ability.TestWatcher;
import io.enmasse.systemtest.selenium.SeleniumProvider;
import org.junit.jupiter.api.extension.*;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class ExternalClientsExtension implements BeforeTestExecutionCallback, AfterTestExecutionCallback, BeforeAllCallback, AfterAllCallback {
    private boolean isFullClass = false;

    @Override
    public void afterAll(ExtensionContext extensionContext) throws Exception {
        SystemtestsKubernetesApps.deleteMessagingClientApp();
        isFullClass = false;
    }

    @Override
    public void beforeAll(ExtensionContext extensionContext) throws Exception {
        SystemtestsKubernetesApps.deployMessagingClientApp();
        isFullClass = true;
    }

    @Override
    public void afterTestExecution(ExtensionContext extensionContext) throws Exception {
        if (extensionContext.getExecutionException().isPresent()) {
            Path path = TestWatcher.getPath(extensionContext.getTestMethod().get(), extensionContext.getTestClass().get());
            SystemtestsKubernetesApps.collectMessagingClientAppLogs(path);
        }


        if (!isFullClass) {
            SystemtestsKubernetesApps.deleteMessagingClientApp();
        }
    }

    @Override
    public void beforeTestExecution(ExtensionContext extensionContext) throws Exception {
        if (!isFullClass) {
            SystemtestsKubernetesApps.deployMessagingClientApp();
        }
    }
}
