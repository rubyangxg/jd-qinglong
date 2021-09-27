package com.meread.selenium.bean;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * @author yangxg
 * @date 2021/9/16
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class QQCache implements Serializable {
    private long qq;
    private String phone;
    private String authCode;
    private String ck;

    public QQCache(long qq) {
        this.qq = qq;
    }


}
