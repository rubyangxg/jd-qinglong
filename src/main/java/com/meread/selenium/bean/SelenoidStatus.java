package com.meread.selenium.bean;

import com.alibaba.fastjson.JSONObject;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * Created by yangxg on 2021/9/22
 *
 * @author yangxg
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class SelenoidStatus {
    private int total;
    private int used;
    private int queued;
    private int pending;
    Map<String, JSONObject> sessions;
}
