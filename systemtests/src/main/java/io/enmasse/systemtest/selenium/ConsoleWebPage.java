/*
 * Copyright 2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.systemtest.selenium;


import com.paulhammant.ngwebdriver.ByAngular;
import io.enmasse.systemtest.*;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

public class ConsoleWebPage {

    private static Logger log = CustomLogger.getLogger();
    private SeleniumProvider selenium;
    private String consoleRoute;
    private AddressApiClient addressApiClient;
    private AddressSpace defaultAddressSpace;
    private ToolbarType toolbarType;
    private LoginWebPage loginWebPage;
    private String username;
    private String password;

    public ConsoleWebPage(SeleniumProvider selenium, String consoleRoute, AddressApiClient addressApiClient, AddressSpace defaultAddressSpace, String username, String password) {
        this.selenium = selenium;
        this.consoleRoute = consoleRoute;
        this.addressApiClient = addressApiClient;
        this.defaultAddressSpace = defaultAddressSpace;
        this.loginWebPage = new LoginWebPage(selenium);
        this.username = username;
        this.password = password;
    }

    public void openWebConsolePage() throws Exception {
        openWebConsolePage(username, password);
    }

    public void openWebConsolePage(String username, String password) throws Exception {
        log.info("Opening console web page");
        logout();
        selenium.getDriver().get(consoleRoute);
        selenium.getAngularDriver().waitForAngularRequestsToFinish();
        selenium.takeScreenShot();
        if (!loginWebPage.login(username, password))
            throw new IllegalAccessException(loginWebPage.getAlertMessage());
    }

    private WebElement getNavigateMenu() throws Exception {
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

    public void openAddressesPageWebConsole() throws Exception {
        selenium.clickOnItem(getLeftMenuItemWebConsole("Addresses"));
        toolbarType = ToolbarType.ADDRESSES;
        log.info("Addresses page opened");
    }

    public void openDashboardPageWebConsole() throws Exception {
        selenium.clickOnItem(getLeftMenuItemWebConsole("Dashboard"));
        log.info("Dashboard page opened");
    }

    public void openConnectionsPageWebConsole() throws Exception {
        selenium.clickOnItem(getLeftMenuItemWebConsole("Connections"));
        toolbarType = ToolbarType.CONNECTIONS;
        log.info("Connections page opened");
    }

    public WebElement getCreateButton() throws Exception {
        return selenium.getDriver().findElement(ByAngular.buttonText("Create"));
    }

    public WebElement getRemoveButton() throws Exception {
        return selenium.getDriver().findElement(ByAngular.buttonText("Delete"));
    }

    public void clickOnCreateButton() throws Exception {
        selenium.clickOnItem(getCreateButton());
    }

    public void clickOnRemoveButton() throws Exception {
        selenium.clickOnItem(getRemoveButton());
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
    private WebElement getContentContainer() throws Exception {
        return selenium.getDriver().findElement(By.id("contentContainer"));
    }

    /**
     * (addresses/connections tab)
     * get toolbar with filter/sort
     */
    private WebElement getToolbar() throws Exception {
        return selenium.getDriver().findElement(By.id(toolbarType.toString()));
    }

    /**
     * (addresses/connections tab)
     * get element from toolbar with Filter elements
     */
    private WebElement getFilterGroup() throws Exception {
        return getToolbar().findElement(By.id("_fields"));
    }

    /**
     * (addresses/connections tab)
     * get button element with filter types * (Type/Name) for addresses
     * or
     * (Container/Hostname/User/Encrypted) for connections
     */
    private WebElement getFilterSwitch() throws Exception {
        return getFilterGroup().findElements(By.tagName("button")).get(0);
    }

    /**
     * (addresses/connections tab)
     * get button element with (address types: [Filter by type.../queue/topic/multicast/anycast])
     * or
     * (connections encrypted types: [Filter by encrypted/unencrypted/encrypted/unencrypted])
     */
    private WebElement getAddressConnectionsSwitch() throws Exception {
        return getFilterGroup().findElements(By.tagName("button")).get(1);
    }

    /**
     * (addresses/connections tab)
     * get input element for search with set filter to (addresses:[Name]) or (connections:[Container/Hostname/User])
     */
    private WebElement getInputSearchAddressesConnections() throws Exception {
        return getFilterGroup().findElement(By.tagName("input"));
    }

    /**
     * (addresses/connections tab)
     * get list of clickable li elements (addresses: [Type/Name]) or (connections: [Container/Hostname/User/Encrypted])
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

    public WebElement getCreateAddressModalWindow() throws Exception {
        return selenium.getDriver().findElement(By.className("modal-dialog")).findElement(By.className("modal-content"));
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
        return getUserDropDown().findElement(By.id("logout"));
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
     * get all addresses
     */
    public List<AddressWebItem> getAddressItems() throws Exception {
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
    public AddressWebItem getAddressItem(Destination destination) throws Exception {
        AddressWebItem returnedElement = null;
        List<AddressWebItem> addressWebItems = getAddressItems();
        for (AddressWebItem item : addressWebItems) {
            if (item.getName().equals(destination.getAddress()))
                returnedElement = item;
        }
        return returnedElement;
    }

    /**
     * get all connections
     */
    public List<ConnectionWebItem> getConnectionItems() throws Exception {
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
     * create multiple addresses
     */
    public void createAddressesWebConsole(Destination... destinations) throws Exception {
        for (Destination dest : destinations) {
            createAddressWebConsole(dest, false, true);
        }
    }

    /**
     * create specific address
     */
    public void createAddressWebConsole(Destination destination) throws Exception {
        createAddressWebConsole(destination, true, true);
    }

    public void createAddressWebConsole(Destination destination, boolean waitForReady) throws Exception {
        createAddressWebConsole(destination, true, waitForReady);
    }

    public void createAddressWebConsole(Destination destination, boolean openConsolePage, boolean waitForReady) throws Exception {
        log.info("Create address using web console");

        if (openConsolePage)
            openWebConsolePage();

        //get addresses item from left panel view
        openAddressesPageWebConsole();

        //click on create button
        clickOnCreateButton();

        //fill address name
        selenium.fillInputItem(selenium.getDriver().findElement(By.id("new-name")), destination.getAddress());

        //select address type
        selenium.clickOnItem(selenium.getDriver().findElement(By.id(destination.getType())), "Radio button " + destination.getType());

        WebElement nextButton = selenium.getDriver().findElement(By.id("nextButton"));

        selenium.clickOnItem(nextButton);

        //select address plan
        selenium.clickOnItem(selenium.getDriver().findElement(By.id(destination.getPlan())), "Radio button " + destination.getPlan());

        selenium.clickOnItem(nextButton);
        selenium.clickOnItem(nextButton);

        selenium.waitUntilItemPresent(60, () -> getAddressItem(destination));

        assertNotNull(getAddressItem(destination), "Console failed, does not contain created address item");

        if (waitForReady)
            TestUtils.waitForDestinationsReady(addressApiClient, defaultAddressSpace,
                    new TimeoutBudget(5, TimeUnit.MINUTES), destination);
    }

    /**
     * delete multiple addresses
     */
    public void deleteAddressesWebConsole(Destination... destinations) throws Exception {
        for (Destination dest : destinations) {
            deleteAddressWebConsole(dest, false);
        }
    }

    /**
     * delete specific address
     */
    public void deleteAddressWebConsole(Destination destination) throws Exception {
        deleteAddressWebConsole(destination, true);
    }

    public void deleteAddressWebConsole(Destination destination, boolean openConsolePage) throws Exception {
        log.info("Remove address using web console");

        if (openConsolePage)
            openWebConsolePage();

        //open addresses
        openAddressesPageWebConsole();

        AddressWebItem addressItem = getAddressItem(destination);

        //click on check box
        selenium.clickOnItem(addressItem.getCheckBox(), "check box: " + destination.getAddress());

        //click on delete
        clickOnRemoveButton();

        selenium.waitUntilItemNotPresent(60, () -> getAddressItem(destination));

        //check if address deleted
        assertNull(getAddressItem(destination), "Console failed, still contains deleted address item ");
    }

    public void logout() throws Exception {
        try {
            selenium.clickOnItem(getUserDropDown(), "User dropdown");
            selenium.clickOnItem(getLogoutHref(), "Logout");
        } catch (Exception ex) {
            log.info("Unable to logout, driver has login page opened.");
        }
    }
}
