package io.enmasse.systemtest.web;


import com.paulhammant.ngwebdriver.ByAngular;
import io.enmasse.systemtest.*;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

public class ConsoleWebPage {

    private SeleniumProvider selenium;
    private String consoleRoute;
    private AddressApiClient addressApiClient;
    private AddressSpace defaultAddressSpace;


    public ConsoleWebPage(SeleniumProvider selenium, String consoleRoute, AddressApiClient addressApiClient, AddressSpace defaultAddressSpace) {
        this.selenium = selenium;
        this.consoleRoute = consoleRoute;
        this.addressApiClient = addressApiClient;
        this.defaultAddressSpace = defaultAddressSpace;
    }


    public void openConsolePageWebConsole() throws Exception {
        selenium.driver.get(consoleRoute);
        selenium.angularDriver.waitForAngularRequestsToFinish();
        Logging.log.info("Console page opened");
        selenium.takeScreenShot();
    }

    private WebElement getLeftMenuItemWebConsole(String itemText) throws Exception {
        List<WebElement> items = selenium.driver.findElements(ByAngular.exactRepeater("item in items"));
        assertNotNull(items);
        WebElement returnedItem = null;
        for (WebElement item : items) {
            Logging.log.info("Got item: " + item.getText());
            if (item.getText().equals(itemText))
                returnedItem = item;
        }
        return returnedItem;
    }

    public void openAddressesPageWebConsole() throws Exception {
        selenium.clickOnItem(getLeftMenuItemWebConsole("Addresses"));
    }

    public void openDashboardPageWebConsole() throws Exception {
        selenium.clickOnItem(getLeftMenuItemWebConsole("Dashboard"));
    }

    public void openConnectionsPageWebConsole() throws Exception {
        selenium.clickOnItem(getLeftMenuItemWebConsole("Connections"));
    }

    public void clickOnCreateButton() throws Exception {
        selenium.clickOnItem(selenium.driver.findElement(ByAngular.buttonText("Create")));
    }

    public void clickOnRemoveButton() throws Exception {
        selenium.clickOnItem(selenium.driver.findElement(ByAngular.buttonText("Delete")));
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
        return selenium.driver.findElement(By.id("contentContainer"));
    }

    /**
     * get element with filter, sort, create, delete, ...
     */
    private WebElement getToolbar() throws Exception {
        return selenium.driver.findElement(By.id("exampleToolbar"));
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
        for (WebElement el : getFilterGroup().findElements(By.className("dropdown-menu"))) {
            List<WebElement> subEl = el.findElements(By.className("inner"));
            if (subEl.size() > 0) {
                List<WebElement> liElements = subEl.get(0).findElements(By.tagName("li"));
                liElements.forEach(liEl -> {
                    Logging.log.info("Got item: {}",
                            liEl.findElement(By.tagName("a")).findElement(By.tagName("span")).getText());
                });
                return liElements;
            }
        }
        throw new IllegalStateException("dropdown-menu doesn't exist");
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

        return dropDownTypes.get(addressTypesMap.get(addressType));
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
        Logging.log.info("Switch sorting to: " + sortType.toString());
        switchFilterOrSort(sortType, getSortSwitch(), getSortDropDown());
    }

    /**
     * switch type of filtering Name/Type
     */
    private void switchFilter(FilterType filterType) throws Exception {
        Logging.log.info("Switch filtering to: " + filterType.toString());
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
    public void addFilter(FilterType filterType, String filterValue) throws Exception {
        Logging.log.info(String.format("Adding filter ->  %s: %s", filterType.toString(), filterValue));
        switchFilter(filterType);
        switch (filterType) {
            case TYPE:
                addFilterByType(filterValue);
                break;
            case NAME:
                WebElement filterInput = getFilterGroup().findElement(By.tagName("input"));
                selenium.fillInputItem(filterInput, filterValue);
                selenium.pressEnter(filterInput);
                break;
        }
    }

    /**
     * add filter by address type
     *
     * @param addressType queue, topic, multicast, anycast
     */
    private void addFilterByType(String addressType) throws Exception {
        selenium.executeJavaScript(
                "document.getElementById('_fields').getElementsByTagName('button')[1].click();",
                "Click on button: 'Filter by Type...'");
        WebElement addressTypeElement = getDropDownAddressType(addressType, getDropDownAddressTypes());
        selenium.clickOnItem(addressTypeElement);
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
                selenium.clickOnItem(button, "clearFilterButton");
            }
        }
    }

    /**
     * remove 'type' filter element by (Name: Value)
     */
    public void removeFilterByType(String filterName) throws Exception {
        Logging.log.info("Removing filter: " + filterName);
        removeFilter(FilterType.TYPE, filterName);
    }

    /**
     * remove 'name' filter element by (Name: Value)
     */
    public void removeFilterByName(String filterName) throws Exception {
        Logging.log.info("Removing filter: " + filterName);
        removeFilter(FilterType.NAME, filterName);
    }

    /**
     * remove all filters elements
     */
    public void clearAllFilters() throws Exception {
        Logging.log.info("Removing all filters");
        WebElement clearAllButton = getFilterResultsToolbar().findElement(By.className("clear-filters"));
        selenium.clickOnItem(clearAllButton);
    }

    /**
     * Sort address items
     */
    public void sortItems(SortType sortType, boolean asc) throws Exception {
        Logging.log.info("Sorting");
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
            Logging.log.info("Got address: " + item.getName());
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
     * create multiple addresses
     */
    public void createAddressesWebConsole(Destination... destinations) throws Exception {
        for (Destination dest : destinations) {
            createAddressWebConsole(dest);
        }
    }

    /**
     * create specific address
     */
    public void createAddressWebConsole(Destination destination) throws Exception {
        //get console page
        openConsolePageWebConsole();

        //get addresses item from left panel view
        openAddressesPageWebConsole();

        //click on create button
        clickOnCreateButton();

        //fill address name
        selenium.fillInputItem(selenium.driver.findElement(By.id("new-name")), destination.getAddress());

        //select address type
        selenium.clickOnItem(selenium.driver.findElement(By.id(destination.getType())), "Radio button " + destination.getType());

        WebElement nextButton = selenium.driver.findElement(By.id("nextButton"));
        selenium.clickOnItem(nextButton);
        selenium.clickOnItem(nextButton);
        selenium.clickOnItem(nextButton);

        assertNotNull(getAddressItem(destination));

        TestUtils.waitForDestinationsReady(addressApiClient, defaultAddressSpace,
                new TimeoutBudget(5, TimeUnit.MINUTES), destination);
    }

    /**
     * delete multiple addresses
     */
    public void deleteAddressesWebConsole(Destination... destinations) throws Exception {
        for (Destination dest : destinations) {
            deleteAddressWebConsole(dest);
        }
    }

    /**
     * delete specific address
     */
    public void deleteAddressWebConsole(Destination destination) throws Exception {
        //open console webpage
        openConsolePageWebConsole();

        //open addresses
        openAddressesPageWebConsole();

        AddressWebItem addressItem = getAddressItem(destination);

        //click on check box
        selenium.clickOnItem(addressItem.getCheckBox(), "check box: " + destination.getAddress());

        //click on delete
        clickOnRemoveButton();

        //check if address deleted
        selenium.driverWait.until(ExpectedConditions.invisibilityOf(addressItem.getAddressItem()));
        assertNull(getAddressItem(destination));
    }
}
