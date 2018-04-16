/*
 * Copyright 2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.systemtest.selenium;


import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;

public class ConnectionWebItem extends WebItem implements Comparable {

    private String name;
    private String headerText;
    private int sendersCount;
    private int receiversCount;
    private int messagesIn;
    private int messagesOut;
    private boolean encrypted;

    public ConnectionWebItem(WebElement item) {
        this.webItem = item;
        this.name = item.findElement(By.className("list-group-item-heading")).getText();
        this.headerText = item.findElement(By.className("list-group-item-text")).getText();
        this.readAdditionalInfo();
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


    public String getSomething() {
        throw new IllegalStateException("method is not implemented");
//        String[] types = type.split(" +");
//        StringBuilder something = new StringBuilder();
//        for (int i = 0; i < types.length - 1; i++) {
//            something.append(types[i]);
//        }
//        return something.toString(); //TODO! in web-console filled as "not available"
    }

    public String getUser() {
        String[] types = headerText.split(" +");
        return types[types.length - 1];
    }

    public String getType() {
        return headerText;
    }

    public String getContainerID() {
        String[] types = headerText.split(" +");
        return types[0];
    }

    @Override
    public int compareTo(Object o) {
        return 0;
    }

    @Override
    public String toString() {
        return String.format("name: %s, senders: %d, receivers: %d, Messages In: %d, Messages Out: %d",
                this.name, this.sendersCount, this.receiversCount, this.messagesIn, this.messagesOut);
    }
}
