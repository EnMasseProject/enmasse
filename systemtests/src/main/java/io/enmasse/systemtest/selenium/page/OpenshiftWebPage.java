/*
 * Copyright 2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.systemtest.selenium.page;


import io.enmasse.systemtest.*;
import io.enmasse.systemtest.apiclients.AddressApiClient;
import io.enmasse.systemtest.selenium.SeleniumProvider;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.slf4j.Logger;

import java.util.List;
import java.util.stream.Collectors;

public class OpenshiftWebPage {

    private static Logger log = CustomLogger.getLogger();

    SeleniumProvider selenium;
    String ocRoute;
    KeycloakCredentials credentials;
    OpenshiftLoginWebPage loginPage;
    AddressApiClient addressApiClient;

    public OpenshiftWebPage(SeleniumProvider selenium, AddressApiClient addressApiClient, String ocRoute, KeycloakCredentials credentials) {
        this.selenium = selenium;
        this.ocRoute = ocRoute;
        this.credentials = credentials;
        this.addressApiClient = addressApiClient;
        this.loginPage = new OpenshiftLoginWebPage(selenium);
    }

    //================================================================================================
    // Getters and finders of elements and data
    //================================================================================================

    private WebElement getCatalog() {
        return selenium.getDriver().findElement(By.className("services-no-sub-categories"));
    }

    private String getTitleFromService(WebElement element) {
        return element.findElement(By.className("services-item-name")).getAttribute("title");
    }

    public WebElement getServiceFromCatalog(String name) {
        List<WebElement> services = getServicesFromCatalog();
        return services.stream().filter(item -> name.equals(getTitleFromService(item))).collect(Collectors.toList()).get(0);
    }

    public List<WebElement> getServicesFromCatalog() {
        List<WebElement> services = getCatalog().findElements(By.className("services-item"));
        services.forEach(item -> log.info("Got service item from catalog: {}", getTitleFromService(item)));
        return services;
    }

    private WebElement getOrderServiceModalWindow() {
        return selenium.getDriver().findElement(By.tagName("order-service"));
    }

    private WebElement getNextButton() {
        return getOrderServiceModalWindow().findElement(By.id("nextButton"));
    }

    private WebElement getBackButton() {
        return getOrderServiceModalWindow().findElement(By.id("backButton"));
    }

    private WebElement getAddToProjectDropDown() {
        return getOrderServiceModalWindow().findElement(By.className("dropdown"));
    }

    private WebElement getItemFromAddToProjectDropDown(String projectName) {
        List<WebElement> result = getItemsFromAddToProjectDropDown().stream()
                .filter(item -> projectName.equals(getTextFromAddToProjectDropDownItem(item))).collect(Collectors.toList());
        return result.size() > 0 ? result.get(0) : null;
    }

    private List<WebElement> getItemsFromAddToProjectDropDown() {
        List<WebElement> dropdownItems = getAddToProjectDropDown()
                .findElement(By.className("dropdown-menu")).findElements(By.className("ui-select-choices-row"));
        for (WebElement el : dropdownItems) {
            log.info("Got add to project choice: {}", getTextFromAddToProjectDropDownItem(el));
        }
        return dropdownItems;
    }

    private String getTextFromAddToProjectDropDownItem(WebElement item) {
        return item.findElement(By.className("ui-select-choices-row-inner")).findElement(By.tagName("span")).getText();
    }

    private List<WebElement> getBindingRadioItems() {
        return getOrderServiceModalWindow().findElement(By.className("radio")).findElements(By.tagName("label"));
    }

    //================================================================================================
    // Operations
    //================================================================================================

    public void openOpenshiftPage() throws Exception {
        log.info("Opening openshift web page on route {}", ocRoute);
        selenium.getDriver().get(ocRoute);
        waitUntilLoginPage();
        selenium.getAngularDriver().waitForAngularRequestsToFinish();
        selenium.takeScreenShot();
        if (!login())
            throw new IllegalAccessException(loginPage.getAlertMessage());
        selenium.getAngularDriver().waitForAngularRequestsToFinish();
        waitUntilConsolePage();
    }

    private boolean login() throws Exception {
        return loginPage.login(credentials.getUsername(), credentials.getPassword());
    }

    private void waitUntilLoginPage() {
        selenium.getDriverWait().until(ExpectedConditions.titleContains("Login"));
    }

    private void waitUntilConsolePage() {
        selenium.getDriverWait().until(ExpectedConditions.visibilityOfElementLocated(By.className("services-no-sub-categories")));
    }

    private void clickOnCreateBrokered() throws Exception {
        selenium.clickOnItem(getServiceFromCatalog("EnMasse (brokered)"));
    }

    public void clickOnCreateStandard() throws Exception {
        selenium.clickOnItem(getServiceFromCatalog("EnMasse (standard)"));
    }

    public void next() throws Exception {
        selenium.clickOnItem(getNextButton());
    }

    public void back() throws Exception {
        selenium.clickOnItem(getBackButton());
    }

    private void clickOnAddToProjectDropdown() throws Exception {
        selenium.clickOnItem(getAddToProjectDropDown(), "Add to project dropdown");
    }

    public void createAddressSpace(AddressSpace addressSpace, String projectName) throws Exception {
        if (addressSpace.getType() == AddressSpaceType.BROKERED) {
            createAddressSpaceBrokered(addressSpace.getNamespace(), projectName);
        } else {
            createAddressSpaceStandard(addressSpace.getNamespace(), projectName, "unlimited-standard");
        }
        TestUtils.waitForAddressSpaceReady(addressApiClient, addressSpace.getNamespace());
        if (addressSpace.getType() == AddressSpaceType.STANDARD) {
            log.info("Waiting 2min before standard address space is created");
            Thread.sleep(120_000);
        }
    }

    private void createAddressSpaceBrokered(String name, String projectName) throws Exception {
        clickOnCreateBrokered();
        next();
        selectProjectInWizard(projectName);
        selenium.fillInputItem(getOrderServiceModalWindow().findElement(By.tagName("catalog-parameters")).findElement(By.id("name")),
                name);
        next();
        next();
        next();
    }

    private void createAddressSpaceStandard(String name, String projectName, String plan) throws Exception {
        clickOnCreateStandard();
        next();
        selectPlanInStandard(plan);
        next();
        selectProjectInWizard(projectName);
        selenium.fillInputItem(getOrderServiceModalWindow().findElement(By.tagName("catalog-parameters")).findElement(By.id("name")),
                name);
        next();
        next();
        next();
    }

    private void selectPlanInStandard(String plan) throws Exception {
        List<WebElement> plansItems = getOrderServiceModalWindow().findElements(By.className("plan-name"));
        for (WebElement element : plansItems) {
            if (element.getText().equals(plan.toLowerCase())) {
                selenium.clickOnItem(element);
            }
        }
    }

    private void selectProjectInWizard(String projectName) throws Exception {
        clickOnAddToProjectDropdown();
        WebElement project = getItemFromAddToProjectDropDown(projectName);
        if (project != null) {
            log.info("Project is present address space will be added into them: {}", projectName);
            selenium.clickOnItem(project);
        } else {
            log.info("Project is not present address space will be added into new");
            selenium.clickOnItem(getItemFromAddToProjectDropDown("Create Project"));
            selenium.fillInputItem(getOrderServiceModalWindow().findElement(By.tagName("select-project")).findElement(By.id("name")),
                    projectName);
            selenium.fillInputItem(getOrderServiceModalWindow().findElement(By.tagName("select-project")).findElement(By.id("displayName")),
                    projectName);
        }
    }
}
