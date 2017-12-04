package io.enmasse.systemtest;

import com.paulhammant.ngwebdriver.ByAngular;
import com.paulhammant.ngwebdriver.NgWebDriver;
import org.junit.After;
import org.junit.Before;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.firefox.FirefoxOptions;

import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertNotNull;

public abstract class SeleniumTestBase extends TestBaseWithDefault {
    private WebDriver driver;
    private NgWebDriver angularDriver;
    private WebBrowserType browserType;
    private String baseWindowID;

    public SeleniumTestBase() {
        this.browserType = getWebBrowserType();
    }

    protected abstract WebBrowserType getWebBrowserType();

    @Before
    public void setupDriver() {
        switch (browserType) {
            case FIREFOX:
                FirefoxOptions firefoxOpts = new FirefoxOptions();
                firefoxOpts.setHeadless(true);
                this.driver = new FirefoxDriver(firefoxOpts);
                break;
            case CHROME:
                ChromeOptions chromeOpts = new ChromeOptions();
                chromeOpts.setHeadless(true);
                this.driver = new ChromeDriver(chromeOpts);
                break;
            default:
                break;
        }
        angularDriver = new NgWebDriver((JavascriptExecutor) driver);
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
        driver = null;
        angularDriver = null;
        baseWindowID = null;
    }

    protected WebDriver getDriver() {
        return this.driver;
    }

    protected String getConsoleRoute() {
        String consoleRoute = String.format("https://%s:%s@%s", username, password,
                openShift.getRouteEndpoint(defaultAddressSpace.getNamespace(), "console"));
        Logging.log.info(consoleRoute);
        return consoleRoute;
    }

    //================================== Common console methods ======================================
    protected void openConsolePageWebConsole() throws Exception {
        driver.get(getConsoleRoute());
        angularDriver.waitForAngularRequestsToFinish();
        Logging.log.info("Console page opened");
    }

    protected WebElement getLeftMenuItemWebConsole(String itemText) throws Exception {
        List<WebElement> items = driver.findElements(ByAngular.exactRepeater("item in items"));
        assertNotNull(items);
        WebElement returnedItem = null;
        for (WebElement item : items) {
            Logging.log.info("Got item: " + item.getText());
            if (item.getText().equals(itemText))
                returnedItem = item;
        }
        return returnedItem;
    }

    protected void openAddressesPageWebConsole() throws Exception {
        WebElement addressesItem = getLeftMenuItemWebConsole("Addresses");
        assertNotNull(addressesItem);
        addressesItem.click();
        Logging.log.info("Clicked on addresses button");
        angularDriver.waitForAngularRequestsToFinish();
        Logging.log.info("Page addresses loaded");
    }

    protected void switchToSubWindow() throws Exception {
        baseWindowID = driver.getWindowHandle();
        String subWindowHandler = null;
        Set<String> windows = driver.getWindowHandles();
        Iterator<String> iterator = windows.iterator();

        while (iterator.hasNext()) {
            subWindowHandler = iterator.next();
        }
        driver.switchTo().window(subWindowHandler);
        angularDriver.waitForAngularRequestsToFinish();
        Logging.log.info("Switched to sub window");
    }

    protected void createAddressWebConsole(Destination destination) throws Exception {
        //get console page
        openConsolePageWebConsole();

        //get addresses item from left panel view
        openAddressesPageWebConsole();

        //click on create button
        WebElement createButton = driver.findElement(ByAngular.buttonText("Create"));
        Logging.log.info("Get the button: " + createButton.getText());
        createButton.click();
        angularDriver.waitForAngularRequestsToFinish();
        Logging.log.info("Clicked on create button");

        //switch to sub window
        switchToSubWindow();

        //fill address name
        WebElement input = driver.findElement(By.cssSelector("#new-name"));
        input.sendKeys("test-" + destination.getType());

        //select address type
        WebElement queueInput = driver.findElement(By.id(destination.getType()));
        queueInput.click();

        //click on next button
        WebElement nextButton = driver.findElement(By.id("nextButton"));
        nextButton.click();
        angularDriver.waitForAngularRequestsToFinish();
        Logging.log.info("Next button clicked");

        //click on next button
        nextButton.click();
        angularDriver.waitForAngularRequestsToFinish();
        Logging.log.info("Next button clicked");

        //click on create button
        nextButton.click();
        angularDriver.waitForAngularRequestsToFinish();
        Logging.log.info("Create button clicked");

        //switch to base page
        driver.switchTo().window(baseWindowID);

        TestUtils.waitForDestinationsReady(addressApiClient, defaultAddressSpace,
                new TimeoutBudget(5, TimeUnit.MINUTES), destination);
    }
}
