package io.enmasse.systemtest.web;

import com.paulhammant.ngwebdriver.ByAngular;
import com.paulhammant.ngwebdriver.NgWebDriver;
import io.enmasse.systemtest.AddressSpace;
import io.enmasse.systemtest.AddressSpaceType;
import io.enmasse.systemtest.Endpoint;
import io.enmasse.systemtest.Logging;
import org.junit.Test;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.htmlunit.HtmlUnitDriver;

import java.util.concurrent.TimeUnit;

public class WebConsoleTest extends SeleniumTestBase {

    @Test
    public void testCreateAddress() throws Exception {
        AddressSpace addressSpace = new AddressSpace("test-create-address-brokered", AddressSpaceType.BROKERED);
        createAddressSpace(addressSpace, "none");
        Endpoint consoleEndpoint = openShift.getRouteEndpoint(addressSpace.getNamespace(), "console");
        HtmlUnitDriver driver = getDriver();
        driver.setJavascriptEnabled(true);
        driver.manage().timeouts().setScriptTimeout(30, TimeUnit.SECONDS);
        driver.setAcceptSslCertificates(true);
        NgWebDriver angular = new NgWebDriver(driver);

        Logging.log.info("https://" + consoleEndpoint.toString());
        driver.get("https://" + consoleEndpoint.toString());
        angular.waitForAngularRequestsToFinish();

        Logging.log.info(driver.getPageSource());
        WebElement addresses = driver.findElementByCssSelector("li.list-group-item:nth-child(2)");
        addresses.click();
        angular.waitForAngular2RequestsToFinish();

        Logging.log.info(driver.getPageSource());
    }
}
