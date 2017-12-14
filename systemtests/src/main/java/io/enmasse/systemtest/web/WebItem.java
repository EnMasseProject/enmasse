package io.enmasse.systemtest.web;

import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;

import java.util.List;

public class WebItem {

    protected List<WebElement> additionalInfo;
    protected WebElement webItem;

    protected int getCountOfAdditionalInfoItem(String item) {
        for (WebElement addInfo : additionalInfo) {
            if (addInfo.getText().toUpperCase().contains(item.toUpperCase())) {
                if (addInfo.findElement(By.tagName("strong")).getText().equals(""))
                    return 0;
                return Integer.parseInt(addInfo.findElement(By.tagName("strong")).getText());
            }
        }
        return 0;
    }

    protected void readAdditionalInfo() {
        additionalInfo = webItem.findElement(By.className("list-view-pf-additional-info")).findElements(By.tagName("div"));
    }

}
