/*
 * Copyright 2019, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.systemtest.selenium;

import io.enmasse.systemtest.Environment;
import io.enmasse.systemtest.utils.TestUtils;
import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.AfterTestExecutionCallback;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.BeforeTestExecutionCallback;
import org.junit.jupiter.api.extension.ExtensionContext;

public class SeleniumChromeExtension implements BeforeTestExecutionCallback,
        AfterTestExecutionCallback, BeforeAllCallback, AfterAllCallback {
    private boolean isFullClass = false;

    @Override
    public void afterAll(ExtensionContext extensionContext) {
        SeleniumProvider.getInstance().tearDownDrivers();
        SeleniumManagement.removeChromeApp();
        isFullClass = false;
    }

    @Override
    public void beforeAll(ExtensionContext extensionContext) throws Exception {
        SeleniumManagement.deployChromeApp();
        isFullClass = true;
    }

    @Override
    public void afterTestExecution(ExtensionContext extensionContext) {
        if (extensionContext.getExecutionException().isPresent() || Environment.getInstance().storeScreenshots()) {
            SeleniumProvider.getInstance().onFailed(extensionContext);
        }
        SeleniumProvider.getInstance().tearDownDrivers();
        if (!isFullClass) {
            SeleniumManagement.removeChromeApp();
        } else {
            SeleniumManagement.restartSeleniumApp();
        }
    }

    @Override
    public void beforeTestExecution(ExtensionContext extensionContext) throws Exception {
        if (!isFullClass) {
            SeleniumManagement.deployChromeApp();
        }
        if (SeleniumProvider.getInstance().getDriver() == null) {
            SeleniumProvider.getInstance().setupDriver(TestUtils.getChromeDriver());
        } else {
            SeleniumProvider.getInstance().clearScreenShots();
        }
    }
}
