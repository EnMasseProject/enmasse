package io.enmasse.systemtest.TestCollection.web;

import org.openqa.selenium.WebDriver;

public interface ISeleniumProvider {
    WebDriver buildDriver();
}
