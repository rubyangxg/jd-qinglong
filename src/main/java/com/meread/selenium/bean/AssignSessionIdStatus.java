package com.meread.selenium.bean;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.servlet.http.Cookie;

/**
 * Created by yangxg on 2021/9/10
 *
 * @author yangxg
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class AssignSessionIdStatus {
    private String assignChromeSessionId;
    private boolean isNew;
    private String clientChromeSessionId;
}
