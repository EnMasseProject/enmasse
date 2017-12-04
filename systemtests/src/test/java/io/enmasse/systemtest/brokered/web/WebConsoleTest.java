package io.enmasse.systemtest.brokered.web;

import com.paulhammant.ngwebdriver.ByAngular;
import com.paulhammant.ngwebdriver.NgWebDriver;
import io.enmasse.systemtest.BrokeredSeleniumTestBase;
import io.enmasse.systemtest.Endpoint;
import io.enmasse.systemtest.Logging;
import org.junit.Test;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.firefox.FirefoxDriver;

import java.util.List;

public class WebConsoleTest extends BrokeredSeleniumTestBase {

    @Test
    public void testCreateAddress() throws Exception {
        Endpoint consoleEndpoint = openShift.getRouteEndpoint(defaultAddressSpace.getNamespace(), "console");

        WebDriver driver = getDriver();
        NgWebDriver angular = new NgWebDriver((FirefoxDriver) driver);

        Logging.log.info("https://" + consoleEndpoint.toString());
        driver.get("https://test:test@" + consoleEndpoint.toString());
        angular.waitForAngularRequestsToFinish();

        Logging.log.info("Page dashboard loaded");
        List<WebElement> addresses = driver.findElements(ByAngular.exactRepeater("item in items"));
        for(WebElement item : addresses){
            Logging.log.info("Item: " + item.getText());
        }
        addresses.get(1).click();
        Logging.log.info("Clicked on addresses button");
        angular.waitForAngularRequestsToFinish();

        Logging.log.info("Page addresses loaded");
    }
}
