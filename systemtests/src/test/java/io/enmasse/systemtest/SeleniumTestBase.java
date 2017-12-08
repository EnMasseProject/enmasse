package io.enmasse.systemtest;

import com.paulhammant.ngwebdriver.ByAngular;
import com.paulhammant.ngwebdriver.NgWebDriver;
import io.enmasse.systemtest.web.AddressWebItem;
import io.enmasse.systemtest.web.FilterType;
import io.enmasse.systemtest.web.SortType;
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

    /**
     * get toolbar element with all filters
     */
    private WebElement getFilterResultsToolbar() throws Exception {
        return getToolbar().findElement(By.id("{{filterDomId}_results}"));
    }

    /**
     * get element with toolbar and all addresses
     */
    private WebElement getContentContainer() throws Exception {
        return driver.findElement(By.id("contentContainer"));
    }

    /**
     * get element with filter, sort, create, delete, ...
     */
    private WebElement getToolbar() throws Exception {
        return driver.findElement(By.id("exampleToolbar"));
    }

    /**
     * get element from toolbar with Filter elements
     */
    private WebElement getFilterGroup() throws Exception {
        WebElement toolbar = getToolbar();
        return toolbar.findElement(By.id("_fields"));
    }

    /**
     * get button element with filter types (Type/Name)
     */
    private WebElement getFilterSwitch() throws Exception {
        return getFilterGroup().findElements(By.tagName("button")).get(0);
    }

    /**
     * get button element with address types (Filter by type.../queue/topic/multicast/anycast)
     */
    private WebElement getAddressTypeSwitch() throws Exception {
        return getFilterGroup().findElements(By.tagName("button")).get(1);
    }

    /**
     * get list of clickable li elements (Type/Name)
     */
    private List<WebElement> getFilterDropDown() throws Exception {
        return getFilterGroup().findElements(ByAngular.repeater("item in config.fields"));
    }

    /**
     * get list of clickable li elements (Name/Senders/Receivers)
     */
    private List<WebElement> getSortDropDown() throws Exception {
        return getSortGroup().findElements(ByAngular.repeater("item in config.fields"));
    }

    /**
     * get list of li elements from dropdown-menu with allowed types of addresses (queue, topic, multicast, anycast)
     */
    private List<WebElement> getDropDownAddressTypes() throws Exception {
        return getFilterGroup().findElements(By.className("dropdown-menu inner")).get(0).findElements(By.tagName("li"));
    }

    /**
     * get clickable element from list of address types
     * (DropDown element with: Filter by type.../queue/topic/multicast/anycast values)
     */
    private WebElement getDropDownAddressType(String addressType, List<WebElement> dropDownTypes) {
        HashMap<String, Integer> addressTypesMap = new HashMap<>();
        addressTypesMap.put("queue", 1);
        addressTypesMap.put("topic", 2);
        addressTypesMap.put("multicast", 3);
        addressTypesMap.put("anycast", 4);

        return dropDownTypes.get(addressTypesMap.get(addressType)).findElements(By.tagName("a")).get(0);
    }

    /**
     * return part of toolbar with sort buttons
     */
    private WebElement getSortGroup() throws Exception {
        return getToolbar().findElement(By.className("sort-pf"));
    }

    /**
     * get sort switch container
     */
    private WebElement getSortSwitch() throws Exception {
        return getSortGroup().findElement(By.className("dropdown")).findElement(By.tagName("button"));
    }

    /**
     * get sort asc/desc button
     */
    private WebElement getAscDescButton() throws Exception {
        return getSortGroup().findElement(By.className("btn-link"));
    }

    /**
     * common method for switching type of filtering/sorting
     */
    private void switchFilterOrSort(Enum switchElement, WebElement switchButton, List<WebElement> switchElements) throws Exception {
        clickOnItem(switchButton);
        for (WebElement element : switchElements) {
            if (element.findElement(By.tagName("a")).getText().toUpperCase().equals(switchElement.toString())) {
                clickOnItem(element);
                break;
            }
        }
    }

    /**
     * switch type of sorting Name/Senders/Receivers
     */
    private void switchSort(SortType sortType) throws Exception {
        switchFilterOrSort(sortType, getSortSwitch(), getSortDropDown());
    }

    /**
     * switch type of filtering Name/Type
     */
    private void switchFilter(FilterType filterType) throws Exception {
        switchFilterOrSort(filterType, getFilterSwitch(), getFilterDropDown());
    }

    /**
     * check if sorted ASC/DESC button is set to ASC.
     */
    private boolean isSortAsc() {
        Boolean isAsc;
        try{
            getAscDescButton().findElement(By.className("fa-sort-alpha-asc"));
            isAsc = true;
        }catch (Exception ex) {
            isAsc = false;
        }

        if(!isAsc) {
            try {
                getAscDescButton().findElement(By.className("fa-sort-numeric-asc"));
                isAsc = true;
            } catch (Exception ex) {
                isAsc = false;
            }
        }
        return isAsc;
    }

    /**
     * add whatever filter you want
     *
     * @param filterType
     * @param filterValue allowed values are for FilterType.NAME (String), FilterType.NAME (queue, topic, multicast, anycast)
     * @throws Exception
     */
    protected void addFilter(FilterType filterType, String filterValue) throws Exception {
        switchFilter(filterType);
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
     */
    protected void addFilterByType(String addressType) throws Exception {
        WebElement switchAddressTypeButton = getAddressTypeSwitch();
        clickOnItem(switchAddressTypeButton);
        WebElement addressTypeElement = getDropDownAddressType(addressType, getDropDownAddressTypes());
        clickOnItem(addressTypeElement);
    }


    /**
     * remove filter element by (Name: Value)
     */
    private void removeFilter(FilterType filterType, String filterName) throws Exception {
        String filterText = String.format("%s: %s", filterType.toString().toLowerCase(), filterName);
        List<WebElement> filters = getFilterResultsToolbar().findElements(ByAngular.repeater("filter in config.appliedFilters"));
        for (WebElement filter : filters) {
            if (filterText.toUpperCase().equals(filter.findElement(By.className("active-filter")).getText().toUpperCase())) {
                WebElement button = filter.findElement(By.className("pficon-close"));
                clickOnItem(button, "clearFilterButton");
            }
        }
    }

    /**
     * remove 'type' filter element by (Name: Value)
     */
    protected void removeFilterByType(String filterName) throws Exception {
        removeFilter(FilterType.TYPE, filterName);
    }

    /**
     * remove 'name' filter element by (Name: Value)
     */
    protected void removeFilterByName(String filterName) throws Exception {
        removeFilter(FilterType.NAME, filterName);
    }

    /**
     * remove all filters elements
     */
    protected void clearAllFilters() throws Exception {
        WebElement clearAllButton = getFilterResultsToolbar().findElement(By.className("clear-filters"));
        clickOnItem(clearAllButton);
    }

    /**
     * Sort address items
     */
    protected void sortItems(SortType sortType, boolean asc) throws Exception {
        switchSort(sortType);
        if(asc && !isSortAsc()) {
            clickOnItem(getAscDescButton());
        } else if (!asc && isSortAsc()) {
            clickOnItem(getAscDescButton());
        }
    }

    /**
     * get all addresses
     */
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

    /**
     * get specific address
     */
    protected AddressWebItem getAddressItem(Destination destination) throws Exception {
        AddressWebItem returnedElement = null;
        List<AddressWebItem> addressWebItems = getAddressItems();
        for (AddressWebItem item : addressWebItems) {
            if (item.getName().equals(destination.getAddress()))
                returnedElement = item;
        }
        return returnedElement;
    }

    /**
     * create multiple addresses
     */
    protected void createAddressesWebConsole(Destination... destinations) throws Exception {
        for (Destination dest : destinations) {
            createAddressWebConsole(dest);
        }
    }

    /**
     * create specific address
     */
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

    /**
     * delete multiple addresses
     */
    protected void deleteAddressesWebConsole(Destination... destinations) throws Exception {
        for (Destination dest : destinations) {
            deleteAddressWebConsole(dest);
        }
    }

    /**
     * delete specific addresses
     */
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
}
