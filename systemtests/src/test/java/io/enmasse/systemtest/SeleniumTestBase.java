package io.enmasse.systemtest;

import org.junit.After;
import org.junit.Before;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.firefox.FirefoxOptions;

public abstract class SeleniumTestBase extends TestBaseWithDefault {
    private WebDriver driver;

    @Before
    public void setupDriver() {
        FirefoxOptions opts = new FirefoxOptions();
        opts.setHeadless(true);
        this.driver = new FirefoxDriver(opts);
    }

    @After
    public void tearDownDrivers() {
        driver.close();
        try {
            driver.quit();
        } catch (Exception ex) {
            Logging.log.warn("Raise exception on quit: ");
            ex.printStackTrace();
        }
        Logging.log.info("Driver is closed");
    }

    protected WebDriver getDriver() {
        return this.driver;
    }

    @Override
    protected AddressSpaceType getAddressSpaceType() {
        return null;
    }
}
