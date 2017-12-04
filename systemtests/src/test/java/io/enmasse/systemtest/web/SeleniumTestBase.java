package io.enmasse.systemtest.web;

import io.enmasse.systemtest.ITestMethod;
import io.enmasse.systemtest.Logging;
import io.enmasse.systemtest.TestBase;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.firefox.FirefoxOptions;

import java.util.ArrayList;
import java.util.List;

public class SeleniumTestBase extends TestBase {
    private List<WebDriver> drivers = new ArrayList<WebDriver>();

    public void tearDownDrivers() {
        for (WebDriver driver : drivers) {
            driver.close();
            try {
                driver.quit();
            } catch (Exception ignored) {
            }
        }
        drivers.clear();
        Logging.log.info("All drivers are closed");
    }

    protected WebDriver getDriver() {
        FirefoxOptions opts = new FirefoxOptions();
        opts.setHeadless(true);
        WebDriver driver = new FirefoxDriver(opts);
        drivers.add(driver);
        return driver;
    }

    protected void runSeleniumTest(ITestMethod test) throws Exception {
        try {
            test.run();
        } finally {
            tearDownDrivers();
        }
    }
}
