/*
 * Copyright 2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.systemtest.selenium.resources;

import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;

public class AddressSpaceWebItem extends WebItem implements Comparable<AddressSpaceWebItem> {
    private WebElement checkBox;
    private String name;
    private String namespace;
    private WebElement consoleRoute;
    private String type;
    private String status;
    private String created;
    private WebElement actionDropDown;

    public AddressSpaceWebItem(WebElement item) {
        this.webItem = item;
        this.checkBox = webItem.findElement(By.xpath("./td[@data-key='0']")).findElement(By.tagName("input"));
        this.name = parseName(webItem.findElement(By.xpath("./td[@data-key='1']")));
        this.namespace = parseNamespace(webItem.findElement(By.xpath("./td[@data-key='1']")));
        this.consoleRoute = parseRoute(webItem.findElement(By.xpath("./td[@data-key='1']")));
        this.type = webItem.findElement(By.xpath("./td[@data-label='Type']")).getText().split(" ")[1];
        this.status = webItem.findElement(By.xpath("./td[@data-label='Status']")).getText();
        this.created = webItem.findElement(By.xpath("./td[@data-label='Time created']")).getText();
        this.actionDropDown = webItem.findElement(By.className("pf-c-dropdown"));
    }

    public WebElement getCheckBox() {
        return checkBox;
    }

    public String getName() {
        return name;
    }

    public String getNamespace() {
        return namespace;
    }

    public WebElement getConsoleRoute() {
        return consoleRoute;
    }

    public String getType() {
        return type;
    }

    public String getStatus() {
        return status;
    }

    public String getCreated() {
        return created;
    }

    public WebElement getActionDropDown() {
        return actionDropDown;
    }

    public WebElement getEditMenuItem() {
        return getActionDropDown().findElement(By.xpath("//a[contains(text(), 'Edit')]"));
    }

    public WebElement getDeleteMenuItem() {
        return getActionDropDown().findElement(By.xpath("//a[contains(text(), 'Delete')]"));
    }

    private String parseName(WebElement elem) {
        try {
            return elem.findElement(By.tagName("a")).getText();
        } catch (Exception ex) {
            return elem.findElements(By.tagName("p")).get(0).getText();
        }
    }

    private WebElement parseRoute(WebElement elem) {
        try {
            return elem.findElement(By.tagName("a"));
        } catch (Exception ex) {
            return null;
        }
    }

    private String parseNamespace(WebElement elem) {
        return elem.getText().split(System.getProperty("line.separator"))[1];
    }

    @Override
    public String toString() {
        return String.format("name: %s, namespace: %s, type: %s, status: %s, time created: %s",
                this.name,
                this.namespace,
                this.type,
                this.status,
                this.created);
    }

    @Override
    public int compareTo(AddressSpaceWebItem o) {
        return name.compareTo(o.name);
    }
}
