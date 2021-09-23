package com.meread.selenium;

import com.meread.selenium.bean.JDCookie;
import lombok.extern.slf4j.Slf4j;
import org.junit.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.Cookie;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.firefox.FirefoxOptions;
import org.openqa.selenium.remote.RemoteWebDriver;
import org.openqa.selenium.support.ui.ExpectedCondition;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.util.Map;
import java.util.Set;

@Slf4j
public class QRTest extends TestBaseSelenium {

    public QRTest() {
        super(chromeOptions);
    }

    public boolean waitForJStoLoad(RemoteWebDriver webDriver) {

        WebDriverWait wait = new WebDriverWait(webDriver, 30);

        // wait for jQuery to load
        ExpectedCondition<Boolean> jQueryLoad = new ExpectedCondition<Boolean>() {
            @Override
            public Boolean apply(WebDriver driver) {
                try {
                    JavascriptExecutor j = (JavascriptExecutor) driver;
                    return ((Long) j.executeScript("return jQuery.active") == 0);
                } catch (Exception e) {
                    return true;
                }
            }
        };

        // wait for Javascript to load
        ExpectedCondition<Boolean> jsLoad = new ExpectedCondition<Boolean>() {
            @Override
            public Boolean apply(WebDriver driver) {
                JavascriptExecutor j = (JavascriptExecutor) driver;
                return j.executeScript("return document.readyState")
                        .toString().equals("complete");
            }
        };

        return wait.until(jQueryLoad) && wait.until(jsLoad);
    }

    public static final ChromeOptions chromeOptions;

    static {
        chromeOptions = new ChromeOptions();
        chromeOptions.setExperimentalOption("excludeSwitches", new String[]{"enable-automation"});
        chromeOptions.setExperimentalOption("useAutomationExtension", true);
        chromeOptions.addArguments("lang=zh-CN,zh,zh-TW,en-US,en");
        chromeOptions.addArguments("user-agent=Mozilla/5.0 (Macintosh; Intel Mac OS X 10_13_5) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/67.0.3396.99 Safari/537.36");
        chromeOptions.addArguments("disable-blink-features=AutomationControlled");
        chromeOptions.setCapability("selenoid:options", Map.<String, Object>of(
                "enableVNC", true,
                "enableVideo", false,
                "enableLog", true
        ));
        chromeOptions.addArguments("--disable-gpu");
        chromeOptions.addArguments("--headless");
        chromeOptions.addArguments("--no-sandbox");
        chromeOptions.addArguments("--disable-extensions");
        chromeOptions.addArguments("--disable-software-rasterizer");
        chromeOptions.addArguments("--ignore-ssl-errors=yes");
        chromeOptions.addArguments("--ignore-certificate-errors");
        chromeOptions.addArguments("--allow-running-insecure-content");
        chromeOptions.addArguments("--window-size=500,700");
    }

    @Test
    public void testQQ_login_selenium() {
        JDCookie ck = new JDCookie();
        WebDriver webDriver = getDriver(DriverType.selenium);
        loop2get_ck(ck, webDriver);
    }

    @Test
    public void testQQ_login_selenoid() {
        JDCookie ck = new JDCookie();
        WebDriver webDriver = getDriver(DriverType.selenoid);
        loop2get_ck(ck, webDriver);
    }

    private void loop2get_ck(JDCookie ck, WebDriver webDriver) {
        while (true) {
            webDriver.navigate().to("https://graph.qq.com/oauth2.0/show?which=Login&display=pc&response_type=code&client_id=100273020&redirect_uri=https%3A%2F%2Fplogin.m.jd.com%2Fcgi-bin%2Fml%2Fqqcallback%3Flsid%3D6pifrkrkjsb6t3mvpr0tplsrgpqa51rcq9sitj2dbej5h617&state=y20nntf3");
            try {
                Thread.sleep(2000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            String pageText = webDriver.findElement(By.tagName("body")).getText();
            System.out.println(pageText);
            //二维码失效 请点击刷新
            Set<Cookie> cookies = webDriver.manage().getCookies();
            for (Cookie cookie : cookies) {
                if ("pt_key".equals(cookie.getName())) {
                    ck.setPtKey(cookie.getValue());
                    break;
                }
            }
            for (Cookie cookie : cookies) {
                if ("pt_pin".equals(cookie.getName())) {
                    ck.setPtPin(cookie.getValue());
                    break;
                }
            }
            if (!ck.isEmpty()) {
                System.out.println(ck.toString());
                break;
            }
        }
    }

}