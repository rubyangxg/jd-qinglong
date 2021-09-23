package com.meread.selenium;

import org.apache.commons.io.FileUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.openqa.selenium.*;
import org.openqa.selenium.remote.Augmenter;
import org.openqa.selenium.remote.DesiredCapabilities;
import org.openqa.selenium.remote.RemoteWebDriver;

import java.io.File;
import java.net.URL;

public class DemoTest {

    private RemoteWebDriver driver;

    @Before
    public void openDriver() throws Exception {
        final DesiredCapabilities browser = DesiredCapabilities.chrome();
//        browser.setCapability("enableVideo", true);
        browser.setCapability("enableLog", true);
        browser.setCapability("enableVNC", true);
        driver = new RemoteWebDriver(new URL(
                "http://localhost:4444/wd/hub" //Replace with correct host and port
        ), browser);
    }

    @Test
    public void browserTest() throws Exception {
        try {
            driver.get("https://graph.qq.com/oauth2.0/show?which=Login&display=pc&response_type=code&client_id=100273020&redirect_uri=https%3A%2F%2Fplogin.m.jd.com%2Fcgi-bin%2Fml%2Fqqcallback%3Flsid%3D6pifrkrkjsb6t3mvpr0tplsrgpqa51rcq9sitj2dbej5h617&state=y20nntf3");
            String pageText = driver.findElement(By.tagName("body")).getText();
            System.out.println(pageText);
        } finally {
            takeScreenshot(driver);
        }

    }

    static void takeScreenshot(RemoteWebDriver driver) throws Exception {
        byte[] screen = ((TakesScreenshot) new Augmenter().augment(driver)).getScreenshotAs(OutputType.BYTES);
        FileUtils.writeByteArrayToFile(new File(driver.getSessionId() + ".png"), screen);
    }

    @After
    public void closeDriver(){
        if (driver != null){
            driver.quit();
            driver = null;
        }
    }
}