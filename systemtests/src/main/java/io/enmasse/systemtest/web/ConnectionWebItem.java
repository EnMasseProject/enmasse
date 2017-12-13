package io.enmasse.systemtest.web;


import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;

import java.util.List;

public class ConnectionWebItem implements Comparable {

    private WebElement connectionElement;
    private List<WebElement> additionalsInfo;
    private String name;
    private int sendersCount;
    private int receiversCount;
    private int messagesIn;
    private int messagesOut;
    private boolean encrypted;

    public ConnectionWebItem(WebElement item) {
        this.connectionElement = item;
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
        return connectionElement;
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

    private List<WebElement> getAdditionalsInfo() {
        return connectionElement.findElement(By.className("list-view-pf-additional-info")).findElements(By.tagName("div"));
    }

    private int getCountOfAdditionalInfoItem(String item) {
        for (WebElement addInfo : additionalsInfo) {
            if (addInfo.getText().matches("(.*)" + item + "(.*)")) {
                if(addInfo.findElement(By.tagName("strong")).getAttribute("innerText").equals(""))
                    return 0;
                return Integer.parseInt(addInfo.findElement(By.tagName("strong")).getAttribute("innerText"));
            }
        }
        return 0;
    }

    @Override
    public int compareTo(Object o) {
        return 0;
    }
}
