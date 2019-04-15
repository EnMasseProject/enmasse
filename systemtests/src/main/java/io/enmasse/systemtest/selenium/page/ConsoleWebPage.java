/*
 * Copyright 2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.systemtest.selenium.page;


import com.paulhammant.ngwebdriver.ByAngular;
import io.enmasse.address.model.Address;
import io.enmasse.address.model.AddressSpace;
import io.enmasse.admin.model.v1.AuthenticationServiceType;
import io.enmasse.systemtest.*;
import io.enmasse.systemtest.apiclients.AddressApiClient;
import io.enmasse.systemtest.selenium.SeleniumProvider;
import io.enmasse.systemtest.selenium.resources.*;
import io.enmasse.systemtest.utils.AddressUtils;
import io.enmasse.systemtest.utils.TestUtils;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.Select;
import org.slf4j.Logger;

import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

public class ConsoleWebPage implements IWebPage {

    private static Logger log = CustomLogger.getLogger();
    private SeleniumProvider selenium;
    private String consoleRoute;
    private AddressSpace defaultAddressSpace;
    private ToolbarType toolbarType;
    private UserCredentials credentials;
    private GlobalConsolePage globalConsole;

    public ConsoleWebPage(SeleniumProvider selenium, AddressSpace defaultAddressSpace) throws Exception {
        this.selenium = selenium;
        this.defaultAddressSpace = defaultAddressSpace;
        this.globalConsole = new GlobalConsolePage(selenium, TestUtils.getGlobalConsoleRoute(), null);
    }

    public ConsoleWebPage(SeleniumProvider selenium, AddressSpace defaultAddressSpace, UserCredentials credentials) throws Exception {
        this.selenium = selenium;
        this.defaultAddressSpace = defaultAddressSpace;
        this.credentials = credentials;
        this.globalConsole = new GlobalConsolePage(selenium, TestUtils.getGlobalConsoleRoute(), credentials);
    }

    public ConsoleWebPage(SeleniumProvider selenium, String consoleRoute, AddressSpace defaultAddressSpace, UserCredentials credentials) throws Exception {
        this(selenium, defaultAddressSpace);
        this.consoleRoute = consoleRoute;
        this.credentials = credentials;
        this.globalConsole = new GlobalConsolePage(selenium, TestUtils.getGlobalConsoleRoute(), credentials);
    }

    //================================================================================================
    // Getters and finders of elements and data
    //================================================================================================

    private WebElement getNavigateMenu() throws Exception {
        selenium.getDriverWait().withTimeout(Duration.ofSeconds(30)).until(ExpectedConditions.presenceOfElementLocated(By.className("nav-pf-vertical")));
        return selenium.getDriver().findElement(By.className("nav-pf-vertical"));
    }

    private WebElement getLeftMenuItemWebConsole(String itemText) throws Exception {
        log.info("Getting navigation menu items");
        List<WebElement> items = getNavigateMenu()
                .findElement(By.className("list-group"))
                .findElements(ByAngular.repeater("item in items"));
        assertNotNull(items, "Console failed, does not contain left menu items");
        WebElement returnedItem = null;
        for (WebElement item : items) {
            log.info("Got item: " + item.getText());
            if (item.getText().equals(itemText))
                returnedItem = item;
        }
        return returnedItem;
    }

    public WebElement getCreateButton() throws Exception {
        return selenium.getWebElement(() -> selenium.getDriver().findElement(ByAngular.buttonText("Create")));
    }

    public WebElement getRemoveButton() throws Exception {
        return selenium.getWebElement(() -> selenium.getDriver().findElement(ByAngular.buttonText("Delete")));
    }

    /**
     * get toolbar element with all filters for addresses/connections
     */
    private WebElement getFilterResultsToolbar() throws Exception {
        return getToolbar().findElement(By.id("{{filterDomId}_results}"));
    }

    /**
     * (addresses/connections tab)
     * get element with toolbar and all addresses/connections
     */
    private WebElement getContentContainer() {
        return selenium.getDriver().findElement(By.id("contentContainer"));
    }

    /**
     * (addresses/connections tab)
     * get toolbar with filter/sort
     */
    private WebElement getToolbar() {
        return selenium.getDriver().findElement(By.id(toolbarType.toString()));
    }

    /**
     * (addresses/connections tab)
     * get element from toolbar with Filter elements
     */
    private WebElement getFilterGroup() {
        return getToolbar().findElement(By.id("_fields"));
    }

    /**
     * (addresses/connections tab)
     * get button element with filter types * (Type/Name) for addresses
     * or
     * (Container/Hostname/User/Encrypted) for connections
     */
    private WebElement getFilterSwitch() {
        return getFilterGroup().findElements(By.tagName("button")).get(0);
    }

    /**
     * (addresses/connections tab)
     * get button element with (address types: [Filter by type.../queue/topic/multicast/anycast])
     * or
     * (connections encrypted types: [Filter by encrypted/unencrypted/encrypted/unencrypted])
     */
    private WebElement getAddressConnectionsSwitch() {
        return getFilterGroup().findElements(By.tagName("button")).get(1);
    }

    /**
     * (addresses/connections tab)
     * get input element for search with set filter to (addresses:[Name]) or (connections:[Container/Hostname/User])
     */
    private WebElement getInputSearchAddressesConnections() {
        return getFilterGroup().findElement(By.tagName("input"));
    }

    /**
     * (addresses/connections tab)
     * get list of clickable li elements (addresses: [Type/Name]) or (connections: [Container/Hostname/User/Encrypted])
     */
    private List<WebElement> getFilterDropDown() {
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
        for (WebElement el : getFilterGroup().findElements(By.className("dropdown-menu"))) {
            List<WebElement> subEl = el.findElements(By.className("inner"));
            if (subEl.size() > 0) {
                List<WebElement> liElements = subEl.get(0).findElements(By.tagName("li"));
                liElements.forEach(liEl -> {
                    log.info("Got item: {}",
                            liEl.findElement(By.tagName("a")).findElement(By.tagName("span")).getText());
                });
                return liElements;
            }
        }
        throw new IllegalStateException("dropdown-menu doesn't exist");
    }

    /**
     * (addresss tab)
     * get clickable element from list of address types
     * (DropDown element with: Filter by type.../queue/topic/multicast/anycast values)
     */
    private WebElement getDropDownAddressType(String addressType, List<WebElement> dropDownTypes) {
        HashMap<String, Integer> addressTypesMap = new HashMap<>();
        addressTypesMap.put("queue", 1);
        addressTypesMap.put("topic", 2);
        addressTypesMap.put("multicast", 3);
        addressTypesMap.put("anycast", 4);

        return dropDownTypes.get(addressTypesMap.get(addressType));
    }

    /**
     * (connections tab)
     * get clickable element from list of connections by encrypted
     * (DropDown element with: Filter by encrypted/unencrypted.../encrypted/unencrypted values)
     */
    private WebElement getDropDownEncryptedType(String encryptedType, List<WebElement> dropDownTypes) {
        HashMap<String, Integer> addressTypesMap = new HashMap<>();
        addressTypesMap.put("encrypted", 1);
        addressTypesMap.put("unencrypted", 2);

        return dropDownTypes.get(addressTypesMap.get(encryptedType));
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

    private WebElement getRightDropDownMenus() throws Exception {
        return selenium.getDriver().findElement(By.className("navbar-right"));
    }

    private WebElement getHelpDropDown() throws Exception {
        return getRightDropDownMenus().findElements(By.className("dropdown")).get(1);
    }

    private WebElement getUserDropDown() throws Exception {
        return getRightDropDownMenus().findElements(By.className("dropdown")).get(2);
    }

    private WebElement getLogoutHref() throws Exception {
        log.info("Getting logout link");
        return getUserDropDown().findElement(By.id("globalconsole"));
    }

    public Integer getResultsCount() throws Exception {
        String resultsString = getFilterResultsToolbar()
                .findElement(By.className("col-sm-12"))
                .findElement(By.className("ng-binding"))
                .getText();
        String[] split = resultsString.split(" ");
        if (split.length == 2) {
            return Integer.valueOf(split[0]);
        }
        throw new IllegalStateException("Incorrect format of results count, Expected: \"Integer 'Results''\"");
    }

    /**
     * get all addresses
     */
    public List<AddressWebItem> getAddressItems() {
        WebElement content = getContentContainer();
        List<WebElement> elements = content.findElements(By.className("list-group-item"));
        List<AddressWebItem> addressItems = new ArrayList<>();
        for (WebElement element : elements) {
            AddressWebItem item = new AddressWebItem(element);
            log.info(String.format("Got address: %s", item.toString()));
            addressItems.add(item);
        }
        return addressItems;
    }

    /**
     * get specific address
     */
    public AddressWebItem getAddressItem(Address destination) {
        AddressWebItem returnedElement = null;
        List<AddressWebItem> addressWebItems = getAddressItems();
        for (AddressWebItem item : addressWebItems) {
            if (item.getName().equals(destination.getSpec().getAddress()))
                returnedElement = item;
        }
        return returnedElement;
    }

    /**
     * get all connections
     */
    public List<ConnectionWebItem> getConnectionItems() {
        WebElement content = getContentContainer();
        List<WebElement> elements = content.findElements(By.className("list-group-item"));
        List<ConnectionWebItem> connectionItems = new ArrayList<>();
        for (WebElement element : elements) {
            if (!element.getAttribute("class").contains("disabled")) {
                ConnectionWebItem item = new ConnectionWebItem(element);
                log.info(String.format("Got connection: %s", item.toString()));
                connectionItems.add(item);
            }
        }
        return connectionItems;
    }

    /**
     * get all connections
     */
    public List<ConnectionWebItem> getConnectionItems(int expectedCount) {
        List<ConnectionWebItem> connectionItems = new ArrayList<>();
        long endTime = System.currentTimeMillis() + 30000;
        while (connectionItems.size() != expectedCount && endTime > System.currentTimeMillis()) {
            log.info("First iteration waiting for {} connections items", expectedCount);
            WebElement content = getContentContainer();
            List<WebElement> elements = content.findElements(By.className("list-group-item"));
            connectionItems.clear();
            for (WebElement element : elements) {
                if (!element.getAttribute("class").contains("disabled")) {
                    ConnectionWebItem item = new ConnectionWebItem(element);
                    log.info(String.format("Got connection: %s", item.toString()));
                    connectionItems.add(item);
                }
            }
        }
        return connectionItems;
    }

    private WebElement getSubscriptionComboBox() throws Exception {
        return selenium.getWebElement(() -> selenium.getDriver().findElement(By.name("topic")));
    }

    /**
     * get alert banner when illegal regex is used in filter box
     */
    public WebElement getFilterRegexAlert() throws Exception {
        return selenium.getWebElement(() -> selenium.getDriver().findElement(By.className("pficon-error-circle-o")));
    }

    private WebElement getFilterRegexAlertClose() throws Exception {
        return selenium.getWebElement(() -> selenium.getDriver().findElement(By.className("pficon-close")));
    }

    private WebElement getNextButton() {
        return getModalWindow().findElement(By.id("nextButton"));
    }

    private WebElement getModalWindow() {
        return selenium.getDriver().findElement(By.className("modal-content"));
    }

    /**
     * get the radio button for the destination
     */
    public WebElement getRadioButtonForAddressType(Address destination) throws Exception {
        return selenium.getWebElement(() -> selenium.getDriver().findElement(By.id(destination.getSpec().getType().toLowerCase())));
    }

    /**
     * get Address Modal window page by selecting numbered circle
     */
    private WebElement getAddressModalPageNumbers() throws Exception {
        return selenium.getDriver().findElement(By.className("wizard-pf-steps-indicator"));
    }

    public WebElement getAddressModalPageByNumber(Integer pageNumber) throws Exception {
        return getAddressModalPageNumbers().findElements(By.className("wizard-pf-step-number")).get(pageNumber - 1);  //zero indexed
    }

    //================================================================================================
    // Operations
    //================================================================================================

    public void openWebConsolePage() throws Exception {
        openWebConsolePage(credentials);
    }

    public void openWebConsolePage(UserCredentials credentials) throws Exception {
        log.info("Opening console web page");
        selenium.getDriver().get(consoleRoute);
        selenium.getAngularDriver().waitForAngularRequestsToFinish();
        selenium.takeScreenShot();
        if (new AdminResourcesManager(Kubernetes.getInstance()).getAuthService(defaultAddressSpace.getSpec()
                .getAuthenticationService().getName()).getSpec().getType().equals(AuthenticationServiceType.standard)) {
            if (!login(credentials.getUsername(), credentials.getPassword()))
                throw new IllegalAccessException("Cannot login");
        }
        checkReachableWebPage();
    }


    public void openAddressesPageWebConsole() throws Exception {
        selenium.clickOnItem(getLeftMenuItemWebConsole("Addresses"));
        toolbarType = ToolbarType.ADDRESSES;
        selenium.getAngularDriver().waitForAngularRequestsToFinish();
        log.info("Addresses page opened");
    }

    public void openDashboardPageWebConsole() throws Exception {
        selenium.clickOnItem(getLeftMenuItemWebConsole("Dashboard"));
        selenium.getAngularDriver().waitForAngularRequestsToFinish();
        log.info("Dashboard page opened");
    }

    public void openConnectionsPageWebConsole() throws Exception {
        selenium.clickOnItem(getLeftMenuItemWebConsole("Connections"));
        toolbarType = ToolbarType.CONNECTIONS;
        selenium.getAngularDriver().waitForAngularRequestsToFinish();
        log.info("Connections page opened");
    }

    public void clickOnCreateButton() throws Exception {
        selenium.clickOnItem(getCreateButton());
    }

    public void clickOnRemoveButton() throws Exception {
        selenium.clickOnItem(getRemoveButton());
    }

    public void next() throws Exception {
        selenium.clickOnItem(getNextButton());
    }

    public void clickOnAddressModalPageByNumber(Integer pageNumber) throws Exception {
        selenium.clickOnItem(getAddressModalPageByNumber(pageNumber));
    }

    public void clickOnRegexAlertClose() throws Exception {
        selenium.clickOnItem(getFilterRegexAlertClose(), "Closing regex alert banner");
    }

    /**
     * common method for switching type of filtering/sorting
     */
    private void switchFilterOrSort(Enum switchElement, WebElement switchButton, List<WebElement> switchElements) throws Exception {
        selenium.clickOnItem(switchButton);
        for (WebElement element : switchElements) {
            if (element.findElement(By.tagName("a")).getText().toUpperCase().equals(switchElement.toString())) {
                selenium.clickOnItem(element);
                break;
            }
        }
    }

    /**
     * switch type of sorting Name/Senders/Receivers
     */
    private void switchSort(SortType sortType) throws Exception {
        log.info("Switch sorting to: " + sortType.toString());
        switchFilterOrSort(sortType, getSortSwitch(), getSortDropDown());
    }

    /**
     * switch type of filtering Name/Type
     */
    private void switchFilter(FilterType filterType) throws Exception {
        log.info("Switch filtering to: " + filterType.toString());
        switchFilterOrSort(filterType, getFilterSwitch(), getFilterDropDown());
    }

    /**
     * check if sorted ASC/DESC button is set to ASC.
     */
    private boolean isSortAsc() {
        Boolean isAsc;
        try {
            getAscDescButton().findElement(By.className("fa-sort-alpha-asc"));
            isAsc = true;
        } catch (Exception ex) {
            isAsc = false;
        }

        if (!isAsc) {
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
    public void addAddressesFilter(FilterType filterType, String filterValue) throws Exception {
        log.info(String.format("Adding filter ->  %s: %s", filterType.toString(), filterValue));
        switchFilter(filterType);
        switch (filterType) {
            case TYPE:
                addFilterBy(filterType, filterValue);
                break;
            case NAME:
                WebElement filterInput = getInputSearchAddressesConnections();
                selenium.fillInputItem(filterInput, filterValue);
                selenium.pressEnter(filterInput);
                break;
            default:
                throw new IllegalArgumentException("Filter type " + filterType + "isn't supported for addresses");
        }
    }

    /**
     * add whatever filter you want
     *
     * @param filterType
     * @param filterValue allowed values are for FilterType.NAME (String), FilterType.NAME (queue, topic, multicast, anycast)
     * @throws Exception
     */
    public void addConnectionsFilter(FilterType filterType, String filterValue) throws Exception {
        log.info(String.format("Adding filter ->  %s: %s", filterType.toString(), filterValue));
        switchFilter(filterType);
        switch (filterType) {
            case CONTAINER:
            case HOSTNAME:
            case USER:
                WebElement filterInput = getInputSearchAddressesConnections();
                selenium.fillInputItem(filterInput, filterValue);
                selenium.pressEnter(filterInput);
                break;
            case ENCRYPTED:
                addFilterBy(filterType, filterValue);
                break;
            default:
                throw new IllegalArgumentException("Filter type " + filterType + "isn't supported for addresses");
        }
    }

    /**
     * add filter by filter type [TYPE/ENCRYPTED]
     *
     * @param filterValue queue/topic/multicast/anycast/encrypted/unencrypted
     */
    private void addFilterBy(FilterType filterType, String filterValue) throws Exception {
        WebElement clickableTypeElement;
        String textToLog;
        switch (filterType) {
            case TYPE:
                textToLog = "Click on button: 'Filter by name...'";
                clickableTypeElement = getDropDownAddressType(filterValue, getDropDownAddressTypes());
                break;
            case ENCRYPTED:
                textToLog = "Click on button: 'Filter by encrypted/unencrypted...'";
                clickableTypeElement = getDropDownEncryptedType(filterValue, getDropDownAddressTypes());
                break;
            default:
                throw new IllegalArgumentException("Filter type " + filterType + "isn't supported for addresses");
        }
        selenium.executeJavaScript(
                "document.getElementById('_fields').getElementsByTagName('button')[1].click();",
                textToLog);
        selenium.clickOnItem(clickableTypeElement);

    }


    /**
     * remove filter element by (Name: Value)
     */
    private void removeFilter(FilterType filterType, String filterName) throws Exception {
        log.info("Removing filter: " + filterName);
        String filterText = String.format("%s: %s", filterType.toString().toLowerCase(), filterName);
        List<WebElement> filters = getFilterResultsToolbar().findElements(ByAngular.repeater("filter in config.appliedFilters"));
        for (WebElement filter : filters) {
            if (filterText.toUpperCase().equals(filter.findElement(By.className("active-filter")).getText().toUpperCase())) {
                WebElement button = filter.findElement(By.className("pficon-close"));
                selenium.clickOnItem(button, "clearFilterButton");
            }
        }
    }

    /**
     * remove 'type' filter element by (Name: Value)
     */
    public void removeFilterByType(String filterName) throws Exception {
        removeFilter(FilterType.TYPE, filterName);
    }

    /**
     * remove 'name' filter element by (Name: Value)
     */
    public void removeFilterByName(String filterName) throws Exception {
        removeFilter(FilterType.NAME, filterName);
    }

    /**
     * remove 'name' filter element by (Name: Value)
     */
    public void removeFilterByUser(String filterName) throws Exception {
        removeFilter(FilterType.USER, filterName);
    }

    /**
     * remove all filters elements
     */
    public void clearAllFilters() throws Exception {
        log.info("Removing all filters");
        WebElement clearAllButton = getFilterResultsToolbar().findElement(By.className("clear-filters"));
        selenium.clickOnItem(clearAllButton);
    }

    /**
     * Sort address items
     */
    public void sortItems(SortType sortType, boolean asc) throws Exception {
        log.info("Sorting");
        switchSort(sortType);
        if (asc && !isSortAsc()) {
            selenium.clickOnItem(getAscDescButton(), "Asc");
        } else if (!asc && isSortAsc()) {
            selenium.clickOnItem(getAscDescButton(), "Desc");
        }
    }

    /**
     * create multiple addresses
     */
    public void createAddressesWebConsole(Address... destinations) throws Exception {
        for (Address dest : destinations) {
            createAddressWebConsole(dest, true);
        }
    }

    /**
     * create specific address
     */
    public void createAddressWebConsole(Address destination) throws Exception {
        createAddressWebConsole(destination, true);
    }

    public void createAddressWebConsole(Address destination, boolean waitForReady) throws Exception {
        log.info("Create address using web console");

        //get addresses item from left panel view
        openAddressesPageWebConsole();

        //click on create button
        clickOnCreateButton();

        //fill address name
        selenium.fillInputItem(selenium.getWebElement(() -> selenium.getDriver().findElement(By.id("new-name"))), destination.getSpec().getAddress());

        //select address type
        selenium.clickOnItem(selenium.getWebElement(() -> selenium.getDriver().findElement(By.id(destination.getSpec().getType()))), "Radio button " + destination.getSpec().getType());

        //if address type is subscription, fill in the topic dropdown box
        if (destination.getSpec().getType().equals(AddressType.SUBSCRIPTION.toString())) {
            log.info("Selecting topic to attach subscription to");
            WebElement topicDropDown = getSubscriptionComboBox();
            selenium.clickOnItem(topicDropDown);
            Select combobox = new Select(topicDropDown);
            combobox.selectByVisibleText(destination.getSpec().getTopic());
        }

        WebElement nextButton = selenium.getWebElement(() -> selenium.getDriver().findElement(By.id("nextButton")));
        selenium.clickOnItem(nextButton);

        //select address plan
        selenium.clickOnItem(selenium.getWebElement(() -> selenium.getDriver().findElement(By.id(destination.getSpec().getPlan()))), "Radio button " + destination.getSpec().getPlan());

        selenium.clickOnItem(nextButton);
        selenium.clickOnItem(nextButton);

        AddressWebItem items = (AddressWebItem) selenium.waitUntilItemPresent(60, () -> getAddressItem(destination));

        assertNotNull(items, String.format("Console failed, does not contain created address item : %s", destination));

        if (waitForReady)
            AddressUtils.waitForDestinationsReady(new AddressApiClient(Kubernetes.getInstance(), defaultAddressSpace.getMetadata().getNamespace()), defaultAddressSpace,
                    new TimeoutBudget(5, TimeUnit.MINUTES), destination);
    }

    /**
     * delete multiple addresses
     */
    public void deleteAddressesWebConsole(Address... destinations) throws Exception {
        for (Address dest : destinations) {
            deleteAddressWebConsole(dest);
        }
    }

    public void deleteAddressWebConsole(Address destination) throws Exception {
        log.info("Remove address using web console");

        //open addresses
        openAddressesPageWebConsole();

        AddressWebItem addressItem = (AddressWebItem) selenium.waitUntilItemPresent(10, () -> getAddressItem(destination));

        //click on check box
        selenium.clickOnItem(addressItem.getCheckBox(), "check box: " + destination.getSpec().getAddress());

        //click on delete
        clickOnRemoveButton();

        selenium.waitUntilItemNotPresent(60, () -> getAddressItem(destination));

        //check if address deleted
        assertNull(getAddressItem(destination), "Console failed, still contains deleted address item ");
    }

    public boolean login() throws Exception {
        return login(credentials);
    }

    public boolean login(UserCredentials credentials) throws Exception {
        return login(credentials.getUsername(), credentials.getPassword());
    }

    public void logout() throws Exception {
        selenium.clickOnItem(getUserDropDown(), "User dropdown");
        selenium.clickOnItem(getLogoutHref(), "Return to global console");
    }

    public boolean login(String username, String password) throws Exception {
        try {
            getNavigateMenu();
            log.info("User is already logged");
            return true;
        } catch (Exception ex) {
            OpenshiftLoginWebPage ocLoginPage = new OpenshiftLoginWebPage(selenium);
            return ocLoginPage.login(username, password);
        }
    }


    @Override
    public void checkReachableWebPage() {
        selenium.getDriverWait().withTimeout(Duration.ofSeconds(60)).until(ExpectedConditions.presenceOfElementLocated(By.className("nav-pf-vertical")));
    }
}
