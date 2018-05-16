/*
 * Copyright 2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.systemtest.selenium;

import io.enmasse.systemtest.TestUtils;
import org.openqa.selenium.WebDriver;

import java.net.MalformedURLException;

public interface ISeleniumProviderFirefox extends ISeleniumProvider {

    @Override
    default WebDriver buildDriver() throws MalformedURLException {
        return TestUtils.getFirefoxDriver();
    }
}
