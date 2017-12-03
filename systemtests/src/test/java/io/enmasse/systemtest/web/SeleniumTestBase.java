package io.enmasse.systemtest.web;

import io.enmasse.systemtest.TestBase;
import org.junit.After;
import org.junit.Before;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.firefox.FirefoxDriver;

public class SeleniumTestBase extends TestBase {
    private WebDriver driver;

    private void buildWebDriver(){
        driver = new FirefoxDriver();
    }

    private void closeWebDriver(){
        driver.close();
    }

    @Before
    public void setUpWebConsoleTest() {
        buildWebDriver();
    }

    @After
    public void tearDownWebConsoleTest() {
        closeWebDriver();
    }

    public WebDriver getDriver() {
        return driver;
    }
}
