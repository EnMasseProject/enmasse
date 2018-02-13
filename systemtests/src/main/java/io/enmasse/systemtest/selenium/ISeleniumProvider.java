/*
 * Copyright 2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.systemtest.selenium;

import org.openqa.selenium.WebDriver;

public interface ISeleniumProvider {
    WebDriver buildDriver();
}
