package com.meread.selenium.util;

import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.remote.RemoteWebDriver;
import org.openqa.selenium.support.ui.ExpectedCondition;
import org.openqa.selenium.support.ui.WebDriverWait;

/**
 * @author yangxg
 * @date 2021/9/13
 */
public class WebDriverUtil {
    public static boolean waitForJStoLoad(RemoteWebDriver webDriver) {

        WebDriverWait wait = new WebDriverWait(webDriver, 10);

        // wait for jQuery to load
        ExpectedCondition<Boolean> jQueryLoad = driver -> {
            try {
                JavascriptExecutor j = (JavascriptExecutor) driver;
                return ((Long) j.executeScript("return jQuery.active") == 0);
            } catch (Exception e) {
                return true;
            }
        };

        // wait for Javascript to load
        ExpectedCondition<Boolean> jsLoad = driver -> {
            JavascriptExecutor j = (JavascriptExecutor) driver;
            return "complete".equals(j.executeScript("return document.readyState")
                    .toString());
        };

        return wait.until(jQueryLoad) && wait.until(jsLoad);
    }
}
