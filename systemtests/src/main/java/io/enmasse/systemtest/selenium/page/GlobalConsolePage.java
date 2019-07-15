/*
 * Copyright 2019, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.systemtest.selenium.page;

import io.enmasse.address.model.AddressSpace;
import io.enmasse.admin.model.v1.AddressSpacePlan;
import io.enmasse.systemtest.AddressSpacePlans;
import io.enmasse.systemtest.AddressSpaceType;
import io.enmasse.systemtest.CustomLogger;
import io.enmasse.systemtest.UserCredentials;
import io.enmasse.systemtest.selenium.SeleniumProvider;
import io.enmasse.systemtest.selenium.resources.AddressSpaceWebItem;
import io.enmasse.systemtest.utils.AddressSpaceUtils;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.slf4j.Logger;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

@SuppressWarnings("unused")
public class GlobalConsolePage implements IWebPage {

    private static Logger log = CustomLogger.getLogger();

    SeleniumProvider selenium;
    String ocRoute;
    UserCredentials credentials;
    OpenshiftLoginWebPage loginPage;

    public GlobalConsolePage(SeleniumProvider selenium, String ocRoute, UserCredentials credentials) {
        this.selenium = selenium;
        this.ocRoute = ocRoute;
        this.credentials = credentials;
        this.loginPage = new OpenshiftLoginWebPage(selenium);
    }

    //================================================================================================
    // Getters and finders of elements and data
    //================================================================================================
    private WebElement getContentElem() {
        return selenium.getDriver().findElement(By.className("pf-c-page__main-section"));
    }

    private WebElement getCreateButton() {
        return selenium.getDriver().findElement(By.id("button-create"));
    }

    private WebElement getAddressSpaceMenu() {
        return getContentElem().findElement(By.className("pf-l-toolbar"))
                .findElement(By.className("pf-c-dropdown__toggle"));
    }

    private WebElement getDeleteButton() {
        return selenium.getDriver().findElement(By.id("dd-menuitem-delete"));
    }

    private WebElement getNamespaceDropDown() {
        return selenium.getDriver().findElement(By.id("form-namespace"));
    }

    private WebElement authServiceDropDown() {
        return selenium.getDriver().findElement(By.id("form-authservice"));
    }

    private WebElement getAddressSpaceNameInput() {
        return selenium.getDriver().findElement(By.id("form-name"));
    }

    private WebElement getBrokeredRadioButton() {
        return selenium.getDriver().findElement(By.id("radio-addressspace-brokered"));
    }

    private WebElement getStandardRadioButton() {
        return selenium.getDriver().findElement(By.id("radio-addressspace-standard"));
    }

    private WebElement getPlanDropDown() {
        return selenium.getDriver().findElement(By.id("form-planName"));
    }

    private WebElement getNextButton() {
        return selenium.getDriver().findElement(By.xpath("//button[contains(text(), 'Next')]"));
    }

    private WebElement getCancelButton() {
        return selenium.getDriver().findElement(By.xpath("//button[contains(text(), 'Cancel')]"));
    }

    private WebElement getFinishButton() {
        return selenium.getDriver().findElement(By.xpath("//button[contains(text(), 'Finish')]"));
    }

    private WebElement getBackButton() {
        return selenium.getDriver().findElement(By.xpath("//button[contains(text(), 'Back')]"));
    }

    private WebElement getTable() {
        return selenium.getDriver().findElement(By.id("table-instances"));
    }

    private WebElement getTableHeader() {
        return getTable().findElement(By.id("table-header"));
    }

    private WebElement getTableBody() {
        return getTable().findElement(By.id("table-body"));
    }

    private WebElement getModalBox() {
        return selenium.getDriver().findElement(By.className("pf-c-modal-box"));
    }

    private WebElement getModalButtonDelete() {
        return getModalBox().findElement(By.id("button-delete"));
    }


    //================================================================================================
    // Operations
    //================================================================================================

    public void openGlobalConsolePage() throws Exception {
        log.info("Opening global console on route {}", ocRoute);
        selenium.getDriver().get(ocRoute);
        if (waitUntilLoginPage()) {
            selenium.getAngularDriver().waitForAngularRequestsToFinish();
            selenium.takeScreenShot();
            try {
                logout();
            } catch (Exception ex) {
                log.info("User is not logged");
            }
            if (!login())
                throw new IllegalAccessException(loginPage.getAlertMessage());
        }
        selenium.getAngularDriver().waitForAngularRequestsToFinish();
        if (!waitUntilConsolePage()) {
            throw new IllegalStateException("Openshift console not loaded");
        }
    }

    private boolean login() throws Exception {
        return loginPage.login(credentials.getUsername(), credentials.getPassword());
    }

    public void logout() {
        try {
            WebElement userDropdown = selenium.getDriver().findElement(By.id("dd-user"));
            selenium.clickOnItem(userDropdown, "User dropdown navigation");
            WebElement logout = selenium.getDriver().findElement(By.id("dd-menuitem-logout"));
            selenium.clickOnItem(logout, "Log out");
        } catch (Exception ex) {
            log.info("Unable to logout, user is not logged in");
        }
    }

    private void selectNamespace(String namespace) throws Exception {
        selenium.clickOnItem(getNamespaceDropDown(), "namespace dropdown");
        selenium.clickOnItem(selenium.getDriver().findElement(By.xpath("//option[@value='" + namespace + "']")), namespace);
    }

    private void selectPlan(String plan) throws Exception {
        selenium.clickOnItem(getPlanDropDown(), "address space plan dropdown");
        selenium.clickOnItem(selenium.getDriver().findElement(By.xpath("//option[@value='" + plan + "']")), plan);
    }

    private void selectAuthService(String authService) throws Exception {
        selenium.clickOnItem(getPlanDropDown(), "address space plan dropdown");
        selenium.clickOnItem(selenium.getDriver().findElement(By.xpath("//option[@value='" + authService + "']")), authService);
    }

    public void createAddressSpace(AddressSpace addressSpace) throws Exception {
        selenium.clickOnItem(getCreateButton());
        selectNamespace(addressSpace.getMetadata().getNamespace());
        selenium.fillInputItem(getAddressSpaceNameInput(), addressSpace.getMetadata().getName());
        selenium.clickOnItem(addressSpace.getSpec().getType().equals(AddressSpaceType.BROKERED.toString().toLowerCase()) ? getBrokeredRadioButton() : getStandardRadioButton(),
                addressSpace.getSpec().getType());
        selectPlan(addressSpace.getSpec().getPlan());
        selectAuthService(addressSpace.getSpec().getAuthenticationService().getName());
        selenium.clickOnItem(getNextButton());
        selenium.clickOnItem(getFinishButton());
        selenium.waitUntilItemPresent(30, () -> getAddressSpaceItem(addressSpace));
        AddressSpaceUtils.waitForAddressSpaceReady(addressSpace);
        selenium.refreshPage();
    }

    public void deleteAddressSpace(AddressSpace addressSpace) throws Exception {
        AddressSpaceWebItem item = selenium.waitUntilItemPresent(30, () -> getAddressSpaceItem(addressSpace));
        selenium.clickOnItem(item.getCheckBox(), "Select address space to delete");
        selenium.clickOnItem(getAddressSpaceMenu(), "Address space dropdown");
        selenium.clickOnItem(getDeleteButton());
        selenium.clickOnItem(getModalButtonDelete());
        selenium.waitUntilItemNotPresent(30, () -> getAddressSpaceItem(addressSpace));
    }

    public ConsoleWebPage openAddressSpaceConsolePage(AddressSpace addressSpace) throws Exception {
        AddressSpaceWebItem item = selenium.waitUntilItemPresent(30, () -> getAddressSpaceItem(addressSpace));
        selenium.clickOnItem(item.getConsoleRoute());
        ConsoleWebPage consolePage = new ConsoleWebPage(selenium, addressSpace, credentials);
        consolePage.login();
        return consolePage;
    }

    public void switchAddressSpacePlan(AddressSpace addressSpace, String addressSpacePlan) {
        selenium.clickOnItem(getAddressSpaceItem(addressSpace).getActionDropDown());
        selenium.clickOnItem(selenium.getDriver().findElement(By.xpath("//a[contains(text(), 'Edit')]")));
        selenium.clickOnItem(selenium.getDriver().findElement(By.id("form-planName")));
        selenium.clickOnItem(selenium.getDriver()
                .findElement(By.xpath("//option[@value='" + addressSpacePlan +"']")));
        selenium.clickOnItem(selenium.getDriver().findElement(By.id("button-edit-save")));
        selenium.refreshPage();
        addressSpace.getSpec().setPlan(addressSpacePlan);
    }

    public List<AddressSpaceWebItem> getAddressSpaceItems() {
        WebElement content = getTableBody();
        List<WebElement> elements = content.findElements(By.tagName("tr"));
        List<AddressSpaceWebItem> addressItems = new ArrayList<>();
        for (WebElement element : elements) {
            AddressSpaceWebItem item = new AddressSpaceWebItem(element);
            log.info(String.format("Got addressSpace: %s", item.toString()));
            addressItems.add(item);
        }
        return addressItems;
    }

    public AddressSpaceWebItem getAddressSpaceItem(AddressSpace as) {
        AddressSpaceWebItem returnedElement = null;
        List<AddressSpaceWebItem> addressWebItems = getAddressSpaceItems();
        for (AddressSpaceWebItem item : addressWebItems) {
            if (item.getName().equals(as.getMetadata().getName()) && item.getNamespace().equals(as.getMetadata().getNamespace()))
                returnedElement = item;
        }
        return returnedElement;
    }

    private boolean waitUntilLoginPage() {
        try {
            selenium.getDriverWait().withTimeout(Duration.ofSeconds(3)).until(ExpectedConditions.titleContains("Log"));
            selenium.clickOnItem(selenium.getDriver().findElement(By.tagName("button")));
            return true;
        } catch (Exception ex) {
            return false;
        }
    }

    private boolean waitUntilConsolePage() {
        try {
            selenium.getDriverWait().until(ExpectedConditions.visibilityOfElementLocated(By.className("pf-c-page__header")));
            return true;
        } catch (Exception ex) {
            return false;
        }
    }

    @Override
    public void checkReachableWebPage() {
        selenium.getDriverWait().withTimeout(Duration.ofSeconds(60)).until(ExpectedConditions.or(
                ExpectedConditions.presenceOfElementLocated(By.className("pf-c-page__header")),
                ExpectedConditions.titleContains("Log")
        ));
    }
}
