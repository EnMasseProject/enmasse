/*
 * Copyright 2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.systemtest.selenium.page;


import io.enmasse.systemtest.AddressSpace;
import io.enmasse.systemtest.AddressSpaceType;
import io.enmasse.systemtest.CustomLogger;
import io.enmasse.systemtest.KeycloakCredentials;
import io.enmasse.systemtest.apiclients.AddressApiClient;
import io.enmasse.systemtest.selenium.SeleniumProvider;
import io.enmasse.systemtest.selenium.resources.BindingSecretData;
import io.enmasse.systemtest.selenium.resources.ProvisionedServiceItem;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.slf4j.Logger;

import java.time.Duration;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class OpenshiftWebPage implements IWebPage {

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

    private WebElement getModalWindow() {
        return selenium.getDriver().findElement(By.className("modal-content"));
    }

    private WebElement getNextButton() {
        return getModalWindow().findElement(By.id("nextButton"));
    }

    private WebElement getBackButton() {
        return getModalWindow().findElement(By.id("backButton"));
    }

    private WebElement getAddToProjectDropDown() {
        return getModalWindow().findElement(By.className("dropdown"));
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
        return getModalWindow().findElement(By.className("radio")).findElements(By.tagName("label"));
    }

    private WebElement getProjectListItem(String name) {
        selenium.takeScreenShot();
        List<WebElement> projects = selenium.getDriver().findElements(By.className("list-group-item"));
        for (WebElement el : projects) {
            if (el.findElement(By.tagName("span")).getText().equals(name)) {
                return el;
            }
        }
        return null;
    }

    private ProvisionedServiceItem getProvisionedServiceItem() {
        return new ProvisionedServiceItem(selenium);
    }

    private WebElement getBindingCheckBoxes(String name) {
        List<WebElement> checkBoxes = selenium.getDriver().findElements(By.className("checkbox"));
        for (WebElement el : checkBoxes) {
            if (name.equals(el.findElement(By.tagName("label")).findElement(By.tagName("span")).getText())) {
                log.info(el.findElement(By.tagName("span")).getAttribute("innerHTML"));
                return el.findElement(By.tagName("span"));
            }
        }
        return null;
    }

    private void allowConsoleAccess() throws Exception {
        selenium.clickOnItem(getBindingCheckBoxes("consoleAccess"), "consoleAccess");
    }

    private void allowConsoleAdmin() throws Exception {
        selenium.clickOnItem(getBindingCheckBoxes("consoleAdmin"), "consoleAdmin");
    }

    private void allowExternalAccess() throws Exception {
        selenium.clickOnItem(getBindingCheckBoxes("externalAccess"), "externalAccess");
    }

    private void fillReceiveAddresses(String text) throws Exception {
        selenium.fillInputItem(selenium.getDriver().findElement(By.id("receiveAddresses")), text);
    }

    private void fillSendAddresses(String text) throws Exception {
        selenium.fillInputItem(selenium.getDriver().findElement(By.id("sendAddresses")), text);
    }

    private WebElement getRevealSecretButton() {
        return selenium.getDriver().findElement(By.cssSelector("a[ng-click='view.showSecret = !view.showSecret']"));
    }

    private BindingSecretData getBindingSecretData() {
        return new BindingSecretData(selenium.getDriver().findElement(By.className("secret-data")));
    }

    //================================================================================================
    // Operations
    //================================================================================================

    public void openOpenshiftPage() throws Exception {
        log.info("Opening openshift web page on route {}", ocRoute);
        selenium.getDriver().get(ocRoute);
        if (waitUntilLoginPage()) {
            selenium.getAngularDriver().waitForAngularRequestsToFinish();
            selenium.takeScreenShot();
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

    private boolean waitUntilLoginPage() {
        try {
            selenium.getDriverWait().withTimeout(Duration.ofSeconds(3)).until(ExpectedConditions.titleContains("Login"));
            return true;
        } catch (Exception ex) {
            return false;
        }
    }

    private boolean waitUntilConsolePage() {
        try {
            selenium.getDriverWait().until(ExpectedConditions.visibilityOfElementLocated(By.className("services-no-sub-categories")));
            return true;
        } catch (Exception ex) {
            return false;
        }
    }

    private void waitForRedirectToService() {
        selenium.getDriverWait().withTimeout(Duration.ofSeconds(10)).until(ExpectedConditions.presenceOfElementLocated(By.tagName("service-instance-row")));
    }

    private void waitUntilServiceIsReady() throws Exception {
        getProvisionedServiceItem().expandServiceItem();
        log.info("Waiting until provisioned service will be completed");
        selenium.getDriverWait().withTimeout(Duration.ofMinutes(4)).until(ExpectedConditions.numberOfElementsToBe(By.className("alert-info"), 0));
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

    public String provisionAddressSpaceViaSC(AddressSpace addressSpace, String projectName) throws Exception {
        log.info("Service for addressSpace {} will be provisioned", addressSpace);
        openOpenshiftPage();
        if (addressSpace.getType() == AddressSpaceType.BROKERED) {
            createAddressSpaceBrokered(addressSpace.getNamespace(), projectName);
        } else {
            createAddressSpaceStandard(addressSpace.getNamespace(), projectName, "unlimited-standard");
        }
        selenium.clickOnItem(getModalWindow().findElement(By.cssSelector(String.format("a[ng-href='project/%s']", projectName))), "Project overview");
        waitForRedirectToService();
        String serviceId = getProvisionedServiceItem().getId();
        waitUntilServiceIsReady();
        return serviceId;
    }

    public void deprovisionAddressSpace(String namespace) throws Exception {
        log.info("Service in namespace {} wil be deprovisioned", namespace);
        openOpenshiftPage();
        clickOnShowAllProjects();
        selenium.clickOnItem(getProjectListItem(namespace), namespace);
        waitForRedirectToService();
        selenium.clickOnItem(getProvisionedServiceItem().getServiceActionDelete(), "Delete");
        selenium.clickOnItem(getModalWindow().findElement(By.className("btn-danger")));
    }

    private void createAddressSpaceBrokered(String name, String projectName) throws Exception {
        clickOnCreateBrokered();
        next();
        selectProjectInWizard(projectName);
        selenium.fillInputItem(getModalWindow().findElement(By.tagName("catalog-parameters")).findElement(By.id("name")),
                name);
        next();
        next();
    }

    private void createAddressSpaceStandard(String name, String projectName, String plan) throws Exception {
        clickOnCreateStandard();
        next();
        selectPlanInStandard(plan);
        next();
        selectProjectInWizard(projectName);
        selenium.fillInputItem(getModalWindow().findElement(By.tagName("catalog-parameters")).findElement(By.id("name")),
                name);
        next();
        next();
    }

    private void selectPlanInStandard(String plan) throws Exception {
        List<WebElement> plansItems = getModalWindow().findElements(By.className("plan-name"));
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
            selenium.fillInputItem(getModalWindow().findElement(By.tagName("select-project")).findElement(By.id("name")),
                    projectName);
            selenium.fillInputItem(getModalWindow().findElement(By.tagName("select-project")).findElement(By.id("displayName")),
                    projectName);
        }
    }

    public void clickOnShowAllProjects() throws Exception {
        selenium.clickOnItem(selenium.getDriver().findElement(By.className("projects-view-all")), "Show all projects");
    }

    public String createBinding(String projectName, boolean consoleAccess, boolean adminConsole, boolean external,
                                String receiveAddress, String sendAddresses) throws Exception {
        log.info("Binding in namespace {} will be created", projectName);
        openOpenshiftPage();
        clickOnShowAllProjects();
        selenium.clickOnItem(getProjectListItem(projectName), projectName);
        waitForRedirectToService();
        selenium.clickOnItem(getProvisionedServiceItem().getServiceActionCreateBinding(), "Create Binding");
        next();
        if (consoleAccess) {
            allowConsoleAccess();
        }
        if (adminConsole) {
            allowConsoleAdmin();
        }
        if (external) {
            allowExternalAccess();
        }
        if (receiveAddress != null) {
            fillReceiveAddresses(receiveAddress);
        }
        if (sendAddresses != null) {
            fillSendAddresses(sendAddresses);
        }
        next();
        Thread.sleep(2000);
        String bindingId = getModalWindow().findElement(By.className("results-message")).findElement(By.tagName("strong")).getText();
        next();
        return bindingId;
    }

    public void removeBinding(String namespace, String bindingID) throws Exception {
        log.info("Binding {} in namespace {} will be removed", bindingID, namespace);
        openOpenshiftPage();
        clickOnShowAllProjects();
        selenium.clickOnItem(getProjectListItem(namespace), namespace);
        waitForRedirectToService();
        ProvisionedServiceItem serviceItem = getProvisionedServiceItem();
        serviceItem.expandServiceItem();
        WebElement binding = serviceItem.getServiceBinding(bindingID);
        selenium.clickOnItem(serviceItem.getDeleteBindingButton(binding), "Delete");
        selenium.clickOnItem(getModalWindow().findElement(By.className("btn-danger")));
    }

    public BindingSecretData viewSecretOfBinding(String namespace, String bindingID) throws Exception {
        openOpenshiftPage();
        clickOnShowAllProjects();
        selenium.clickOnItem(getProjectListItem(namespace), namespace);
        waitForRedirectToService();
        ProvisionedServiceItem serviceItem = getProvisionedServiceItem();
        serviceItem.expandServiceItem();
        WebElement binding = serviceItem.getServiceBinding(bindingID);
        selenium.clickOnItem(serviceItem.getViewSecretBindingButton(binding), "View Secret");
        selenium.clickOnItem(getRevealSecretButton(), "Reveal Secret");
        BindingSecretData secretData = getBindingSecretData();
        log.info(secretData.toString());
        return secretData;
    }

    public ConsoleWebPage clickOnDashboard(String namespace, AddressSpace addressSpace) throws Exception {
        openOpenshiftPage();
        clickOnShowAllProjects();
        selenium.clickOnItem(getProjectListItem(namespace), namespace);
        waitForRedirectToService();
        ProvisionedServiceItem serviceItem = getProvisionedServiceItem();
        serviceItem.collapseServiceItem();
        selenium.clickOnItem(serviceItem.getRedirectConsoleButton());
        Set<String> tabHandles = selenium.getDriver().getWindowHandles();
        selenium.getDriver().switchTo().window(tabHandles.toArray()[tabHandles.size() - 1].toString());
        return new ConsoleWebPage(selenium, addressApiClient, addressSpace);
    }

    @Override
    public void checkReachableWebPage() {
        //TODO
    }
}
