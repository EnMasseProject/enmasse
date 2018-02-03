package io.enmasse.systemtest.selenium;

import org.openqa.selenium.WebDriver;

public interface ISeleniumProvider {
    WebDriver buildDriver();
}
