package io.enmasse.systemtest.web;

import org.openqa.selenium.WebDriver;

public interface ISeleniumProvider {
    WebDriver buildDriver();
}
