package com.meread.selenium.service;

import com.meread.selenium.bean.*;
import org.openqa.selenium.remote.RemoteWebDriver;

import java.util.*;

/**
 * @author yangxg
 * @date 2021/9/7
 */
public interface WebDriverManager {

    MyChromeClient getCacheMyChromeClient(String httpSessionId);

    MyChromeClient createNewMyChromeClient(String httpSessionId, LoginType loginType, JDLoginType jdLoginType);

    void releaseWebDriver(String chromeSessionId);

    RemoteWebDriver getDriverBySessionId(String chromeSessionId);

    List<QLConfig> getQlConfigs();

    StatClient getStatClient();

    String getXddUrl();

    String getXddToken();

    Properties getProperties();

    boolean getToken(QLConfig qlConfig);

    boolean isInitSuccess();
}
