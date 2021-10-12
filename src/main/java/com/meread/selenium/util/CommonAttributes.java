/*
 * 
 * 
 * 
 */
package com.meread.selenium.util;

import org.springframework.web.socket.WebSocketSession;

/**
 * 公共参数
 */
public final class CommonAttributes {

    public static final String TMPDIR = System.getProperty("java.io.tmpdir");
//    public static final String TMPDIR = "/tmp";
    public static final String SESSION_ID = "HTTP_SESSION_ID";
    public static final String JD_LOGIN_TYPE = "JD_LOGIN_TYPE";

    public static WebSocketSession webSocketSession;

    /**
     * 日期格式配比
     */
    public static final String[] DATE_PATTERNS = new String[]{"yyyy", "yyyy-MM", "yyyyMM", "yyyy/MM", "yyyy-MM-dd", "yyyyMMdd", "yyyy/MM/dd", "yyyy-MM-dd HH:mm:ss", "yyyyMMddHHmmss", "yyyy/MM/dd HH:mm:ss"};

    /**
     * 不可实例化
     */
    private CommonAttributes() {
    }

}