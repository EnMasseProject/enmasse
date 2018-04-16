/*
 * Copyright 2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.systemtest.selenium;

import io.enmasse.systemtest.AddressStatus;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;

public class AddressWebItem extends WebItem implements Comparable<AddressWebItem> {


    private WebElement checkBox;
    private boolean isReady;
    private String name;
    private String type;
    private String plan;
    private AddressStatus status;
    private int sendersCount;
    private int receiversCount;
    private int messagesIn;
    private int messagesOut;
    private int messagesStored;

    public AddressWebItem(WebElement item) {
        this.webItem = item;
        this.checkBox = item.findElement(By.className("list-view-pf-checkbox"));
        this.name = item.findElement(By.className("list-group-item-heading")).getText();
        setTypeAndPlan();
        this.status = this.setStatus();
        this.readAdditionalInfo();
        this.sendersCount = getCountOfAdditionalInfoItem("Senders");
        this.receiversCount = getCountOfAdditionalInfoItem("Receivers");
        this.messagesIn = getCountOfAdditionalInfoItem("Messages In");
        this.messagesOut = getCountOfAdditionalInfoItem("Messages Out");
        this.messagesStored = getCountOfAdditionalInfoItem("Stored");
        this.isReady = AddressStatus.READY == this.status;
    }

    public WebElement getAddressItem() {
        return webItem;
    }

    public WebElement getCheckBox() {
        return checkBox;
    }

    public boolean getIsReady() {
        return isReady;
    }

    public String getName() {
        return name;
    }

    public String getType() {
        return type;
    }

    public String getPlan() {
        return plan;
    }

    public int getSendersCount() {
        return sendersCount;
    }

    public int getReceiversCount() {
        return receiversCount;
    }

    public int getMessagesIn() {
        return messagesIn;
    }

    public int getMessagesOut() {
        return messagesOut;
    }

    public int getMessagesStored() {
        return messagesStored;
    }

    public AddressStatus getStatus() {
        return status;
    }


    private AddressStatus setStatus() {
        WebElement statusElement = this.webItem.findElement(By.className("list-view-pf-left"));
        String iconStatus = statusElement.findElement(By.className("fa")).getAttribute("class");
        if (iconStatus.contains("pficon-ok"))
            return AddressStatus.READY;
        if (iconStatus.contains("pficon-error-circle-o"))
            return AddressStatus.ERROR;
        if (iconStatus.contains("fa-spinner"))
            return AddressStatus.PENDING;
        if (iconStatus.contains("pficon-warning-triangle-o"))
            return AddressStatus.WARNING;

        return AddressStatus.UNKNOWN;
    }

    private void setTypeAndPlan() {
        String[] tmp = this.webItem.findElement(By.className("list-group-item-text")).getText()
                .split("(\\s|&nbsp;){2,}");
        this.type = tmp[0];
        this.plan = tmp[1];
    }

    @Override
    public int compareTo(AddressWebItem o) {
        return name.compareTo(o.name);
    }

    @Override
    public String toString() {
        return String.format("name: %s, status: %s, type: %s, plan: %s, senders: %d, receivers: %d, Messages In: %d, Messages Out: %d, Messages stored: %d",
                this.name,
                this.status,
                this.type,
                this.plan,
                this.sendersCount,
                this.receiversCount,
                this.messagesIn,
                this.messagesOut,
                this.messagesStored);
    }
}
