package com.meread.selenium.bean;

import com.alibaba.fastjson.JSONObject;
import lombok.Data;
import org.openqa.selenium.remote.RemoteWebDriver;

/**
 * Created by yangxg on 2021/9/9
 *
 * @author yangxg
 */
@Data
public class MyChrome {
    private RemoteWebDriver webDriver;
    private JSONObject sessionInfoJson;
    private String selenoidSessionId;
    private String clientSessionId;
}
