package io.enmasse.systemtest.web;

import io.enmasse.systemtest.Logging;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;

import java.util.List;

public class AddressWebItem implements Comparable<AddressWebItem> {
    private WebElement addressItem;
    private List<WebElement> additionalsInfo;
    private WebElement checkBox;
    private boolean isReady;
    private String name;
    private int sendersCount;
    private int receiversCount;
    private int messagesIn;
    private int messagesOut;
    private int messagesStored;

    public AddressWebItem(WebElement item) {
        this.addressItem = item;
        this.checkBox = item.findElement(By.className("list-view-pf-checkbox"));
        this.name = item.findElement(By.className("list-group-item-heading")).getText();
        this.additionalsInfo = getAdditionalsInfo();
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
        return addressItem;
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

    private List<WebElement> getAdditionalsInfo() {
        return addressItem.findElement(By.className("list-view-pf-additional-info")).findElements(By.tagName("div"));
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
    public int compareTo(AddressWebItem o) {
        return name.compareTo(o.name);
    }
}
