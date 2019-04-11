/*
 * Copyright 2019, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.systemtest.selenium.page;


import io.enmasse.systemtest.CustomLogger;
import io.enmasse.systemtest.UserCredentials;
import io.enmasse.systemtest.selenium.SeleniumProvider;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.slf4j.Logger;

import java.time.Duration;
import java.util.stream.Collectors;

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


    //================================================================================================
    // Operations
    //================================================================================================

    public void openGlobalConsolePage() throws Exception {
        log.info("Opening openshift web page on route {}", ocRoute);
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
            WebElement userDropdown = selenium.getDriver().findElement(By.className("pf-l-toolbar"))
                    .findElement(By.className("pf-l-toolbar__group")).findElements(By.className("pf-l-toolbar__item")).stream()
                    .filter(webElement -> webElement.findElement(By.className("pf-c-dropdown__toggle-text")).getText().equals(credentials.getUsername()))
                    .collect(Collectors.toList()).get(0);
            selenium.clickOnItem(userDropdown, "User dropdown navigation");
            WebElement logout = selenium.getDriver().findElement(By.className("navbar-right")).findElement(By.cssSelector("a[href='oauth/sign_in']"));
            selenium.clickOnItem(logout, "Log out");
        } catch (Exception ex) {
            log.info("Unable to logout, user is not logged in");
        }
    }

    private boolean waitUntilLoginPage() {
        try {
            selenium.getDriverWait().withTimeout(Duration.ofSeconds(3)).until(ExpectedConditions.titleContains("Login"));
            return true;
        } catch (Exception ex) {
            ex.printStackTrace();
            return false;
        }
    }

    private boolean waitUntilConsolePage() {
        try {
            selenium.getDriverWait().until(ExpectedConditions.visibilityOfElementLocated(By.className("services-no-sub-categories")));
            return true;
        } catch (Exception ex) {
            ex.printStackTrace();
            return false;
        }
    }

    @Override
    public void checkReachableWebPage() {
        selenium.getDriverWait().withTimeout(Duration.ofSeconds(60)).until(ExpectedConditions.presenceOfElementLocated(By.className("pf-c-page__header")));
    }
}
