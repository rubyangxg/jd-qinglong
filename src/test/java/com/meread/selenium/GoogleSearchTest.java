package com.meread.selenium;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.impl.client.HttpClientBuilder;
import org.junit.Assert;
import org.junit.Test;
import org.openqa.selenium.*;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.html5.LocalStorage;
import org.openqa.selenium.remote.RemoteExecuteMethod;
import org.openqa.selenium.remote.RemoteWebDriver;
import org.openqa.selenium.remote.html5.RemoteWebStorage;
import org.openqa.selenium.support.ui.ExpectedCondition;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;

import java.nio.charset.StandardCharsets;

@Slf4j
public class GoogleSearchTest extends TestBase {

    public GoogleSearchTest() {
        super(getOpt());
    }

    private static MutableCapabilities getOpt() {
        ChromeOptions chromeOptions = new ChromeOptions();
        chromeOptions.setExperimentalOption("excludeSwitches", new String[]{"enable-automation"});
        chromeOptions.setExperimentalOption("useAutomationExtension", false);
        chromeOptions.addArguments("lang=zh-CN,zh,zh-TW,en-US,en");
        chromeOptions.addArguments("user-agent=Mozilla/5.0 (Macintosh; Intel Mac OS X 10_13_5) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/67.0.3396.99 Safari/537.36");
        chromeOptions.addArguments("disable-blink-features=AutomationControlled");
        return chromeOptions;
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

    @Test
    public void enterGoogleSearchAndViewResults() {
        WebDriver webDriver = getDriver();
        By searchLocator = By.cssSelector("input[value='Google Search']");
        webDriver.navigate().to("http://www.google.com");
        WebElement searchText = webDriver.findElement(By.cssSelector("input[title=Search]"));
        searchText.sendKeys("hi");
        WebElement searchButton = webDriver.findElement(searchLocator);
        searchButton.click();
        Assert.assertEquals("hi - Google Search", webDriver.getTitle());
    }

    @Test
    public void enterGoogleSearchAndImageSearch() {
        WebDriver webDriver = getDriver();
        By searchLocator = By.cssSelector("input[value='Google Search']");
        webDriver.navigate().to("http://www.google.com");
        WebElement searchText = webDriver.findElement(By.cssSelector("input[title=Search]"));
        searchText.sendKeys("hi");
        WebElement searchButton = webDriver.findElement(searchLocator);
        searchButton.click();
        WebElement imageSearch = webDriver.findElement(By.xpath("//a[contains(text(), 'Images')]"));
        imageSearch.click();
    }


}