/*
 * Copyright 2019, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.systemtest.selenium.page;


import io.enmasse.address.model.AddressSpace;
import io.enmasse.systemtest.AddressSpaceType;
import io.enmasse.systemtest.CustomLogger;
import io.enmasse.systemtest.UserCredentials;
import io.enmasse.systemtest.selenium.SeleniumProvider;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.slf4j.Logger;

import java.time.Duration;

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
        return selenium.getDriver().findElement(By.className("pf-c-content"));
    }

    private WebElement getCreateButton() {
        return selenium.getDriver().findElement(By.id("button-create"));
    }

    private WebElement getAddressSpaceMenu() {
        return getContentElem().findElement(By.className("pf-l-toolbar")).findElement(By.className("pf-c-dropdown"));
    }

    private WebElement getDeleteButton() {
        return selenium.getDriver().findElement(By.id("dd-menuitem-delete"));
    }

    private WebElement getNamespaceDropDown() {
        return selenium.getDriver().findElement(By.id("form-namespace"));
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

    public void createAddressSpace(AddressSpace addressSpace) throws Exception {
        selenium.clickOnItem(getCreateButton());
        selectNamespace(addressSpace.getMetadata().getNamespace());
        selenium.fillInputItem(getAddressSpaceNameInput(), addressSpace.getMetadata().getName());
        selenium.clickOnItem(addressSpace.getSpec().getType().equals(AddressSpaceType.BROKERED.toString().toLowerCase()) ? getBrokeredRadioButton() : getStandardRadioButton());
        selectPlan(addressSpace.getSpec().getPlan());
        selenium.clickOnItem(getNextButton());
        selenium.clickOnItem(getFinishButton());
    }

    private boolean waitUntilLoginPage() {
        try {
            selenium.getDriverWait().withTimeout(Duration.ofSeconds(3)).until(ExpectedConditions.titleContains("Log"));
            selenium.clickOnItem(selenium.getDriver().findElement(By.tagName("button")));
            return true;
        } catch (Exception ex) {
            ex.printStackTrace();
            return false;
        }
    }

    private boolean waitUntilConsolePage() {
        try {
            selenium.getDriverWait().until(ExpectedConditions.visibilityOfElementLocated(By.className("pf-c-page__header")));
            return true;
        } catch (Exception ex) {
            ex.printStackTrace();
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
