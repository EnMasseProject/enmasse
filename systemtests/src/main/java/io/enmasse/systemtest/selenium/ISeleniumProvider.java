/*
 * Copyright 2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.systemtest.selenium;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.openqa.selenium.WebDriver;

import java.net.MalformedURLException;

public interface ISeleniumProvider {
    SeleniumProvider selenium = new SeleniumProvider();
    WebDriver buildDriver() throws MalformedURLException;

    @AfterEach
    default void tearDownWebConsoleTests(ExtensionContext context) throws Exception {
        if (context.getExecutionException().isPresent()) {
            selenium.onFailed(context);
        }
    }
}
