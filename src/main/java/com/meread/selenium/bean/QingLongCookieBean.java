package com.meread.selenium.bean;

import lombok.Data;

/**
 * @author yangxg
 * @date 2021/9/11
 */
@Data
public class QingLongCookieBean {
    //{"value":"AAABBB",
    // "_id":"xXoB7jGMN5sYYofb",
    // "created":1631291172989,
    // "status":0,
    // "timestamp":"Sat Sep 11 2021 00:26:12 GMT+0800 (中国标准时间)",
    // "position":4999999999.5,
    // "name":"JD_COOKIE"}
    private String value;
    private String name;
    private String _id;
    private long created;
    private int status;
    private String timestamp;
}
