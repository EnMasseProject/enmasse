package io.enmasse.systemtest.web;

import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;

import java.util.List;

public class WebItem {

    protected List<WebElement> additionalsInfo;
    protected WebElement webItem;

    protected int getCountOfAdditionalInfoItem(String item) {
        for (WebElement addInfo : additionalsInfo) {
            if (addInfo.getText().matches("(.*)" + item + "(.*)")) {
                if (addInfo.findElement(By.tagName("strong")).getAttribute("innerText").equals(""))
                    return 0;
                return Integer.parseInt(addInfo.findElement(By.tagName("strong")).getAttribute("innerText"));
            }
        }
        return 0;
    }

    protected List<WebElement> getAdditionalsInfo() {
        return webItem.findElement(By.className("list-view-pf-additional-info")).findElements(By.tagName("div"));
    }

}
