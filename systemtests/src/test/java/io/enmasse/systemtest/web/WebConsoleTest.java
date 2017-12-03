package io.enmasse.systemtest.web;

import com.paulhammant.ngwebdriver.ByAngular;
import com.paulhammant.ngwebdriver.NgWebDriver;
import io.enmasse.systemtest.AddressSpace;
import io.enmasse.systemtest.AddressSpaceType;
import io.enmasse.systemtest.Endpoint;
import io.enmasse.systemtest.Logging;
import org.junit.Test;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.firefox.FirefoxDriver;

import java.util.List;
import java.util.concurrent.TimeUnit;

public class WebConsoleTest extends SeleniumTestBase {

    @Test
    public void testCreateAddress() throws Exception {
        AddressSpace addressSpace = new AddressSpace("test-create-address-brokered", AddressSpaceType.BROKERED);
        createAddressSpace(addressSpace, "none");
        Endpoint consoleEndpoint = openShift.getRouteEndpoint(addressSpace.getNamespace(), "console");
        WebDriver driver = getDriver();
        driver.manage().timeouts().setScriptTimeout(30, TimeUnit.SECONDS);
        NgWebDriver angular = new NgWebDriver((FirefoxDriver)driver);

        Logging.log.info("https://" + consoleEndpoint.toString());
        driver.get("https://" + consoleEndpoint.toString());
        angular.waitForAngularRequestsToFinish();

        Logging.log.info(driver.getPageSource());
        List<WebElement> addresses = driver.findElements(ByAngular.exactRepeater("items in item"));
        addresses.get(0).click();
        angular.waitForAngular2RequestsToFinish();

        Logging.log.info(driver.getPageSource());
    }
}
