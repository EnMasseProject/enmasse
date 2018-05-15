/*
 * Copyright 2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.systemtest.selenium.resources;

import io.enmasse.systemtest.KeycloakCredentials;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;

import java.util.List;

public class BindingSecretData {
    private String externalMessagingHost;
    private String externalMessagingPort;
    private String messagingCert;
    private String messagingHost;
    private String username;
    private String password;

    public BindingSecretData(WebElement item) {
        List<WebElement> data = item.findElements(By.className("image-source-item"));
        this.externalMessagingHost = (String) parseDataFromSecret(data, "externalMessagingHost");
        this.externalMessagingPort = (String) parseDataFromSecret(data, "externalMessagingPort");
        this.messagingCert = (String) parseDataFromSecret(data, "messagingCert");
        this.messagingHost = (String) parseDataFromSecret(data, "messagingHost");
        this.username = (String) parseDataFromSecret(data, "username");
        this.password = (String) parseDataFromSecret(data, "password");
    }

    public String getExternalMessagingHost() {
        return externalMessagingHost;
    }

    public String getExternalMessagingPort() {
        return externalMessagingPort;
    }

    public String getMessagingCert() {
        return messagingCert;
    }

    public String getMessagingHost() {
        return messagingHost;
    }

    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
    }

    public KeycloakCredentials getCredentials() {
        return new KeycloakCredentials(username, password);
    }

    private Object parseDataFromSecret(List<WebElement> data, String dataName) {
        for (WebElement d : data) {
            if (d.findElement(By.tagName("dt")).getText().equals(dataName)) {
                return d.findElement(By.tagName("input")).getAttribute("value");
            }
        }
        return null;
    }

    @Override
    public String toString() {
        return String.format("Binding secret data username: %s, password %s, external: %s:%s",
                username, password, externalMessagingHost, externalMessagingPort);
    }
}
