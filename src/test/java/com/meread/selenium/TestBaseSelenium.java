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
public class TestBaseSelenium {

    public enum DriverType {
        selenium,
        selenoid
    }

    protected static ThreadLocal<RemoteWebDriver> driver = new ThreadLocal<>();
    protected static ThreadLocal<RemoteWebDriver> driver_selenoid = new ThreadLocal<>();

    public RemoteWebDriver getDriver(DriverType type) {
        return type == DriverType.selenium ? driver.get() : driver_selenoid.get();
    }

    public ChromeOptions capabilities;


    public TestBaseSelenium(ChromeOptions capabilities) {
        this.capabilities = capabilities;
    }

    @Before
    public void setUp() throws Exception {
        RemoteWebDriver webDriver = new RemoteWebDriver(new URL("http://localhost:4445/wd/hub"), capabilities);
        RemoteWebDriver webDriver_selenoid = new RemoteWebDriver(new URL("http://localhost:4444/wd/hub"), capabilities);
        driver.set(webDriver);
        driver_selenoid.set(webDriver_selenoid);
    }

    @After
    public void tearDown() {
        getDriver(DriverType.selenium).quit();
        getDriver(DriverType.selenoid).quit();
    }

    @AfterClass
    public static void remove() {
        driver.remove();
        driver_selenoid.remove();
    }
}
