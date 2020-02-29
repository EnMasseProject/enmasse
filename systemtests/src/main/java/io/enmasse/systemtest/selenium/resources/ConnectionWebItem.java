/*
 * Copyright 2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.systemtest.selenium.resources;

import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;

public class ConnectionWebItem extends WebItem implements Comparable<ConnectionWebItem> {
    private String host;
    private WebElement hostRoute;
    private String containerId;
    private String protocol;
    private String timeCreated;
    private double messagesIn;
    private double messagesOut;
    private int senders;
    private int receivers;

    public ConnectionWebItem(WebElement item) {
        this.webItem = item;
        this.host = parseName(webItem.findElement(By.xpath("./td[@data-label='host']")));
        this.hostRoute = webItem.findElement(By.xpath("./td[@data-label='host']"));
        this.containerId = webItem.findElement(By.xpath("./td[@data-label='Container ID']")).getText();
        this.protocol = webItem.findElement(By.xpath("./td[@data-label='Protocol']")).getText().split(" ")[0];
        this.timeCreated = webItem.findElement(By.xpath("./td[@data-label='Time created']")).getText();
        this.messagesIn = defaultDouble(webItem.findElement(By.xpath("./td[@data-label='column-4']")).getText());
        this.messagesOut = defaultDouble(webItem.findElement(By.xpath("./td[@data-label='column-5']")).getText());
        this.senders = defaultInt(webItem.findElement(By.xpath("./td[@data-label='Senders']")).getText());
        this.receivers = defaultInt(webItem.findElement(By.xpath("./td[@data-label='Receivers']")).getText());
    }

    public String getHost() {
        return host;
    }

    public String getContainerId() {
        return containerId;
    }

    public String getProtocol() {
        return protocol;
    }

    public String getTimeCreated() {
        return timeCreated;
    }

    public double getMessagesIn() {
        return messagesIn;
    }

    public double getMessagesOut() {
        return messagesOut;
    }

    public int getSenders() {
        return senders;
    }

    public int getReceivers() {
        return receivers;
    }

    public WebElement getHostRoute() { return hostRoute; }

    private String parseName(WebElement elem) {
        try {
            return elem.findElement(By.tagName("a")).getText();
        } catch (Exception ex) {
            return elem.findElements(By.tagName("p")).get(0).getText();
        }
    }

    @Override
    public String toString() {
        return String.format("host: %s, containerId: %s, messagesIn: %f, messagesOut: %f, senders: %d, receivers: %d",
                this.host,
                this.containerId,
                this.messagesIn,
                this.messagesOut,
                this.senders,
                this.receivers);
    }

    @Override
    public int compareTo(ConnectionWebItem o) {
        return host.compareTo(o.host);
    }

}
