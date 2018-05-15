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

public class AuthorizeAccessWebPage {

    private static Logger log = CustomLogger.getLogger();
    private static final String webPageTitle = "Authorize service account kc-oauth";

    SeleniumProvider selenium;

    public AuthorizeAccessWebPage(SeleniumProvider selenium) {
        this.selenium = selenium;
    }


    private WebElement getCheckBoxRequestedPermission() {
        return selenium.getDriver().findElement(By.id("scope-0"));
    }

    private WebElement getBtnAllowRequestedPermissions() {
        return selenium.getInputByName("approve");
    }

    private WebElement getBtnDeny() {
        return selenium.getInputByName("deny");
    }

    public void clickOnCheckboxRequestedPermissions() throws Exception {
        selenium.clickOnItem(getCheckBoxRequestedPermission());
    }

    public void clickOnBtnAllowSelectedPermissions() throws Exception {
        selenium.clickOnItem(getBtnAllowRequestedPermissions());
    }

    public void clickOnBtnDeny() throws Exception {
        selenium.clickOnItem(getBtnDeny());
    }

    public boolean isOpenedInBrowser() {
        try {
            WebElement title = selenium.getDriver().findElement(By.tagName("title"));
            if (title != null && title.getText() != null) {
                String titleContent = title.getText();
                return titleContent.contains(webPageTitle);
            }
            return false;
        } catch (Exception ex) {
            return false;
        }
    }

}