package io.enmasse.systemtest.web;


import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;

public class ConnectionWebItem extends WebItem implements Comparable {

    private String name;
    private String type;
    private int sendersCount;
    private int receiversCount;
    private int messagesIn;
    private int messagesOut;
    private boolean encrypted;

    public ConnectionWebItem(WebElement item) {
        this.webItem = item;
        this.name = item.findElement(By.className("list-group-item-heading")).getText();
        this.type = item.findElement(By.className("list-group-item-text")).getText();
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
        String[] types = type.split(" +");
        return types[types.length - 1];
    }

    public String getType() {
        return type;
    }

    @Override
    public int compareTo(Object o) {
        return 0;
    }
}
