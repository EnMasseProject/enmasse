/*
 * Copyright 2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.systemtest.selenium.page;

import io.enmasse.systemtest.CustomLogger;
import io.enmasse.systemtest.selenium.SeleniumProvider;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.slf4j.Logger;

public class AuthorizeAccessWebPage implements IWebPage {

    private SeleniumProvider selenium;
    private static final String webPageTitle = "Authorize service account kc-oauth";

    private static Logger log = CustomLogger.getLogger();

    public AuthorizeAccessWebPage(SeleniumProvider selenium) {
        this.selenium = selenium;
        checkReachableWebPage();
    }


    //===========================
    //======= Get element methods
    //===========================

    private WebElement getCheckBoxRequestedPermission() {
        return selenium.getDriver().findElement(By.id("scope-0"));
    }

    private WebElement getBtnAllowRequestedPermissions() {
        return selenium.getInputByName("approve");
    }

    private WebElement getBtnDeny() {
        return selenium.getInputByName("deny");
    }

    //===========================
    //= Click on elements methods
    //===========================


    private void clickOnCheckboxRequestedPermissions() throws Exception {
        selenium.clickOnItem(getCheckBoxRequestedPermission(), "checkbox user:info");
    }

    public void clickOnBtnAllowSelectedPermissions() throws Exception {
        selenium.clickOnItem(getBtnAllowRequestedPermissions(), "Allow selected permissions");
    }

    public void clickOnBtnDeny() throws Exception {
        selenium.clickOnItem(getBtnDeny(), "Deny");
    }

    public void setValueOnCheckboxRequestedPermissions(boolean check) throws Exception {
        selenium.setValueOnCheckboxRequestedPermissions(getCheckBoxRequestedPermission(), check);
    }

    @Override
    public void checkReachableWebPage() {
        checkTitle(selenium, webPageTitle);
    }

}