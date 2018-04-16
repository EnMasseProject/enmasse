/*
 * Copyright 2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.systemtest.selenium;


import io.enmasse.systemtest.CustomLogger;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.slf4j.Logger;

public class LoginWebPage {

    private static Logger log = CustomLogger.getLogger();

    SeleniumProvider selenium;

    public LoginWebPage(SeleniumProvider selenium) {
        this.selenium = selenium;
    }

    private WebElement getContentElement() throws Exception {
        return selenium.getDriver().findElement(By.id("kc-content"));
    }

    private WebElement getUsernameTextInput() throws Exception {
        return getContentElement().findElement(By.id("username"));
    }

    private WebElement getPasswordTextInput() throws Exception {
        return getContentElement().findElement(By.id("password"));
    }

    private WebElement getLoginButton() throws Exception {
        return getContentElement().findElement(By.className("btn-lg"));
    }

    private WebElement getAlertContainer() throws Exception {
        return selenium.getDriver().findElement(By.className("alert"));
    }

    public String getAlertMessage() throws Exception {
        return getAlertContainer().findElement(By.className("kc-feedback-text")).getText();
    }

    private boolean checkAlert() throws Exception {
        try {
            getAlertMessage();
            return false;
        } catch (Exception ignored) {
            return true;
        }
    }

    public boolean login(String username, String password) throws Exception {
        log.info("Try to login with credentials {} : {}", username, password);
        selenium.fillInputItem(getUsernameTextInput(), username);
        selenium.fillInputItem(getPasswordTextInput(), password);
        selenium.clickOnItem(getLoginButton(), "Log in");
        return checkAlert();
    }
}
