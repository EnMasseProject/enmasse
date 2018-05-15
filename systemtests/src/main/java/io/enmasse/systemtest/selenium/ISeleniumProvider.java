/*
 * Copyright 2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.systemtest.selenium;

import io.enmasse.systemtest.Environment;
import io.enmasse.systemtest.resolvers.EnvironmentParameterResolver;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.openqa.selenium.WebDriver;

@ExtendWith(EnvironmentParameterResolver.class)
public interface ISeleniumProvider {
    SeleniumProvider selenium = new SeleniumProvider();

    WebDriver buildDriver();

    @AfterEach
    default void tearDownWebConsoleTests(ExtensionContext context, Environment env) {
        if (context.getExecutionException().isPresent() || env.storeScreenshots()) {
            selenium.onFailed(context);
        }
    }
}
