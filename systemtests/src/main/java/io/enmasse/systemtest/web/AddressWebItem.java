package io.enmasse.systemtest.web;

import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;

public class AddressWebItem implements Comparable<AddressWebItem> {
    private WebElement addressItem;
    private WebElement checkBox;
    private boolean isReady;
    private String name;

    public AddressWebItem(WebElement item) {
        this.addressItem = item;
        this.checkBox = item.findElement(By.className("list-view-pf-checkbox"));
        this.name = item.findElement(By.className("list-group-item-heading")).getText();
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

    @Override
    public int compareTo(AddressWebItem o) {
        return name.compareTo(o.name);
    }
}
