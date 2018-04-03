/*
 * Copyright 2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.systemtest.selenium;


import com.paulhammant.ngwebdriver.NgWebDriver;
import io.enmasse.systemtest.CustomLogger;
import io.enmasse.systemtest.Environment;
import io.enmasse.systemtest.Kubernetes;
import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.openqa.selenium.*;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.slf4j.Logger;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertNotNull;

public class SeleniumProvider {

    private static Logger log = CustomLogger.getLogger();
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd_HH:mm:ss:SSSS");
    private WebDriver driver;
    private NgWebDriver angularDriver;
    private WebDriverWait driverWait;
    private Map<Date, File> browserScreenshots = new HashMap<>();
    private String webconsoleFolder = "selenium_tests";
    private Environment environment;
    private Kubernetes kubernetes;


    public void onFailed(ExtensionContext extensionContext) {
        try {
            String getTestClassName = extensionContext.getTestClass().get().getName();
            String getTestMethodName = extensionContext.getTestMethod().get().getName();
            takeScreenShot();
            Path path = Paths.get(
                    environment.testLogDir(),
                    webconsoleFolder,
                    getTestClassName,
                    getTestMethodName);
            Files.createDirectories(path);
            for (Date key : browserScreenshots.keySet()) {
                FileUtils.copyFile(browserScreenshots.get(key), new File(Paths.get(path.toString(),
                        String.format("%s.%s_%s.png", getTestClassName, getTestMethodName, dateFormat.format(key))).toString()));
            }
            log.info("Screenshots stored");
        } catch (Exception ex) {
            log.warn("Cannot save screenshots: " + ex.getMessage());
        } finally {
            tearDownDrivers();
        }
    }

    public void setupDriver(Environment environment, Kubernetes kubernetes, WebDriver driver) throws Exception {
        this.environment = environment;
        this.kubernetes = kubernetes;
        this.driver = driver;
        angularDriver = new NgWebDriver((JavascriptExecutor) driver);
        driverWait = new WebDriverWait(driver, 10);
        browserScreenshots.clear();
    }


    public void tearDownDrivers() {
        log.info("Tear down selenium web drivers");
        if (driver != null) {
            try {
                driver.quit();
            } catch (Exception ex) {
                log.warn("Raise warning on quit: " + ex.getMessage());
            }
            log.info("Driver is closed");
            driver = null;
            angularDriver = null;
            driverWait = null;
            browserScreenshots.clear();
        }
    }

    public WebDriver getDriver() {
        return this.driver;
    }

    public NgWebDriver getAngularDriver() {
        return this.angularDriver;
    }

    public WebDriverWait getDriverWait() {
        return driverWait;
    }

    protected void takeScreenShot() {
        try {
            log.info("Taking screenshot");
            browserScreenshots.put(new Date(), ((TakesScreenshot) driver).getScreenshotAs(OutputType.FILE));
        } catch (Exception ex) {
            log.warn("Cannot take screenshot: " + ex.getMessage());
        }
    }

    public void clearScreenShots() {
        if (browserScreenshots != null) {
            browserScreenshots.clear();
            log.info("Screenshots cleared");
        }
    }

    protected void clickOnItem(WebElement element) throws Exception {
        clickOnItem(element, null);
    }

    protected void executeJavaScript(String script) throws Exception {
        executeJavaScript(script, null);
    }

    protected void executeJavaScript(String script, String textToLog) throws Exception {
        takeScreenShot();
        assertNotNull("Selenium provider failed, script to execute is null", script);
        log.info("Execute script: " + (textToLog == null ? script : textToLog));
        ((JavascriptExecutor) driver).executeScript(script);
        angularDriver.waitForAngularRequestsToFinish();
        takeScreenShot();
    }

    protected void clickOnItem(WebElement element, String textToLog) throws Exception {
        takeScreenShot();
        assertNotNull(element, "Selenium provider failed, element is null");
        log.info("Click on button: " + (textToLog == null ? element.getText() : textToLog));
        element.click();
        angularDriver.waitForAngularRequestsToFinish();
        takeScreenShot();
    }


    protected void fillInputItem(WebElement element, String text) throws Exception {
        takeScreenShot();
        assertNotNull(element, "Selenium provider failed, element is null");
        element.sendKeys(text);
        angularDriver.waitForAngularRequestsToFinish();
        log.info("Filled input with text: " + text);
        takeScreenShot();
    }

    protected void pressEnter(WebElement element) throws Exception {
        takeScreenShot();
        assertNotNull(element, "Selenium provider failed, element is null");
        element.sendKeys(Keys.RETURN);
        angularDriver.waitForAngularRequestsToFinish();
        log.info("Enter pressed");
        takeScreenShot();
    }

    public void refreshPage() {
        takeScreenShot();
        log.info("Web page is going to be refreshed");
        driver.navigate().refresh();
        angularDriver.waitForAngularRequestsToFinish();
        log.info("Web page successfully refreshed");
        takeScreenShot();
    }

    public void waitUntilItemPresent(int timeInSeconds, IWebProperty<WebItem> item) throws Exception {
        waitUntilItem(timeInSeconds, item, true);
    }

    protected void waitUntilItemNotPresent(int timeInSeconds, IWebProperty<WebItem> item) throws Exception {
        waitUntilItem(timeInSeconds, item, false);
    }

    private void waitUntilItem(int timeInSeconds, IWebProperty<WebItem> item, boolean present) throws Exception {
        log.info("Waiting for element be present");
        int attempts = 0;
        while (attempts++ < timeInSeconds) {
            if (present) {
                try {
                    boolean result = item.get() != null;
                    if (result)
                        break;
                } catch (Exception ignored) {
                } finally {
                    log.info("Element not present, go to next iteration: " + attempts);
                }
            } else {
                try {
                    boolean result = item.get() == null;
                    if (result)
                        break;
                } catch (Exception ignored) {
                } finally {
                    log.info("Element still present, go to next iteration: " + attempts);
                }
            }
            Thread.sleep(1000);
        }
        log.info("End of waiting");
    }

    public void waitUntilPropertyPresent(int timeoutInSeconds, int expectedValue, IWebProperty<Integer> item) throws Exception {
        log.info("Waiting until data will be present");
        int attempts = 0;
        while (attempts < timeoutInSeconds) {
            if (expectedValue == item.get())
                break;
            Thread.sleep(1000);
            attempts++;
        }
        log.info("End of waiting");
    }
}
