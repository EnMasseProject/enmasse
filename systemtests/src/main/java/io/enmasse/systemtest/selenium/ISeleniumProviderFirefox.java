/*
 * Copyright 2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.systemtest.selenium;

import io.enmasse.systemtest.TestUtils;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.openqa.selenium.WebDriver;

public interface ISeleniumProviderFirefox extends ISeleniumProvider {
    @Override
    default WebDriver buildDriver() throws Exception {
        return TestUtils.getFirefoxDriver();
    }

    @BeforeAll
    default void deployContainer() {
        SeleniumContainers.deployFirefoxContainer();
    }

    @AfterAll
    default void removeContainers() {
        selenium.tearDownDrivers();
        SeleniumContainers.stopAndRemoveFirefoxContainer();
    }
}
