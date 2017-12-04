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

public class WebConsoleTest extends SeleniumTestBase {

    @Test
    public void testCreateAddress() throws Exception {
        AddressSpace addressSpace = new AddressSpace("test-create-address-brokered", AddressSpaceType.BROKERED);
        createAddressSpace(addressSpace, "none");
        Endpoint consoleEndpoint = openShift.getRouteEndpoint(addressSpace.getNamespace(), "console");

        runSeleniumTest(() -> {
            WebDriver driver = getDriver();
            NgWebDriver angular = new NgWebDriver((FirefoxDriver)driver);

            Logging.log.info("https://" + consoleEndpoint.toString());
            driver.get("https://" + consoleEndpoint.toString());
            angular.waitForAngularRequestsToFinish();

            Logging.log.info("Page dashboard loaded");
            List<WebElement> addresses = driver.findElements(ByAngular.exactRepeater("item in items"));
            addresses.get(0).click();
            Logging.log.info("Clicked on addresses button");
            angular.waitForAngularRequestsToFinish();

            Logging.log.info("Page addresses loaded");
        });
    }
}
