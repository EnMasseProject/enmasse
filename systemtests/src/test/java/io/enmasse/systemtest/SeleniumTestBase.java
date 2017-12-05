package io.enmasse.systemtest;

import com.paulhammant.ngwebdriver.ByAngular;
import com.paulhammant.ngwebdriver.NgWebDriver;
import org.apache.commons.io.FileUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.rules.TestWatcher;
import org.junit.runner.Description;
import org.openqa.selenium.*;

import java.io.File;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertNotNull;

public abstract class SeleniumTestBase extends TestBaseWithDefault {
    private WebDriver driver;
    private NgWebDriver angularDriver;
    private List<File> browserScreenshots = new ArrayList<>();
    @Rule
    public TestWatcher watchman = new TestWatcher() {
        @Override
        protected void failed(Throwable e, Description description) {
            try {
                for (int i = 0; i < browserScreenshots.size(); i++) {
                    FileUtils.copyFile(browserScreenshots.get(i),
                            new File(Paths.get(environment.testLogDir(),
                                    String.format("%s_%d.png", description.getDisplayName(), i)).toString()));
                }
            } catch (Exception ex) {
                Logging.log.warn("Cannot save screenshots: " + ex.getMessage());
            }
        }
    };

    protected abstract WebDriver buildDriver();

    @Before
    public void setupDriver() throws Exception {
        driver = buildDriver();
        angularDriver = new NgWebDriver((JavascriptExecutor) driver);
        browserScreenshots.clear();
    }

    @After
    public void tearDownDrivers() throws Exception {
        takeScreenShot();
        Thread.sleep(5000);
        try {
            driver.close();
            //driver.quit();
        } catch (Exception ex) {
            Logging.log.warn("Raise exception on quit: " + ex.getMessage());
        }
        Logging.log.info("Driver is closed");
        driver = null;
        angularDriver = null;
    }

    protected WebDriver getDriver() {
        return this.driver;
    }

    protected NgWebDriver getAngularDriver() {
        return this.angularDriver;
    }

    protected String getConsoleRoute() {
        String consoleRoute = String.format("https://%s:%s@%s", username, password,
                openShift.getRouteEndpoint(defaultAddressSpace.getNamespace(), "console"));
        Logging.log.info(consoleRoute);
        return consoleRoute;
    }

    protected void takeScreenShot() {
        try {
            browserScreenshots.add(((TakesScreenshot) driver).getScreenshotAs(OutputType.FILE));
        } catch (Exception ex) {
            Logging.log.warn("Cannot take screenshot: " + ex.getMessage());
        }
    }

    //================================== Common console methods ======================================
    protected void openConsolePageWebConsole() throws Exception {
        driver.get(getConsoleRoute());
        angularDriver.waitForAngularRequestsToFinish();
        Logging.log.info("Console page opened");
        takeScreenShot();
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
        clickOnItem(getLeftMenuItemWebConsole("Addresses"));
    }

    protected void openDashboardPageWebConsole() throws Exception {
        clickOnItem(getLeftMenuItemWebConsole("Dashboard"));
    }

    protected void openConnectionsPageWebConsole() throws Exception {
        clickOnItem(getLeftMenuItemWebConsole("Connections"));
    }

    protected void clickOnItem(WebElement element) throws Exception {
        takeScreenShot();
        assertNotNull(element);
        Logging.log.info("Click on button: " + element.getText());
        element.click();
        angularDriver.waitForAngularRequestsToFinish();
        takeScreenShot();
    }

    protected void fillInputItem(WebElement element, String text) throws Exception {
        takeScreenShot();
        assertNotNull(element);
        element.sendKeys(text);
        angularDriver.waitForAngularRequestsToFinish();
        Logging.log.info("Filled input with text: " + text);
        takeScreenShot();
    }

    protected void createAddressWebConsole(Destination destination) throws Exception {
        //get console page
        openConsolePageWebConsole();

        //get addresses item from left panel view
        openAddressesPageWebConsole();

        //click on create button
        clickOnItem(driver.findElement(ByAngular.buttonText("Create")));

        //fill address name
        fillInputItem(driver.findElement(By.cssSelector("#new-name")), "test-" + destination.getType());

        //select address type
        clickOnItem(driver.findElement(By.id(destination.getType())));

        WebElement nextButton = driver.findElement(By.id("nextButton"));
        clickOnItem(nextButton);
        clickOnItem(nextButton);
        clickOnItem(nextButton);

        TestUtils.waitForDestinationsReady(addressApiClient, defaultAddressSpace,
                new TimeoutBudget(5, TimeUnit.MINUTES), destination);
    }
}
