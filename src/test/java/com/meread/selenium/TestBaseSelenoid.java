package com.meread.selenium;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.remote.RemoteWebDriver;

import java.net.URL;

/**
 * Created by yangxg on 2021/9/1
 *
 * @author yangxg
 */
public class TestBaseSelenoid {

    protected static ThreadLocal<RemoteWebDriver> driver = new ThreadLocal<>();

    public RemoteWebDriver getDriver() {
        return driver.get();
    }

    public ChromeOptions capabilities;


    public TestBaseSelenoid(ChromeOptions capabilities) {
        this.capabilities = capabilities;
    }

    @Before
    public void setUp() throws Exception {
        RemoteWebDriver webDriver = new RemoteWebDriver(new URL("http://localhost:4444/wd/hub"), capabilities);
        driver.set(webDriver);
    }

    @After
    public void tearDown() {
        getDriver().quit();
    }

    @AfterClass
    public static void remove() {
        driver.remove();
    }
}
