/*
 * Copyright 2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.systemtest.selenium;

import io.enmasse.systemtest.Environment;
import io.enmasse.systemtest.resolvers.ExtensionContextParameterResolver;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.openqa.selenium.WebDriver;

@ExtendWith(ExtensionContextParameterResolver.class)
public interface ISeleniumProvider {
    SeleniumProvider selenium = new SeleniumProvider();

    WebDriver buildDriver() throws Exception;

    @AfterEach
    default void tearDownWebConsoleTests(ExtensionContext context) throws Exception {
        if (context.getExecutionException().isPresent() || Environment.getInstance().storeScreenshots()) {
            selenium.onFailed(context);
        }
        selenium.tearDownDrivers();
    }
}
