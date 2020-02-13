/*
 * Copyright 2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.systemtest.selenium.resources;

import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;

public class ClientWebItem extends WebItem implements Comparable<ClientWebItem> {
    private String role;
    private String containerId;
    private String name;
    private String deliveryRate;
    private String backlog;

    public ClientWebItem(WebElement item) {
        this.webItem = item;
        this.role = webItem.findElement(By.xpath("./td[@data-label='Role']")).getText();
        this.containerId = webItem.findElement(By.xpath("./td[@data-label='Container ID']")).getText();
        this.name = webItem.findElement(By.xpath("./td[@data-label='Name']")).getText().split(" ")[0];
        this.deliveryRate = webItem.findElement(By.xpath("./td[@data-label='Delivery Rate']")).getText();
        this.backlog = webItem.findElement(By.xpath("./td[@data-label='Backlog]")).getText();
    }

    public String getRole() {
        return role;
    }

    public String getContainerId() {
        return containerId;
    }

    public String getName() {
        return name;
    }

    public String getDeliveryRate() {
        return deliveryRate;
    }

    public String getBacklog() {
        return backlog;
    }

    @Override
    public String toString() {
        return String.format("role: %s, containerId: %s, name: %s, deliveryRate: %s, backlog: %s",
                this.role,
                this.containerId,
                this.name,
                this.deliveryRate,
                this.backlog);
    }

    @Override
    public int compareTo(ClientWebItem o) {
        return containerId.compareTo(o.containerId);
    }
}
