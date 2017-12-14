package io.enmasse.systemtest.web;


import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;

import java.util.List;

public class ConnectionWebItem extends WebItem implements Comparable {

    private List<WebElement> additionalsInfo;
    private String name;
    private int sendersCount;
    private int receiversCount;
    private int messagesIn;
    private int messagesOut;
    private boolean encrypted;

    public ConnectionWebItem(WebElement item) {
        this.webItem = item;
        this.name = item.findElement(By.className("list-group-item-heading")).getText();
        this.additionalsInfo = getAdditionalsInfo();
        this.sendersCount = getCountOfAdditionalInfoItem("Senders");
        this.receiversCount = getCountOfAdditionalInfoItem("Receivers");
        this.messagesIn = getCountOfAdditionalInfoItem("Messages In");
        this.messagesOut = getCountOfAdditionalInfoItem("Messages Out");

        try {
            item.findElement(By.className("fa-lock-fa"));
            this.encrypted = true;
        } catch (Exception ex) {
            this.encrypted = false;
        }
    }

    public WebElement getConnectionElement() {
        return webItem;
    }

    public String getName() {
        return name;
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

    public boolean isEncrypted() {
        return encrypted;
    }

    @Override
    public int compareTo(Object o) {
        return 0;
    }
}
