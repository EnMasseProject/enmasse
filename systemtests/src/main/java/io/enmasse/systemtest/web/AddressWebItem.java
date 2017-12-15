package io.enmasse.systemtest.web;

import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;

public class AddressWebItem extends WebItem implements Comparable<AddressWebItem> {


    private WebElement checkBox;
    private boolean isReady;
    private String name;
    private String type;
    private int sendersCount;
    private int receiversCount;
    private int messagesIn;
    private int messagesOut;
    private int messagesStored;

    public AddressWebItem(WebElement item) {
        this.webItem = item;
        this.checkBox = item.findElement(By.className("list-view-pf-checkbox"));
        this.name = item.findElement(By.className("list-group-item-heading")).getText();
        this.type = item.findElement(By.className("list-group-item-text")).getText();
        this.readAdditionalInfo();
        this.sendersCount = getCountOfAdditionalInfoItem("Senders");
        this.receiversCount = getCountOfAdditionalInfoItem("Receivers");
        this.messagesIn = getCountOfAdditionalInfoItem("Messages In");
        this.messagesOut = getCountOfAdditionalInfoItem("Messages Out");
        this.messagesStored = getCountOfAdditionalInfoItem("Stored");

        try {
            item.findElement(By.className("pficon-ok"));
            isReady = true;
        } catch (Exception ex) {
            isReady = false;
        }
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

    @Override
    public int compareTo(AddressWebItem o) {
        return name.compareTo(o.name);
    }

    @Override
    public String toString() {
        return String.format("name: %s, senders: %d, receivers: %d, Messages In: %d, Messages Out: %d, Messages stored: %d",
                this.name, this.sendersCount, this.receiversCount, this.messagesIn, this.messagesOut, this.messagesStored);
    }
}
