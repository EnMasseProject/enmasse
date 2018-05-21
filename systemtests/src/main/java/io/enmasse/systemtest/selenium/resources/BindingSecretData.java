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
    private String console;
    private String externalMqttHost;
    private String externalMqttPort;
    private String messagingAmqpPort;
    private String messagingAmqpsPort;
    private String mqttCert;
    private String mqttHost;
    private String mqqtPort;
    private String mqttsPort;

    public BindingSecretData(WebElement item) {
        List<WebElement> data = item.findElements(By.className("image-source-item"));
        this.externalMessagingHost = (String) parseDataFromSecret(data, "externalMessagingHost");
        this.externalMessagingPort = (String) parseDataFromSecret(data, "externalMessagingPort");
        this.messagingCert = (String) parseDataFromSecret(data, "messagingCert.pem");
        this.messagingHost = (String) parseDataFromSecret(data, "messagingHost");
        this.username = (String) parseDataFromSecret(data, "username");
        this.password = (String) parseDataFromSecret(data, "password");
        this.console = (String) parseDataFromSecret(data, "console");
        this.externalMqttHost = (String) parseDataFromSecret(data, "externalMqttHost");
        this.externalMqttPort = (String) parseDataFromSecret(data, "externalMqttPort");
        this.messagingAmqpPort = (String) parseDataFromSecret(data, "messagingAmqpPort");
        this.messagingAmqpsPort = (String) parseDataFromSecret(data, "messagingAmqpsPort");
        this.mqttCert = (String) parseDataFromSecret(data, "mqttCert.pem");
        this.mqttHost = (String) parseDataFromSecret(data, "mqttHost");
        this.mqqtPort = (String) parseDataFromSecret(data, "mqttMqttPort");
        this.mqttsPort = (String) parseDataFromSecret(data, "mqttMqttsPort");
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

    public String getConsole() {
        return console;
    }

    public String getExternalMqttHost() {
        return externalMqttHost;
    }

    public String getExternalMqttPort() {
        return externalMqttPort;
    }

    public String getMessagingAmqpPort() {
        return messagingAmqpPort;
    }

    public String getMessagingAmqpsPort() {
        return messagingAmqpsPort;
    }

    public String getMqttCert() {
        return mqttCert;
    }

    public String getMqttHost() {
        return mqttHost;
    }

    public String getMqqtPort() {
        return mqqtPort;
    }

    public String getMqttsPort() {
        return mqttsPort;
    }

    private Object parseDataFromSecret(List<WebElement> data, String dataName) {
        for (WebElement d : data) {
            if (d.findElement(By.tagName("dt")).getText().equals(dataName)) {
                try {
                    return d.findElement(By.tagName("input")).getAttribute("value");
                } catch (Exception ex) {
                    return d.findElement(By.tagName("pre")).getText();
                }
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
