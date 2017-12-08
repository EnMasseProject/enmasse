package io.enmasse.systemtest;

import com.paulhammant.ngwebdriver.ByAngular;
import com.paulhammant.ngwebdriver.NgWebDriver;
import io.enmasse.systemtest.web.FilterType;
import org.apache.commons.io.FileUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.rules.TestWatcher;
import org.junit.runner.Description;
import org.openqa.selenium.*;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

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
    private WebDriverWait driverWait;
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
        driverWait = new WebDriverWait(driver, 10);
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
        driverWait = null;
    }

    protected WebDriver getDriver() {
        return this.driver;
    }

    protected NgWebDriver getAngularDriver() {
        return this.angularDriver;
    }

    protected WebDriverWait getDriverWait() {
        return driverWait;
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

    private WebElement getLeftMenuItemWebConsole(String itemText) throws Exception {
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

    protected void pressEnter(WebElement element) throws Exception {
        takeScreenShot();
        assertNotNull(element);
        element.sendKeys(Keys.RETURN);
        angularDriver.waitForAngularRequestsToFinish();
        Logging.log.info("Enter pressed");
        takeScreenShot();
    }

    private WebElement getContentContainer() throws Exception {
        return driver.findElement(By.id("contentContainer"));
    }

    private WebElement getToolbar() throws Exception {
        return driver.findElement(By.id("exampleToolbar"));
    }

    protected WebElement getFilterGroup() throws Exception {
        WebElement toolbar = getToolbar();
        return toolbar.findElement(By.id("_fields"));
    }

    private WebElement getFilterSwitch() throws Exception {
        return getFilterGroup().findElements(By.tagName("button")).get(0);
    }

    private WebElement getTypeSwitch() throws Exception {
        return getFilterGroup().findElements(By.tagName("button")).get(1);
    }

    private List<WebElement> getFilterDropDown() throws Exception {
        return getFilterGroup().findElements(ByAngular.repeater("item in config.fields"));
    }

    /**
     * get list of li elements from dropdown-menu with allowed types of addresses (queue, topic, multicast, anycast)
     *
     * @return
     * @throws Exception
     */
    private List<WebElement> getDropDownTypes() throws Exception {
        return getFilterGroup().findElements(By.className("dropdown-menu inner")).get(0).findElements(By.tagName("li"));
    }

    private WebElement getDropDownAddressType(String addressType, List<WebElement> dropDownTypes) {
        switch (addressType) {
            case "queue":
                return dropDownTypes.get(1);
            case "topic":
                return dropDownTypes.get(2);
            case "multicast":
                return dropDownTypes.get(3);
            case "anycast":
                return dropDownTypes.get(4);
            default:
                throw new IllegalStateException(String.format("Address type '%s'doesn't exist", addressType));
        }
    }

    /**
     * switch type of filtering
     *
     * @param filterType type, name
     * @throws Exception
     */
    protected void switchFilter(String filterType) throws Exception {
        WebElement switchButton = getFilterSwitch();
        clickOnItem(switchButton);
        for (WebElement element : getFilterDropDown()) {
            if (element.findElement(By.tagName("a")).getText().toUpperCase().equals(filterType.toUpperCase())) {
                clickOnItem(element);
                break;
            }
        }
    }


    /**
     * add whatever filter you want
     *
     * @param filterType
     * @param filterValue allowed values are for FilterType.NAME (String), FilterType.NAME (queue, topic, multicast, anycast)
     * @throws Exception
     */
    protected void addFilter(FilterType filterType, String filterValue) throws Exception {
        switchFilter(filterType.toString());
        switch (filterType) {
            case TYPE:
                addFilterByType(filterValue);
                break;
            case NAME:
                WebElement filterInput = getFilterGroup().findElement(By.tagName("input"));
                fillInputItem(filterInput, filterValue);
                pressEnter(filterInput);
                break;
        }
    }

    /**
     * add filter by address type
     *
     * @param addressType queue, topic, multicast, anycast
     * @throws Exception
     */
    protected void addFilterByType(String addressType) throws Exception {
        WebElement switchButton = getFilterSwitch();
        clickOnItem(switchButton);
        WebElement addressTypeElement = getDropDownAddressType(addressType, getDropDownTypes());
        clickOnItem(addressTypeElement);
    }

    private WebElement getFilterResultsToolbar() throws Exception {
        return getToolbar().findElement(By.id("{{filterDomId}_results}"));
    }

    private void removeFilter(String filterType, String filterName) throws Exception {
        String filterText = String.format("%s: %s", filterType, filterName);
        List<WebElement> filters = getFilterResultsToolbar().findElements(ByAngular.repeater("filter in config.appliedFilters"));
        for (WebElement filter : filters) {
            if(filterText.toUpperCase().equals(filter.findElement(By.className("active-filter")).getText().toUpperCase())) {
                WebElement button = filter.findElement(By.className("pficon-close"));
                clickOnItem(button, "clearFilterButton");
            }
        }
    }

    protected void removeFilterByType(String filterName) throws Exception {
        removeFilter("type", filterName);
    }

    protected void removeFilterByName(String filterName) throws Exception {
        removeFilter("name", filterName);
    }

    protected void clearAllFilters() throws Exception {
        WebElement clearAllButton = getFilterResultsToolbar().findElement(By.className("clear-filters"));
        clickOnItem(clearAllButton);
    }

    protected List<AddressWebItem> getAddressItems() throws Exception {
        WebElement content = getContentContainer();
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

    protected void createAddressesWebConsole(Destination... destinations) throws Exception {
        for (Destination dest : destinations) {
            createAddressWebConsole(dest);
        }
    }

    protected void createAddressWebConsole(Destination destination) throws Exception {
        //get console page
        openConsolePageWebConsole();

        //get addresses item from left panel view
        openAddressesPageWebConsole();

        //click on create button
        clickOnCreateButton();

        //fill address name
        fillInputItem(driver.findElement(By.id("new-name")), destination.getAddress());

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

    protected void deleteAddressesWebConsole(Destination... destinations) throws Exception {
        for (Destination dest : destinations) {
            deleteAddressWebConsole(dest);
        }
    }

    protected void deleteAddressWebConsole(Destination destination) throws Exception {
        //open console webpage
        openConsolePageWebConsole();

        //open addresses
        openAddressesPageWebConsole();

        AddressWebItem addressItem = getAddressItem(destination);

        //click on check box
        clickOnItem(addressItem.getCheckBox(), "check box: " + destination.getAddress());

        //click on delete
        clickOnRemoveButton();

        //check if address deleted
        driverWait.until(ExpectedConditions.invisibilityOf(addressItem.getAddressItem()));
        assertNull(getAddressItem(destination));
    }

    public class AddressWebItem {
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
