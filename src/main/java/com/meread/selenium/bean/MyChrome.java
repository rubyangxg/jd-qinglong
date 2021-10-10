package com.meread.selenium.bean;

import com.alibaba.fastjson.JSONObject;
import lombok.Data;
import org.openqa.selenium.chrome.ChromeDriverService;
import org.openqa.selenium.remote.RemoteWebDriver;

/**
 * Created by yangxg on 2021/9/9
 *
 * @author yangxg
 */
@Data
public class MyChrome {
    //这两个字段会随着创建chrome自动更新
    private RemoteWebDriver webDriver;
    private ChromeDriverService chromeDriverService;
    private JSONObject sessionInfoJson;
    private long expireTime;
    private String userTrackId;

    public MyChrome(RemoteWebDriver webDriver, ChromeDriverService chromeDriverService, long expireTime) {
        this.webDriver = webDriver;
        this.expireTime = expireTime;
        this.chromeDriverService = chromeDriverService;
    }

    public boolean isExpire() {
        return expireTime < System.currentTimeMillis();
    }

    public String getChromeSessionId() {
        return webDriver.getSessionId().toString();
    }
}
