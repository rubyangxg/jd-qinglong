package com.meread.selenium.bean;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.io.Serializable;

/**
 * @author yangxg
 * @date 2021/9/16
 */
@Data
@AllArgsConstructor
public class StringCache implements Serializable {

    private long createTime;
    private String chromeSessionId;
    private int expire;

    public long getRemainSeconds() {
        long res = (createTime + expire * 1000 - System.currentTimeMillis()) / 1000;
        return res <= 0 ? -1 : res;
    }

}
