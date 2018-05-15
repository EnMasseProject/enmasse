/*
 * Copyright 2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.systemtest.selenium.resources;

import io.enmasse.systemtest.CustomLogger;
import io.enmasse.systemtest.selenium.SeleniumProvider;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.slf4j.Logger;

import java.util.List;

public class ProvisionedServiceItem {
    private static Logger log = CustomLogger.getLogger();
    private SeleniumProvider selenium;
    private WebElement serviceItem;
    private String id;

    public ProvisionedServiceItem(SeleniumProvider selenium) {
        this.selenium = selenium;
        this.serviceItem = selenium.getDriver().findElement(By.tagName("service-instance-row")).findElement(By.className("provisioned-service"));
        this.id = serviceItem.findElement(By.className("list-row-longname")).getText();
    }

    public WebElement getServiceItem() {
        return serviceItem;
    }

    public String getId() {
        return id;
    }

    private WebElement getServiceActions() {
        return serviceItem.findElement(By.className("list-pf-actions"));
    }

    private WebElement getActionMenuItem(String item) throws Exception {
        selenium.clickOnItem(getServiceActions(), "Provisioned service actions");
        List<WebElement> actions = getServiceActions().findElement(By.className("dropdown-menu-right")).findElements(By.tagName("li"));
        for (WebElement action : actions) {
            if (action.findElement(By.tagName("a")).getText().equals(item))
                return action;
        }
        return null;
    }

    public WebElement getServiceActionDelete() throws Exception {
        return getActionMenuItem("Delete");
    }

    public WebElement getServiceActionCreateBinding() throws Exception {
        return getActionMenuItem("Create Binding");
    }

    private List<WebElement> getServiceBindings() {
        List<WebElement> bindings = serviceItem.findElement(By.tagName("service-instance-bindings")).findElements(By.tagName("service-binding"));
        bindings.forEach(item -> log.info("Got binding: {}", item.findElement(By.tagName("h3")).getText()));
        return bindings;
    }

    public WebElement getServiceBinding(String bindingID) {
        List<WebElement> bindings = getServiceBindings();
        for (WebElement el : bindings) {
            if (el.findElement(By.tagName("h3")).getText().contains(bindingID)) {
                return el;
            }
        }
        log.info("Binding {} not found", bindingID);
        return null;
    }

    public WebElement getDeleteBindingButton(WebElement binding) {
        return getBindingAction(binding, "Delete");
    }

    public WebElement getViewSecretBindingButton(WebElement binding) {
        return getBindingAction(binding, "View Secret");
    }

    private WebElement getBindingAction(WebElement binding, String action) {
        List<WebElement> actions = binding.findElement(By.className("service-binding-actions")).findElements(By.tagName("a"));
        for (WebElement el : actions) {
            if (el.getText().equals(action)) {
                return el;
            }
        }
        return null;
    }

    public void expandServiceItem() throws Exception {
        if (!serviceItem.getAttribute("class").contains("active")) {
            selenium.clickOnItem(serviceItem.findElement(By.className("list-pf-chevron")), "Provisioned service");
        }
    }

    public void collapseServiceItem() throws Exception {
        if (serviceItem.getAttribute("class").contains("active")) {
            selenium.clickOnItem(serviceItem.findElement(By.className("list-pf-chevron")), "Provisioned service");
        }
    }

    public WebElement getRedirectConsoleButton() {
        return serviceItem.findElement(By.className("list-pf-details")).findElement(By.cssSelector("a[target='_blank']"));
    }
}
