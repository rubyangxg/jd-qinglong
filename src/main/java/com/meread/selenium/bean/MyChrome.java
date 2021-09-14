package com.meread.selenium.bean;

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
    private SlotStatus slotStatus;
    private String clientSessionId;
}
