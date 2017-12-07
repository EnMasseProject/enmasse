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
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

public abstract class SeleniumTestBase extends TestBaseWithDefault {
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd_HH:mm:ss:SSSS");
    private WebDriver driver;
    private NgWebDriver angularDriver;
    private Map<Date, File> browserScreenshots = new HashMap<>();
    private String webconsoleFolder = "selenium_tests";
    @Rule
    public TestWatcher watchman = new TestWatcher() {
        @Override
        protected void failed(Throwable e, Description description) {
            try {
                Path path = Paths.get(
                        environment.testLogDir(),
                        webconsoleFolder,
                        description.getClassName(),
                        description.getMethodName());
                Files.createDirectories(path);
                for (Date key : browserScreenshots.keySet()) {
                    FileUtils.copyFile(browserScreenshots.get(key), new File(Paths.get(path.toString(),
                            String.format("%s_%s.png", description.getDisplayName(), dateFormat.format(key))).toString()));
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
        Thread.sleep(3000);
        try {
            driver.close();
        } catch (Exception ex) {
            Logging.log.warn("Raise exception in close: " + ex.getMessage());
        }

        try {
            driver.quit();
        } catch (Exception ex) {
            Logging.log.warn("Raise warning on quit: " + ex.getMessage());
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
            browserScreenshots.put(new Date(), ((TakesScreenshot) driver).getScreenshotAs(OutputType.FILE));
        } catch (Exception ex) {
            Logging.log.warn("Cannot take screenshot: " + ex.getMessage());
        }
    }

    //================================================================================================
    //================================== Common console methods ======================================
    //================================================================================================
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
        clickOnItem(element, null);
    }

    protected void clickOnItem(WebElement element, String textToLog) throws Exception {
        takeScreenShot();
        assertNotNull(element);
        Logging.log.info("Click on button: " + (textToLog == null ? element.getText() : textToLog));
        element.click();
        angularDriver.waitForAngularRequestsToFinish();
        takeScreenShot();
    }

    protected void clickOnCreateButton() throws Exception {
        clickOnItem(driver.findElement(ByAngular.buttonText("Create")));
    }

    protected void clickOnRemoveButton() throws Exception {
        clickOnItem(driver.findElement(ByAngular.buttonText("Delete")));
    }

    protected void fillInputItem(WebElement element, String text) throws Exception {
        takeScreenShot();
        assertNotNull(element);
        element.sendKeys(text);
        angularDriver.waitForAngularRequestsToFinish();
        Logging.log.info("Filled input with text: " + text);
        takeScreenShot();
    }

    protected List<AddressWebItem> getAddressItems() {
        WebElement content = driver.findElement(By.id("contentContainer"));
        List<WebElement> elements = content.findElements(By.className("list-group-item"));
        List<AddressWebItem> addressItems = new ArrayList<>();
        for (WebElement element : elements) {
            AddressWebItem item = new AddressWebItem(element);
            Logging.log.info("Got address: " + item.getName());
            addressItems.add(item);
        }
        return addressItems;
    }

    protected AddressWebItem getAddressItem(Destination destination) throws Exception {
        AddressWebItem returnedElement = null;
        List<AddressWebItem> addressWebItems = getAddressItems();
        for (AddressWebItem item : addressWebItems) {
            if (item.getName().equals(destination.getAddress()))
                returnedElement = item;
        }
        return returnedElement;
    }

    protected void createAddressWebConsole(Destination destination) throws Exception {
        //get console page
        openConsolePageWebConsole();

        //get addresses item from left panel view
        openAddressesPageWebConsole();

        //click on create button
        clickOnCreateButton();

        //fill address name
        fillInputItem(driver.findElement(By.id("new-name")), "test-" + destination.getType());

        //select address type
        clickOnItem(driver.findElement(By.id(destination.getType())), "Radio button " + destination.getType());

        WebElement nextButton = driver.findElement(By.id("nextButton"));
        clickOnItem(nextButton);
        clickOnItem(nextButton);
        clickOnItem(nextButton);

        assertNotNull(getAddressItem(destination));

        TestUtils.waitForDestinationsReady(addressApiClient, defaultAddressSpace,
                new TimeoutBudget(5, TimeUnit.MINUTES), destination);
    }

    protected void deleteAddressWebConsole(Destination destination) throws Exception {
        //open console webpage
        openConsolePageWebConsole();

        //open addresses
        openAddressesPageWebConsole();

        //click on check box
        clickOnItem(getAddressItem(destination).getCheckBox(), "check box: " + destination.getAddress());

        //click on delete
        clickOnRemoveButton();

        //check if address deleted
        assertNull(getAddressItem(destination));
    }

    protected class AddressWebItem {
        private WebElement addressItem;
        private WebElement checkBox;
        private boolean isReady;
        private String name;

        public AddressWebItem(WebElement item) {
            this.addressItem = item;
            this.checkBox = item.findElement(By.className("list-view-pf-checkbox"));
            this.name = item.findElement(By.className("list-group-item-heading")).getText();
            try {
                item.findElement(By.className("pficon-ok"));
                isReady = true;
            } catch (Exception ex) {
                isReady = false;
            }
        }

        public WebElement getAddressItem() {
            return addressItem;
        }

        public WebElement getCheckBox() {
            return checkBox;
        }

        public boolean getIsReady() {
            return isReady;
        }

        public String getName() {
            return name;
        }
    }
}
