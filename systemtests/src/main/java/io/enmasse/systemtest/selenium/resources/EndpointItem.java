/*
 * Copyright 2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.systemtest.selenium.resources;

import io.enmasse.systemtest.Endpoint;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class EndpointItem extends WebItem {
    private String name;
    private String type;
    private String host;
    private Map<String, Integer> ports = new HashMap<>();

    public EndpointItem(WebElement item) {
        this.webItem = item;
        this.name = webItem.findElement(By.xpath("./td[@data-label='Name']")).getText();
        this.type = webItem.findElement(By.xpath("./td[@data-label='Type']")).findElement(By.tagName("span")).getText();
        this.host = webItem.findElement(By.xpath("./td[@data-label='Host']")).getText();
        List<WebElement> portNames = webItem.findElement(By.xpath("./td[@data-label='Ports']")).findElements(By.tagName("div"));
        List<WebElement> portNumbers = webItem.findElement(By.xpath("./td[@data-key='4']")).findElements(By.tagName("div"));
        for (int i = 0; i < portNames.size(); i++) {
            ports.put(portNames.get(i).getText(), Integer.parseInt(portNumbers.get(i).getText()));
        }
    }

    public Endpoint getEndpoint(String protocol) {
        return new Endpoint(host, ports.get(protocol.toUpperCase()));
    }

    public String getName() {
        return name;
    }

    public String getType() {
        return type;
    }

    public String getHost() {
        return host;
    }

    public Map<String, Integer> getPorts() {
        return ports;
    }

    @Override
    public String toString() {
        return String.format("name: %s, type: %s, host: %s",
                this.name,
                this.type,
                this.host);
    }
}
