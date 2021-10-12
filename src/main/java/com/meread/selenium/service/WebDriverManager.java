package com.meread.selenium.service;

import com.meread.selenium.bean.JDLoginType;
import com.meread.selenium.bean.LoginType;
import com.meread.selenium.bean.MyChromeClient;
import com.meread.selenium.util.WebDriverOpCallBack;

/**
 * @author yangxg
 * @date 2021/9/7
 */
public interface WebDriverManager {

    MyChromeClient createNewMyChromeClient(String httpSessionId, LoginType loginType, JDLoginType jdLoginType);

    void releaseWebDriver(String chromeSessionId, boolean quit);

    void createChrome();

    void createChromeOptions();

    <T> T exec(WebDriverOpCallBack<T> executor);

    void destroyAll();
}
