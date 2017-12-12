package io.enmasse.systemtest.marathon;

import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;

public class ChromeWebConsoleTest extends WebConsoleTest {


    @Override
    public WebDriver buildDriver() {
        ChromeOptions opts = new ChromeOptions();
        opts.setHeadless(true);
        opts.addArguments("--no-sandbox");
        return new ChromeDriver(opts);
    }
}
