package com.meread.selenium.bean;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Created by yangxg on 2021/9/14
 *
 * @author yangxg
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class QLToken {
    private String token;
    private String tokenType;
    private long expiration;

    public boolean isExpired() {
        return expiration > System.currentTimeMillis() / 1000;
    }
}
