package io.enmasse.systemtest.web;

import com.gargoylesoftware.htmlunit.BrowserVersion;
import com.gargoylesoftware.htmlunit.WebClient;
import io.enmasse.systemtest.TestBase;
import org.junit.After;
import org.junit.Before;
import org.openqa.selenium.htmlunit.HtmlUnitDriver;

public class SeleniumTestBase extends TestBase {
    private HtmlUnitDriver driver;

    private void buildWebDriver(){
        driver = new HtmlUnitDriver(BrowserVersion.FIREFOX_52) {
            @Override
            protected WebClient newWebClient(BrowserVersion version) {
                WebClient webClient = super.newWebClient(version);
                webClient.getOptions().setThrowExceptionOnScriptError(false);
                return webClient;
            }
        };
    }

    private void closeWebDriver(){
        driver.close();
    }

    @Before
    public void setUpWebConsoleTest() {
        buildWebDriver();
    }

    @After
    public void tearDownWebConsoleTest() {
        closeWebDriver();
    }

    public HtmlUnitDriver getDriver() {
        return driver;
    }
}
