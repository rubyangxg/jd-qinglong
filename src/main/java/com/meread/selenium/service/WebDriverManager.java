package com.meread.selenium.service;

import com.meread.selenium.bean.*;
import com.meread.selenium.util.WebDriverOpCallBack;
import org.openqa.selenium.remote.RemoteWebDriver;

import java.util.List;
import java.util.Properties;

/**
 * @author yangxg
 * @date 2021/9/7
 */
public interface WebDriverManager {

    MyChromeClient createNewMyChromeClient(String httpSessionId, LoginType loginType, JDLoginType jdLoginType);

    void releaseWebDriver(String chromeSessionId);

    void createChrome();

    void createChromeOptions();

    <T> T exec(WebDriverOpCallBack<T> executor);

    void destroyAll();
}
