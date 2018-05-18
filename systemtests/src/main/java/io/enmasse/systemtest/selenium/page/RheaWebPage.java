/*
 * Copyright 2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.systemtest.selenium.page;


import io.enmasse.systemtest.CustomLogger;
import io.enmasse.systemtest.KeycloakCredentials;
import io.enmasse.systemtest.selenium.SeleniumProvider;
import org.openqa.selenium.By;
import org.slf4j.Logger;

public class RheaWebPage implements IWebPage {

    private static final String webPageTitle = "cli-rhea client";
    private static Logger log = CustomLogger.getLogger();
    private SeleniumProvider selenium;

    public RheaWebPage(SeleniumProvider selenium) {
        this.selenium = selenium;
    }

    public void openRheaWebPage() {
        log.info("Opening rhea web page");
        selenium.getDriver().get("file:///opt/rhea.html");
        selenium.getAngularDriver().waitForAngularRequestsToFinish();
        selenium.takeScreenShot();
        checkReachableWebPage();
    }

    public void sendReceiveMessages(String server, String address, int count, KeycloakCredentials credentials) throws Exception {
        openRheaWebPage();
        String command = String.format("connect_to_enmasse(\"wss://%s\", \"%s\", \"%s\", \"%s\", \"%s\")",
                server, address, count, credentials.getUsername(), credentials.getPassword());
        selenium.executeJavaScript(command);
    }

    public boolean checkCountMessage(int count) throws Exception {
        return selenium.getWebElements(() -> selenium.getDriver().findElements(By.tagName("div")), count).size() == count;
    }

    @Override
    public void checkReachableWebPage() {
        checkTitle(selenium, webPageTitle);
    }
}
